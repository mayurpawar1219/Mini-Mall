package com.minidmart.service;

import com.minidmart.dto.CheckoutConfirmRequest;
import com.minidmart.dto.OrderResponse;
import com.minidmart.dto.PaymentIntentResponse;
import com.minidmart.entity.Cart;
import com.minidmart.entity.CartItem;
import com.minidmart.entity.Inventory;
import com.minidmart.entity.Order;
import com.minidmart.entity.OrderItem;
import com.minidmart.entity.OrderStatus;
import com.minidmart.entity.Product;
import com.minidmart.entity.User;
import com.minidmart.entity.PickupSlot;
import com.minidmart.exception.EmptyCartException;
import com.minidmart.exception.InvalidOperationException;
import com.minidmart.repository.CartItemRepository;
import com.minidmart.repository.CartRepository;
import com.minidmart.repository.InventoryRepository;
import com.minidmart.repository.OrderItemRepository;
import com.minidmart.repository.OrderRepository;
import com.minidmart.repository.PickupSlotRepository;
import com.minidmart.repository.UserRepository;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.RefundCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CheckoutService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final InventoryRepository inventoryRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserRepository userRepository;
    private final PickupSlotRepository pickupSlotRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public PaymentIntentResponse createPaymentIntent(UUID userId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new EmptyCartException("Cart is empty"));

        if (cart.getItems().isEmpty()) {
            throw new EmptyCartException("Cannot create payment for an empty cart");
        }

        // Calculate authoritative total
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (CartItem item : cart.getItems()) {
            Product product = item.getProduct();
            if (!product.isActive()) {
                throw new InvalidOperationException("Product " + product.getName() + " is no longer active");
            }
            BigDecimal subtotal = product.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            totalAmount = totalAmount.add(subtotal);
        }

        // Convert to cents for Stripe
        long amountInCents = totalAmount.multiply(new BigDecimal(100)).longValue();

        try {
            PaymentIntentCreateParams params =
                    PaymentIntentCreateParams.builder()
                            .setAmount(amountInCents)
                            .setCurrency("usd")
                            // In a real app, you can pass metadata like cartId or userId
                            .putMetadata("userId", userId.toString())
                            .putMetadata("cartId", cart.getId().toString())
                            .build();

            PaymentIntent intent = PaymentIntent.create(params);
            
            return PaymentIntentResponse.builder()
                    .clientSecret(intent.getClientSecret())
                    .build();
        } catch (StripeException e) {
            log.error("Stripe payment intent creation failed", e);
            throw new InvalidOperationException("Failed to initialize payment: " + e.getMessage());
        }
    }

    @Transactional
    public OrderResponse checkout(UUID userId, CheckoutConfirmRequest request) {
        // 1. Verify Stripe Payment first
        PaymentIntent paymentIntent;
        try {
            paymentIntent = PaymentIntent.retrieve(request.getPaymentIntentId());
            if (!"succeeded".equals(paymentIntent.getStatus())) {
                throw new InvalidOperationException("Payment has not succeeded. Status: " + paymentIntent.getStatus());
            }
        } catch (StripeException e) {
            log.error("Failed to retrieve PaymentIntent", e);
            throw new InvalidOperationException("Invalid payment intent ID");
        }

        // 2. Load context
        User user = userRepository.findById(userId).orElseThrow();
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new EmptyCartException("Cart is empty"));

        List<CartItem> items = cart.getItems();
        
        // 3. Validate cart
        if (items.isEmpty()) {
            refundPayment(paymentIntent.getId(), "Cart was empty at final checkout");
            throw new EmptyCartException("Cannot checkout an empty cart");
        }

        // 3a. Validate delivery fields
        if (request.getType() == com.minidmart.entity.OrderType.HOME_DELIVERY) {
            if (request.getDeliveryAddress() == null || request.getDeliveryAddress().isBlank()) {
                refundPayment(paymentIntent.getId(), "Missing delivery address");
                throw new InvalidOperationException("Delivery address is required for HOME_DELIVERY");
            }
            if (request.getDeliveryCity() == null || request.getDeliveryCity().isBlank()) {
                refundPayment(paymentIntent.getId(), "Missing delivery city");
                throw new InvalidOperationException("Delivery city is required for HOME_DELIVERY");
            }
            if (request.getDeliveryPostalCode() == null || request.getDeliveryPostalCode().isBlank()) {
                refundPayment(paymentIntent.getId(), "Missing delivery postal code");
                throw new InvalidOperationException("Delivery postal code is required for HOME_DELIVERY");
            }
        }
        
        PickupSlot pickupSlot = null;
        if (request.getType() == com.minidmart.entity.OrderType.SCHEDULED_PICKUP) {
            if (request.getPickupSlotId() == null) {
                refundPayment(paymentIntent.getId(), "Missing pickup slot ID");
                throw new InvalidOperationException("Pickup slot is required for SCHEDULED_PICKUP");
            }
            
            // Atomically reserve slot
            int updatedRows = pickupSlotRepository.incrementBookingIfCapacityAllows(request.getPickupSlotId());
            if (updatedRows == 0) {
                refundPayment(paymentIntent.getId(), "Pickup slot became full or unavailable");
                throw new InvalidOperationException("The selected pickup slot is no longer available.");
            }
            
            pickupSlot = pickupSlotRepository.findById(request.getPickupSlotId())
                    .orElseThrow(() -> new InvalidOperationException("Invalid pickup slot ID"));
        }

        BigDecimal totalAmount = BigDecimal.ZERO;

        // 4. Lock & verify stock + Calculate total
        try {
            for (CartItem item : items) {
                Product product = item.getProduct();
                if (!product.isActive()) {
                    throw new InvalidOperationException("Product " + product.getName() + " is no longer active");
                }

                Inventory inventory = inventoryRepository.findByProductId(product.getId())
                        .orElseThrow(() -> new InvalidOperationException("Inventory missing for product: " + product.getName()));

                if (item.getQuantity() > inventory.getAvailableQuantity()) {
                    throw new InvalidOperationException(
                            "Not enough stock for " + product.getName() + ". Available: " + inventory.getAvailableQuantity()
                    );
                }

                // Adjust stock (This uses @Version optimistic locking)
                inventory.setAvailableQuantity(inventory.getAvailableQuantity() - item.getQuantity());
                inventoryRepository.save(inventory);

                // Accumulate total
                BigDecimal subtotal = product.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
                totalAmount = totalAmount.add(subtotal);
            }
        } catch (Exception e) {
            // If stock validation fails or optimistic locking fails, refund the customer!
            log.error("Stock validation or locking failed during checkout for user {}, issuing refund.", userId, e);
            refundPayment(paymentIntent.getId(), "Stock conflict during checkout: " + e.getMessage());
            throw e; // re-throw so the transaction rolls back
        }

        // 5. Create Order
        String orderNumber = "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Order order = Order.builder()
                .user(user)
                .orderNumber(orderNumber)
                .status(OrderStatus.PLACED)
                .type(request.getType())
                .totalAmount(totalAmount)
                .deliveryAddress(request.getDeliveryAddress())
                .deliveryCity(request.getDeliveryCity())
                .deliveryPostalCode(request.getDeliveryPostalCode())
                .deliveryPhone(request.getDeliveryPhone())
                .pickupSlot(pickupSlot)
                .paymentIntentId(paymentIntent.getId()) // Store payment reference if we add field later
                .build();
        
        order = orderRepository.save(order);

        // 6. Create OrderItems
        Order finalOrder = order;
        List<OrderItem> orderItems = items.stream().map(cartItem -> {
            return OrderItem.builder()
                    .order(finalOrder)
                    .product(cartItem.getProduct())
                    .productName(cartItem.getProduct().getName())
                    .priceAtPurchase(cartItem.getProduct().getPrice())
                    .quantity(cartItem.getQuantity())
                    .build();
        }).collect(Collectors.toList());

        orderItemRepository.saveAll(orderItems);

        // 7. Cleanup Cart
        cartItemRepository.deleteByCartId(cart.getId());
        cart.getItems().clear();
        cartRepository.save(cart);

        // 8. Audit Log
        auditService.log(
                user.getId(),
                "CHECKOUT_COMPLETED",
                "Order",
                order.getId().toString(),
                "Order placed successfully: " + orderNumber + " with PaymentIntent: " + paymentIntent.getId()
        );

        return OrderResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .build();
    }

    public void refundPayment(String paymentIntentId, String reason) {
        if (paymentIntentId == null || paymentIntentId.isBlank()) {
            log.info("No paymentIntentId provided. Skipping refund.");
            return;
        }
        try {
            log.info("Issuing refund for PaymentIntent: {} due to: {}", paymentIntentId, reason);
            RefundCreateParams params = RefundCreateParams.builder()
                    .setPaymentIntent(paymentIntentId)
                    .setReason(RefundCreateParams.Reason.REQUESTED_BY_CUSTOMER)
                    .build();
            Refund.create(params);
        } catch (StripeException se) {
            log.error("CRITICAL: Failed to refund PaymentIntent {} automatically. Reason: {}", paymentIntentId, se.getMessage(), se);
            // In a production system, this would trigger an alert for manual refund processing.
        }
    }
}

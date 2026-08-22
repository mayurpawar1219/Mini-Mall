package com.minidmart.service;

import com.minidmart.dto.OrderDetailResponse;
import com.minidmart.dto.OrderItemResponse;
import com.minidmart.dto.OrderSummaryResponse;
import com.minidmart.entity.Inventory;
import com.minidmart.entity.Order;
import com.minidmart.entity.PickupSlot;
import com.minidmart.entity.OrderItem;
import com.minidmart.entity.OrderStatus;
import com.minidmart.entity.OrderType;
import com.minidmart.exception.InvalidOperationException;
import com.minidmart.exception.ResourceNotFoundException;
import com.minidmart.repository.InventoryRepository;
import com.minidmart.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Order management service handling order retrieval, status transitions,
 * and cancellation with inventory restoration.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final InventoryRepository inventoryRepository;
    private final com.minidmart.repository.PickupSlotRepository pickupSlotRepository;
    private final AuditService auditService;
    private final CheckoutService checkoutService;

    // ---- Status Transition Rules ----

    /**
     * Defines the valid status transitions for each current status.
     * CANCELLED is handled separately since it depends on cancellation eligibility rules.
     */
    private static final Map<OrderStatus, Set<OrderStatus>> VALID_TRANSITIONS;

    static {
        VALID_TRANSITIONS = new EnumMap<>(OrderStatus.class);
        VALID_TRANSITIONS.put(OrderStatus.PLACED, Set.of(OrderStatus.CONFIRMED, OrderStatus.CANCELLED));
        VALID_TRANSITIONS.put(OrderStatus.CONFIRMED, Set.of(OrderStatus.PREPARING, OrderStatus.CANCELLED));
        VALID_TRANSITIONS.put(OrderStatus.PREPARING, Set.of(OrderStatus.READY_FOR_PICKUP, OrderStatus.OUT_FOR_DELIVERY));
        VALID_TRANSITIONS.put(OrderStatus.READY_FOR_PICKUP, Set.of(OrderStatus.PICKED_UP));
        VALID_TRANSITIONS.put(OrderStatus.OUT_FOR_DELIVERY, Set.of(OrderStatus.DELIVERED));
        // Terminal states — no transitions allowed
        VALID_TRANSITIONS.put(OrderStatus.PICKED_UP, Set.of());
        VALID_TRANSITIONS.put(OrderStatus.DELIVERED, Set.of());
        VALID_TRANSITIONS.put(OrderStatus.CANCELLED, Set.of());
    }

    /**
     * Maps OrderType to the statuses that are NOT applicable to that type.
     * Used to prevent invalid type-status combinations.
     */
    private static final Map<OrderType, Set<OrderStatus>> INCOMPATIBLE_STATUSES;

    static {
        INCOMPATIBLE_STATUSES = new EnumMap<>(OrderType.class);
        // Pickup orders cannot use delivery statuses
        INCOMPATIBLE_STATUSES.put(OrderType.STORE_PICKUP, Set.of(OrderStatus.OUT_FOR_DELIVERY, OrderStatus.DELIVERED));
        INCOMPATIBLE_STATUSES.put(OrderType.SCHEDULED_PICKUP, Set.of(OrderStatus.OUT_FOR_DELIVERY, OrderStatus.DELIVERED));
        // Delivery orders cannot use pickup statuses
        INCOMPATIBLE_STATUSES.put(OrderType.HOME_DELIVERY, Set.of(OrderStatus.READY_FOR_PICKUP, OrderStatus.PICKED_UP));
    }

    /** Statuses from which cancellation is allowed. */
    private static final Set<OrderStatus> CANCELLABLE_STATUSES = Set.of(
            OrderStatus.PLACED, OrderStatus.CONFIRMED
    );

    // ---- Customer Operations ----

    /**
     * Retrieve paginated order history for the authenticated customer.
     * Only returns orders belonging to the specified user.
     */
    @Transactional(readOnly = true)
    public Page<OrderSummaryResponse> getCustomerOrders(UUID userId, int page, int size) {
        size = Math.min(size, 50);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return orderRepository.findByUserId(userId, pageable).map(this::toSummaryResponse);
    }

    /**
     * Retrieve a specific order for the authenticated customer.
     * Returns 404 if the order does not exist OR does not belong to the user (IDOR protection).
     */
    @Transactional(readOnly = true)
    public OrderDetailResponse getCustomerOrder(UUID orderId, UUID userId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));
        return toDetailResponse(order);
    }

    /**
     * Cancel a customer's own order.
     * Only allowed from PLACED or CONFIRMED status.
     * Restores inventory and creates an audit log.
     */
    @Transactional
    public OrderDetailResponse cancelCustomerOrder(UUID orderId, UUID userId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));
        return performCancellation(order, userId, "CUSTOMER");
    }

    // ---- Staff/Admin Operations ----

    /**
     * Retrieve paginated orders for staff/admin with optional filtering.
     */
    @Transactional(readOnly = true)
    public Page<OrderSummaryResponse> getAdminOrders(int page, int size,
                                                      OrderStatus status, OrderType type) {
        size = Math.min(size, 50);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Order> orders;
        if (status != null && type != null) {
            orders = orderRepository.findByStatusAndType(status, type, pageable);
        } else if (status != null) {
            orders = orderRepository.findByStatus(status, pageable);
        } else if (type != null) {
            orders = orderRepository.findByType(type, pageable);
        } else {
            orders = orderRepository.findAll(pageable);
        }

        return orders.map(this::toSummaryResponse);
    }

    /**
     * Retrieve any order by ID for staff/admin.
     */
    @Transactional(readOnly = true)
    public OrderDetailResponse getAdminOrder(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));
        return toDetailResponse(order);
    }

    /**
     * Update order status. Validates the transition against the state machine
     * and OrderType compatibility. Creates an audit log.
     */
    @Transactional
    public OrderDetailResponse updateOrderStatus(UUID orderId, OrderStatus newStatus, UUID actorId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        OrderStatus currentStatus = order.getStatus();

        // Reject if this is a cancellation request — use the cancel endpoint instead
        if (newStatus == OrderStatus.CANCELLED) {
            throw new InvalidOperationException(
                    "Use the cancellation endpoint to cancel an order");
        }

        // Validate transition
        validateTransition(currentStatus, newStatus, order.getType());

        String oldStatusName = currentStatus.name();
        order.setStatus(newStatus);
        
        if (newStatus == OrderStatus.DELIVERED || newStatus == OrderStatus.PICKED_UP) {
            order.setCompletedAt(java.time.LocalDateTime.now());
        }

        orderRepository.save(order);

        auditService.log(actorId, "ORDER_STATUS_CHANGED", "Order",
                order.getId().toString(),
                "Order " + order.getOrderNumber() + ": " + oldStatusName + " → " + newStatus.name());

        log.info("Order {} status changed: {} → {} by {}",
                order.getOrderNumber(), oldStatusName, newStatus.name(), actorId);

        return toDetailResponse(order);
    }

    @Transactional
    public OrderDetailResponse bookPickupSlot(UUID orderId, UUID userId, Long slotId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        if (order.getType() != OrderType.SCHEDULED_PICKUP) {
            throw new InvalidOperationException("Only SCHEDULED_PICKUP orders can book a time slot");
        }

        if (order.getStatus() != OrderStatus.PLACED && order.getStatus() != OrderStatus.CONFIRMED) {
            throw new InvalidOperationException("Cannot book or change slot for order in status: " + order.getStatus());
        }

        // Release old slot if replacing
        if (order.getPickupSlot() != null) {
            if (order.getPickupSlot().getId().equals(slotId)) {
                return toDetailResponse(order); // No change
            }
            int released = pickupSlotRepository.decrementBookingSafely(order.getPickupSlot().getId());
            if (released > 0) {
                auditService.log(userId, "SLOT_RELEASED", "PickupSlot", order.getPickupSlot().getId().toString(),
                        "Released slot for order " + order.getOrderNumber());
            }
        }

        // Try booking new slot atomically
        int updated = pickupSlotRepository.incrementBookingIfCapacityAllows(slotId);
        if (updated == 0) {
            throw new InvalidOperationException("Selected pickup slot is full or unavailable");
        }

        PickupSlot slot = pickupSlotRepository.findById(slotId)
                .orElseThrow(() -> new ResourceNotFoundException("PickupSlot", "id", slotId));
        order.setPickupSlot(slot);
        orderRepository.save(order);

        auditService.log(userId, "SLOT_BOOKED", "PickupSlot", slotId.toString(),
                "Booked slot for order " + order.getOrderNumber());

        return toDetailResponse(order);
    }

    /**
     * Cancel any eligible order (staff/admin).
     * Restores inventory and creates an audit log.
     */
    @Transactional
    public OrderDetailResponse cancelAdminOrder(UUID orderId, UUID actorId, String actorRole) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));
        return performCancellation(order, actorId, actorRole);
    }

    // ---- Private Helpers ----

    /**
     * Perform cancellation: validate status, restore inventory, update order, audit.
     */
    private OrderDetailResponse performCancellation(Order order, UUID actorId, String actorRole) {
        OrderStatus currentStatus = order.getStatus();

        if (!CANCELLABLE_STATUSES.contains(currentStatus)) {
            throw new InvalidOperationException(
                    "Order cannot be cancelled from status: " + currentStatus.name()
                    + ". Cancellation is only allowed for PLACED or CONFIRMED orders.");
        }

        String oldStatusName = currentStatus.name();

        // Restore inventory for each order item
        for (OrderItem item : order.getItems()) {
            if (item.getProduct() != null) {
                Inventory inventory = inventoryRepository.findByProductId(item.getProduct().getId())
                        .orElse(null);
                if (inventory != null) {
                    inventory.setAvailableQuantity(
                            inventory.getAvailableQuantity() + item.getQuantity());
                    inventoryRepository.save(inventory);
                    log.debug("Restored {} units of product {} for cancelled order {}",
                            item.getQuantity(), item.getProduct().getId(), order.getOrderNumber());
                }
            }
        }

        // Release pickup slot if present
        if (order.getPickupSlot() != null) {
            Long slotId = order.getPickupSlot().getId();
            int released = pickupSlotRepository.decrementBookingSafely(slotId);
            if (released > 0) {
                auditService.log(actorId, "SLOT_RELEASED", "PickupSlot", slotId.toString(),
                        "Released slot due to cancellation of order " + order.getOrderNumber());
                log.info("Released pickup slot {} for cancelled order {}", slotId, order.getOrderNumber());
            }
        }

        // Refund Stripe payment if it exists
        if (order.getPaymentIntentId() != null) {
            checkoutService.refundPayment(order.getPaymentIntentId(), "Order cancelled by " + actorRole);
        }

        // Update order status
        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);

        // Audit
        String action = "CUSTOMER".equals(actorRole)
                ? "ORDER_CANCELLED_BY_CUSTOMER"
                : "ORDER_CANCELLED_BY_STAFF";
        auditService.log(actorId, action, "Order",
                order.getId().toString(),
                "Order " + order.getOrderNumber() + " cancelled by " + actorRole
                + ". Previous status: " + oldStatusName);

        log.info("Order {} cancelled by {} ({}). Previous status: {}",
                order.getOrderNumber(), actorId, actorRole, oldStatusName);

        return toDetailResponse(order);
    }

    /**
     * Validate a status transition against the state machine and OrderType compatibility.
     */
    private void validateTransition(OrderStatus from, OrderStatus to, OrderType orderType) {
        Set<OrderStatus> allowed = VALID_TRANSITIONS.get(from);
        if (allowed == null || !allowed.contains(to)) {
            throw new InvalidOperationException(
                    "Invalid status transition: " + from.name() + " → " + to.name());
        }

        Set<OrderStatus> incompatible = INCOMPATIBLE_STATUSES.get(orderType);
        if (incompatible != null && incompatible.contains(to)) {
            throw new InvalidOperationException(
                    "Status " + to.name() + " is not applicable for order type " + orderType.name());
        }
    }

    // ---- DTO Mapping ----

    private OrderSummaryResponse toSummaryResponse(Order order) {
        return OrderSummaryResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .status(order.getStatus())
                .type(order.getType())
                .totalAmount(order.getTotalAmount())
                .itemCount(order.getItems().size())
                .createdAt(order.getCreatedAt())
                .build();
    }

    private OrderDetailResponse toDetailResponse(Order order) {
        List<OrderItemResponse> items = order.getItems().stream()
                .map(this::toItemResponse)
                .collect(Collectors.toList());

        return OrderDetailResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .status(order.getStatus())
                .type(order.getType())
                .totalAmount(order.getTotalAmount())
                .items(items)
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .customerName(order.getUser() != null ? order.getUser().getFirstName() + " " + order.getUser().getLastName() : "Unknown")
                .customerEmail(order.getUser() != null ? order.getUser().getEmail() : "Unknown")
                .deliveryAddress(order.getDeliveryAddress())
                .deliveryCity(order.getDeliveryCity())
                .deliveryPostalCode(order.getDeliveryPostalCode())
                .deliveryPhone(order.getDeliveryPhone())
                .pickupSlotId(order.getPickupSlot() != null ? order.getPickupSlot().getId() : null)
                .pickupSlotStartTime(order.getPickupSlot() != null ? order.getPickupSlot().getStartTime() : null)
                .pickupSlotEndTime(order.getPickupSlot() != null ? order.getPickupSlot().getEndTime() : null)
                .build();
    }

    private OrderItemResponse toItemResponse(OrderItem item) {
        BigDecimal subtotal = item.getPriceAtPurchase()
                .multiply(BigDecimal.valueOf(item.getQuantity()));
        return OrderItemResponse.builder()
                .id(item.getId())
                .productId(item.getProduct() != null ? item.getProduct().getId() : null)
                .productName(item.getProductName())
                .priceAtPurchase(item.getPriceAtPurchase())
                .quantity(item.getQuantity())
                .subtotal(subtotal)
                .build();
    }
}

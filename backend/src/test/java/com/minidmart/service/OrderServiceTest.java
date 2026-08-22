package com.minidmart.service;

import com.minidmart.dto.OrderDetailResponse;
import com.minidmart.dto.OrderSummaryResponse;
import com.minidmart.entity.Inventory;
import com.minidmart.entity.Order;
import com.minidmart.entity.OrderItem;
import com.minidmart.entity.OrderStatus;
import com.minidmart.entity.OrderType;
import com.minidmart.entity.Product;
import com.minidmart.entity.User;
import com.minidmart.exception.InvalidOperationException;
import com.minidmart.exception.ResourceNotFoundException;
import com.minidmart.repository.InventoryRepository;
import com.minidmart.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private InventoryRepository inventoryRepository;
    @Mock
    private AuditService auditService;

    @InjectMocks
    private OrderService orderService;

    private UUID userId;
    private UUID otherUserId;
    private UUID orderId;
    private User user;
    private Product product;
    private Inventory inventory;
    private Order order;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        otherUserId = UUID.randomUUID();
        orderId = UUID.randomUUID();

        user = User.builder().id(userId).firstName("John").lastName("Doe").build();
        product = Product.builder().id(100L).name("Historical Name").price(BigDecimal.valueOf(99.99)).active(true).build();
        inventory = Inventory.builder().id(10L).product(product).availableQuantity(50).reservedQuantity(0).build();

        OrderItem item = OrderItem.builder()
                .id(1L)
                .product(product)
                .productName("Historical Name")
                .priceAtPurchase(BigDecimal.valueOf(25.00))
                .quantity(3)
                .build();

        order = Order.builder()
                .id(orderId)
                .orderNumber("ORD-ABCD1234")
                .user(user)
                .status(OrderStatus.PLACED)
                .type(OrderType.STORE_PICKUP)
                .totalAmount(BigDecimal.valueOf(75.00))
                .items(new ArrayList<>(List.of(item)))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        item.setOrder(order);
    }

    // ===== Customer Order Retrieval =====

    @Nested
    @DisplayName("Customer Order History")
    class CustomerOrderHistory {

        @Test
        @DisplayName("Customer retrieves own order history — paginated, newest first")
        void getCustomerOrders_ReturnsOwnOrders() {
            Page<Order> page = new PageImpl<>(List.of(order));
            when(orderRepository.findByUserId(eq(userId), any(Pageable.class))).thenReturn(page);

            Page<OrderSummaryResponse> result = orderService.getCustomerOrders(userId, 0, 20);

            assertNotNull(result);
            assertEquals(1, result.getContent().size());
            assertEquals("ORD-ABCD1234", result.getContent().get(0).getOrderNumber());
            verify(orderRepository).findByUserId(eq(userId), any(Pageable.class));
        }

        @Test
        @DisplayName("Page size is capped at 50")
        void getCustomerOrders_CapsPageSize() {
            Page<Order> page = new PageImpl<>(List.of());
            when(orderRepository.findByUserId(eq(userId), any(Pageable.class))).thenReturn(page);

            orderService.getCustomerOrders(userId, 0, 100);

            verify(orderRepository).findByUserId(eq(userId), argThat(pageable ->
                    pageable.getPageSize() == 50));
        }
    }

    @Nested
    @DisplayName("Customer Order Details")
    class CustomerOrderDetails {

        @Test
        @DisplayName("Customer retrieves own order details with items")
        void getCustomerOrder_ReturnsDetails() {
            when(orderRepository.findByIdAndUserId(orderId, userId)).thenReturn(Optional.of(order));

            OrderDetailResponse result = orderService.getCustomerOrder(orderId, userId);

            assertNotNull(result);
            assertEquals("ORD-ABCD1234", result.getOrderNumber());
            assertEquals(OrderStatus.PLACED, result.getStatus());
            assertEquals(1, result.getItems().size());
            assertEquals("Historical Name", result.getItems().get(0).getProductName());
        }

        @Test
        @DisplayName("Customer cannot retrieve another customer's order — returns 404")
        void getCustomerOrder_OtherUser_Throws404() {
            when(orderRepository.findByIdAndUserId(orderId, otherUserId)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> orderService.getCustomerOrder(orderId, otherUserId));
        }

        @Test
        @DisplayName("Historical product name is preserved in response")
        void getCustomerOrder_PreservesHistoricalProductName() {
            // Change the current product name — order should still show historical name
            product.setName("Updated Current Name");
            when(orderRepository.findByIdAndUserId(orderId, userId)).thenReturn(Optional.of(order));

            OrderDetailResponse result = orderService.getCustomerOrder(orderId, userId);

            assertEquals("Historical Name", result.getItems().get(0).getProductName());
        }

        @Test
        @DisplayName("Historical purchase price is preserved in response")
        void getCustomerOrder_PreservesHistoricalPrice() {
            product.setPrice(BigDecimal.valueOf(999.99));
            when(orderRepository.findByIdAndUserId(orderId, userId)).thenReturn(Optional.of(order));

            OrderDetailResponse result = orderService.getCustomerOrder(orderId, userId);

            assertEquals(BigDecimal.valueOf(25.00), result.getItems().get(0).getPriceAtPurchase());
        }

        @Test
        @DisplayName("Item subtotal is correctly computed from historical data")
        void getCustomerOrder_ComputesSubtotalCorrectly() {
            when(orderRepository.findByIdAndUserId(orderId, userId)).thenReturn(Optional.of(order));

            OrderDetailResponse result = orderService.getCustomerOrder(orderId, userId);

            // 25.00 × 3 = 75.00
            assertEquals(0, BigDecimal.valueOf(75.00).compareTo(result.getItems().get(0).getSubtotal()));
        }

        @Test
        @DisplayName("Order total remains correct in response")
        void getCustomerOrder_TotalAmountCorrect() {
            when(orderRepository.findByIdAndUserId(orderId, userId)).thenReturn(Optional.of(order));

            OrderDetailResponse result = orderService.getCustomerOrder(orderId, userId);

            assertEquals(BigDecimal.valueOf(75.00), result.getTotalAmount());
        }
    }

    // ===== Status Transitions =====

    @Nested
    @DisplayName("Order Status Transitions")
    class StatusTransitions {

        @Test
        @DisplayName("PLACED → CONFIRMED succeeds")
        void updateStatus_PlacedToConfirmed_Succeeds() {
            order.setStatus(OrderStatus.PLACED);
            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
            when(orderRepository.save(any(Order.class))).thenReturn(order);

            OrderDetailResponse result = orderService.updateOrderStatus(orderId, OrderStatus.CONFIRMED, userId);

            assertEquals(OrderStatus.CONFIRMED, result.getStatus());
            verify(auditService).log(eq(userId), eq("ORDER_STATUS_CHANGED"),
                    eq("Order"), eq(orderId.toString()), anyString());
        }

        @Test
        @DisplayName("CONFIRMED → PREPARING succeeds")
        void updateStatus_ConfirmedToPreparing_Succeeds() {
            order.setStatus(OrderStatus.CONFIRMED);
            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
            when(orderRepository.save(any(Order.class))).thenReturn(order);

            OrderDetailResponse result = orderService.updateOrderStatus(orderId, OrderStatus.PREPARING, userId);

            assertEquals(OrderStatus.PREPARING, result.getStatus());
        }

        @Test
        @DisplayName("PREPARING → READY_FOR_PICKUP succeeds for pickup order")
        void updateStatus_PreparingToReady_PickupOrder_Succeeds() {
            order.setStatus(OrderStatus.PREPARING);
            order.setType(OrderType.STORE_PICKUP);
            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
            when(orderRepository.save(any(Order.class))).thenReturn(order);

            OrderDetailResponse result = orderService.updateOrderStatus(orderId, OrderStatus.READY_FOR_PICKUP, userId);

            assertEquals(OrderStatus.READY_FOR_PICKUP, result.getStatus());
        }

        @Test
        @DisplayName("PREPARING → OUT_FOR_DELIVERY succeeds for delivery order")
        void updateStatus_PreparingToOutForDelivery_DeliveryOrder_Succeeds() {
            order.setStatus(OrderStatus.PREPARING);
            order.setType(OrderType.HOME_DELIVERY);
            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
            when(orderRepository.save(any(Order.class))).thenReturn(order);

            OrderDetailResponse result = orderService.updateOrderStatus(orderId, OrderStatus.OUT_FOR_DELIVERY, userId);

            assertEquals(OrderStatus.OUT_FOR_DELIVERY, result.getStatus());
        }

        @Test
        @DisplayName("Invalid transition DELIVERED → PLACED is rejected")
        void updateStatus_InvalidTransition_Throws() {
            order.setStatus(OrderStatus.DELIVERED);
            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

            assertThrows(InvalidOperationException.class,
                    () -> orderService.updateOrderStatus(orderId, OrderStatus.PLACED, userId));
        }

        @Test
        @DisplayName("Skipping transition PLACED → PREPARING is rejected")
        void updateStatus_SkippedTransition_Throws() {
            order.setStatus(OrderStatus.PLACED);
            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

            assertThrows(InvalidOperationException.class,
                    () -> orderService.updateOrderStatus(orderId, OrderStatus.PREPARING, userId));
        }

        @Test
        @DisplayName("HOME_DELIVERY → READY_FOR_PICKUP is rejected (type incompatible)")
        void updateStatus_TypeIncompatible_Throws() {
            order.setStatus(OrderStatus.PREPARING);
            order.setType(OrderType.HOME_DELIVERY);
            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

            assertThrows(InvalidOperationException.class,
                    () -> orderService.updateOrderStatus(orderId, OrderStatus.READY_FOR_PICKUP, userId));
        }

        @Test
        @DisplayName("STORE_PICKUP → OUT_FOR_DELIVERY is rejected (type incompatible)")
        void updateStatus_PickupToDelivery_Throws() {
            order.setStatus(OrderStatus.PREPARING);
            order.setType(OrderType.STORE_PICKUP);
            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

            assertThrows(InvalidOperationException.class,
                    () -> orderService.updateOrderStatus(orderId, OrderStatus.OUT_FOR_DELIVERY, userId));
        }

        @Test
        @DisplayName("Using status endpoint for CANCELLED is rejected — must use cancel endpoint")
        void updateStatus_CancelledViaStatusEndpoint_Throws() {
            order.setStatus(OrderStatus.PLACED);
            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

            assertThrows(InvalidOperationException.class,
                    () -> orderService.updateOrderStatus(orderId, OrderStatus.CANCELLED, userId));
        }
    }

    // ===== Cancellation =====

    @Nested
    @DisplayName("Order Cancellation")
    class Cancellation {

        @Test
        @DisplayName("Customer cancellation succeeds from PLACED")
        void cancelCustomerOrder_FromPlaced_Succeeds() {
            order.setStatus(OrderStatus.PLACED);
            when(orderRepository.findByIdAndUserId(orderId, userId)).thenReturn(Optional.of(order));
            when(inventoryRepository.findByProductId(product.getId())).thenReturn(Optional.of(inventory));
            when(orderRepository.save(any(Order.class))).thenReturn(order);

            OrderDetailResponse result = orderService.cancelCustomerOrder(orderId, userId);

            assertEquals(OrderStatus.CANCELLED, result.getStatus());
            assertEquals(53, inventory.getAvailableQuantity()); // 50 + 3
            verify(inventoryRepository).save(inventory);
            verify(auditService).log(eq(userId), eq("ORDER_CANCELLED_BY_CUSTOMER"),
                    eq("Order"), eq(orderId.toString()), anyString());
        }

        @Test
        @DisplayName("Customer cancellation succeeds from CONFIRMED")
        void cancelCustomerOrder_FromConfirmed_Succeeds() {
            order.setStatus(OrderStatus.CONFIRMED);
            when(orderRepository.findByIdAndUserId(orderId, userId)).thenReturn(Optional.of(order));
            when(inventoryRepository.findByProductId(product.getId())).thenReturn(Optional.of(inventory));
            when(orderRepository.save(any(Order.class))).thenReturn(order);

            OrderDetailResponse result = orderService.cancelCustomerOrder(orderId, userId);

            assertEquals(OrderStatus.CANCELLED, result.getStatus());
        }

        @Test
        @DisplayName("Customer cancellation rejected from PREPARING")
        void cancelCustomerOrder_FromPreparing_Throws() {
            order.setStatus(OrderStatus.PREPARING);
            when(orderRepository.findByIdAndUserId(orderId, userId)).thenReturn(Optional.of(order));

            assertThrows(InvalidOperationException.class,
                    () -> orderService.cancelCustomerOrder(orderId, userId));
        }

        @Test
        @DisplayName("Customer cancellation rejected from DELIVERED")
        void cancelCustomerOrder_FromDelivered_Throws() {
            order.setStatus(OrderStatus.DELIVERED);
            when(orderRepository.findByIdAndUserId(orderId, userId)).thenReturn(Optional.of(order));

            assertThrows(InvalidOperationException.class,
                    () -> orderService.cancelCustomerOrder(orderId, userId));
        }

        @Test
        @DisplayName("Customer cannot cancel another user's order — returns 404")
        void cancelCustomerOrder_OtherUser_Throws404() {
            when(orderRepository.findByIdAndUserId(orderId, otherUserId)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> orderService.cancelCustomerOrder(orderId, otherUserId));
        }

        @Test
        @DisplayName("Cancellation restores inventory correctly")
        void cancelOrder_RestoresInventory() {
            order.setStatus(OrderStatus.PLACED);
            when(orderRepository.findByIdAndUserId(orderId, userId)).thenReturn(Optional.of(order));
            when(inventoryRepository.findByProductId(product.getId())).thenReturn(Optional.of(inventory));
            when(orderRepository.save(any(Order.class))).thenReturn(order);

            orderService.cancelCustomerOrder(orderId, userId);

            // Inventory was 50, item quantity was 3, should now be 53
            assertEquals(53, inventory.getAvailableQuantity());
            verify(inventoryRepository).save(inventory);
        }

        @Test
        @DisplayName("Admin cancellation creates proper audit log")
        void cancelAdminOrder_AuditsCorrectly() {
            order.setStatus(OrderStatus.CONFIRMED);
            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
            when(inventoryRepository.findByProductId(product.getId())).thenReturn(Optional.of(inventory));
            when(orderRepository.save(any(Order.class))).thenReturn(order);

            orderService.cancelAdminOrder(orderId, userId, "ADMIN");

            verify(auditService).log(eq(userId), eq("ORDER_CANCELLED_BY_STAFF"),
                    eq("Order"), eq(orderId.toString()), contains("ADMIN"));
        }
    }

    // ===== Admin Operations =====

    @Nested
    @DisplayName("Admin Order Operations")
    class AdminOperations {

        @Test
        @DisplayName("Admin retrieves all orders unfiltered")
        void getAdminOrders_NoFilter_ReturnsAll() {
            Page<Order> page = new PageImpl<>(List.of(order));
            when(orderRepository.findAll(any(Pageable.class))).thenReturn(page);

            Page<OrderSummaryResponse> result = orderService.getAdminOrders(0, 20, null, null);

            assertEquals(1, result.getContent().size());
            verify(orderRepository).findAll(any(Pageable.class));
        }

        @Test
        @DisplayName("Admin filters by status")
        void getAdminOrders_FilterByStatus() {
            Page<Order> page = new PageImpl<>(List.of(order));
            when(orderRepository.findByStatus(eq(OrderStatus.PLACED), any(Pageable.class))).thenReturn(page);

            orderService.getAdminOrders(0, 20, OrderStatus.PLACED, null);

            verify(orderRepository).findByStatus(eq(OrderStatus.PLACED), any(Pageable.class));
        }

        @Test
        @DisplayName("Admin filters by type")
        void getAdminOrders_FilterByType() {
            Page<Order> page = new PageImpl<>(List.of(order));
            when(orderRepository.findByType(eq(OrderType.STORE_PICKUP), any(Pageable.class))).thenReturn(page);

            orderService.getAdminOrders(0, 20, null, OrderType.STORE_PICKUP);

            verify(orderRepository).findByType(eq(OrderType.STORE_PICKUP), any(Pageable.class));
        }

        @Test
        @DisplayName("Admin filters by both status and type")
        void getAdminOrders_FilterByStatusAndType() {
            Page<Order> page = new PageImpl<>(List.of(order));
            when(orderRepository.findByStatusAndType(eq(OrderStatus.PLACED), eq(OrderType.HOME_DELIVERY), any(Pageable.class)))
                    .thenReturn(page);

            orderService.getAdminOrders(0, 20, OrderStatus.PLACED, OrderType.HOME_DELIVERY);

            verify(orderRepository).findByStatusAndType(
                    eq(OrderStatus.PLACED), eq(OrderType.HOME_DELIVERY), any(Pageable.class));
        }

        @Test
        @DisplayName("Admin retrieves any order details")
        void getAdminOrder_ReturnsDetails() {
            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

            OrderDetailResponse result = orderService.getAdminOrder(orderId);

            assertNotNull(result);
            assertEquals("ORD-ABCD1234", result.getOrderNumber());
        }

        @Test
        @DisplayName("Admin get non-existent order returns 404")
        void getAdminOrder_NotFound_Throws() {
            when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> orderService.getAdminOrder(orderId));
        }
    }
}

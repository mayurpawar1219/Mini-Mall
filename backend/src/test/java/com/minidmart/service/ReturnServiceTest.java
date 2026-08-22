package com.minidmart.service;

import com.minidmart.dto.EligibleItemResponse;
import com.minidmart.dto.RequestStatusUpdateDto;
import com.minidmart.dto.ReturnCreateRequest;
import com.minidmart.dto.ReturnResponse;
import com.minidmart.entity.*;
import com.minidmart.exception.InvalidOperationException;
import com.minidmart.repository.InventoryRepository;
import com.minidmart.repository.OrderItemRepository;
import com.minidmart.repository.OrderRepository;
import com.minidmart.repository.ReturnRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReturnServiceTest {

    @Mock
    private ReturnRequestRepository returnRequestRepository;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private OrderItemRepository orderItemRepository;
    @Mock
    private InventoryRepository inventoryRepository;
    @Mock
    private AuditService auditService;

    @InjectMocks
    private ReturnService returnService;

    private User user;
    private Order order;
    private OrderItem orderItem;
    private Product product;
    private Inventory inventory;

    @BeforeEach
    void setUp() {
        user = User.builder().id(UUID.randomUUID()).build();
        order = Order.builder().id(UUID.randomUUID()).user(user).status(OrderStatus.DELIVERED).completedAt(LocalDateTime.now()).orderNumber("ORD-1").build();
        product = Product.builder().id(1L).build();
        orderItem = OrderItem.builder().id(10L).order(order).product(product).quantity(2).build();
        order.setItems(List.of(orderItem));
        inventory = Inventory.builder().id(1L).product(product).availableQuantity(5).build();
    }

    @Test
    void getEligibleItems_Success() {
        when(orderRepository.findByIdAndUserId(order.getId(), user.getId())).thenReturn(Optional.of(order));
        when(returnRequestRepository.existsByOrderItemIdAndStatusNot(orderItem.getId(), RequestStatus.REJECTED)).thenReturn(false);

        List<EligibleItemResponse> items = returnService.getEligibleItems(order.getId(), user.getId());
        assertEquals(1, items.size());
        assertTrue(items.get(0).isEligibleForReturn());
    }

    @Test
    void createReturnRequest_Success() {
        ReturnCreateRequest request = new ReturnCreateRequest(10L, "Damaged");
        when(orderItemRepository.findById(10L)).thenReturn(Optional.of(orderItem));
        when(returnRequestRepository.existsByOrderItemIdAndStatusNot(10L, RequestStatus.REJECTED)).thenReturn(false);
        
        ReturnRequest saved = ReturnRequest.builder().id(1L).order(order).orderItem(orderItem).status(RequestStatus.PENDING).build();
        when(returnRequestRepository.save(any())).thenReturn(saved);

        ReturnResponse response = returnService.createReturnRequest(request, user.getId());
        assertEquals(1L, response.getId());
        assertEquals(RequestStatus.PENDING, response.getStatus());
    }

    @Test
    void createReturnRequest_ExpiredWindow_ThrowsException() {
        order.setCompletedAt(LocalDateTime.now().minusDays(15));
        ReturnCreateRequest request = new ReturnCreateRequest(10L, "Damaged");
        when(orderItemRepository.findById(10L)).thenReturn(Optional.of(orderItem));

        assertThrows(InvalidOperationException.class, () -> returnService.createReturnRequest(request, user.getId()));
    }

    @Test
    void updateStatus_ToCompleted_RestoresInventory() {
        ReturnRequest request = ReturnRequest.builder().id(1L).order(order).orderItem(orderItem).status(RequestStatus.APPROVED).build();
        when(returnRequestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(inventoryRepository.findByProductId(product.getId())).thenReturn(Optional.of(inventory));

        returnService.updateStatus(1L, new RequestStatusUpdateDto(RequestStatus.COMPLETED), UUID.randomUUID());

        assertEquals(RequestStatus.COMPLETED, request.getStatus());
        assertEquals(7, inventory.getAvailableQuantity()); // 5 + 2
        verify(inventoryRepository).save(inventory);
    }
}

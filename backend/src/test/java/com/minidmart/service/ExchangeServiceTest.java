package com.minidmart.service;

import com.minidmart.dto.ExchangeCreateRequest;
import com.minidmart.dto.ExchangeResponse;
import com.minidmart.dto.RequestStatusUpdateDto;
import com.minidmart.entity.*;
import com.minidmart.exception.InvalidOperationException;
import com.minidmart.repository.ExchangeRequestRepository;
import com.minidmart.repository.InventoryRepository;
import com.minidmart.repository.OrderItemRepository;
import com.minidmart.repository.OrderRepository;
import com.minidmart.repository.ProductRepository;
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
class ExchangeServiceTest {

    @Mock
    private ExchangeRequestRepository exchangeRequestRepository;
    @Mock
    private OrderItemRepository orderItemRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private InventoryRepository inventoryRepository;
    @Mock
    private AuditService auditService;

    @InjectMocks
    private ExchangeService exchangeService;

    private User user;
    private Order order;
    private OrderItem orderItem;
    private Product replacementProduct;
    private Inventory replacementInventory;
    private Inventory originalInventory;

    @BeforeEach
    void setUp() {
        user = User.builder().id(UUID.randomUUID()).build();
        order = Order.builder().id(UUID.randomUUID()).user(user).status(OrderStatus.DELIVERED).completedAt(LocalDateTime.now()).orderNumber("ORD-1").build();
        Product originalProduct = Product.builder().id(1L).build();
        orderItem = OrderItem.builder().id(10L).order(order).product(originalProduct).quantity(2).build();
        order.setItems(List.of(orderItem));

        replacementProduct = Product.builder().id(2L).active(true).build();
        
        originalInventory = Inventory.builder().id(100L).product(originalProduct).availableQuantity(0).build();
        replacementInventory = Inventory.builder().id(200L).product(replacementProduct).availableQuantity(5).build();
    }

    @Test
    void createExchangeRequest_Success() {
        ExchangeCreateRequest request = new ExchangeCreateRequest(10L, 2L, "Wrong size");
        when(orderItemRepository.findById(10L)).thenReturn(Optional.of(orderItem));
        when(productRepository.findById(2L)).thenReturn(Optional.of(replacementProduct));
        when(exchangeRequestRepository.existsByOriginalItemIdAndStatusNot(10L, RequestStatus.REJECTED)).thenReturn(false);

        ExchangeRequest saved = ExchangeRequest.builder().id(1L).order(order).originalItem(orderItem).replacementProduct(replacementProduct).status(RequestStatus.PENDING).build();
        when(exchangeRequestRepository.save(any())).thenReturn(saved);

        ExchangeResponse response = exchangeService.createExchangeRequest(request, user.getId());
        assertEquals(1L, response.getId());
        assertEquals(RequestStatus.PENDING, response.getStatus());
    }

    @Test
    void updateStatus_ToApproved_ReservesInventory() {
        ExchangeRequest request = ExchangeRequest.builder().id(1L).order(order).originalItem(orderItem).replacementProduct(replacementProduct).status(RequestStatus.PENDING).build();
        when(exchangeRequestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(inventoryRepository.findByProductId(2L)).thenReturn(Optional.of(replacementInventory));
        when(inventoryRepository.reserveInventorySafely(200L, 2)).thenReturn(1);

        exchangeService.updateStatus(1L, new RequestStatusUpdateDto(RequestStatus.APPROVED), UUID.randomUUID());

        verify(inventoryRepository).reserveInventorySafely(200L, 2);
        assertEquals(RequestStatus.APPROVED, request.getStatus());
    }
    
    @Test
    void updateStatus_ToCompleted_ConsumesReservedAndRestoresOriginal() {
        ExchangeRequest request = ExchangeRequest.builder().id(1L).order(order).originalItem(orderItem).replacementProduct(replacementProduct).status(RequestStatus.APPROVED).build();
        when(exchangeRequestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(inventoryRepository.findByProductId(2L)).thenReturn(Optional.of(replacementInventory));
        when(inventoryRepository.consumeReservedInventorySafely(200L, 2)).thenReturn(1);
        when(inventoryRepository.findByProductId(1L)).thenReturn(Optional.of(originalInventory));

        exchangeService.updateStatus(1L, new RequestStatusUpdateDto(RequestStatus.COMPLETED), UUID.randomUUID());

        verify(inventoryRepository).consumeReservedInventorySafely(200L, 2);
        verify(inventoryRepository).save(originalInventory);
        assertEquals(2, originalInventory.getAvailableQuantity());
        assertEquals(RequestStatus.COMPLETED, request.getStatus());
    }
}

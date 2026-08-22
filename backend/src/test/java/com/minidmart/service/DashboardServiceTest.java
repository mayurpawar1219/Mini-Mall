package com.minidmart.service;

import com.minidmart.dto.dashboard.CustomerDashboardResponse;
import com.minidmart.dto.dashboard.StaffDashboardResponse;
import com.minidmart.entity.Order;
import com.minidmart.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private InventoryRepository inventoryRepository;
    @Mock
    private ReturnRequestRepository returnRequestRepository;
    @Mock
    private ExchangeRequestRepository exchangeRequestRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private DashboardService dashboardService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(dashboardService, "lowStockThreshold", 10);
    }

    @Test
    void getCustomerDashboard_shouldReturnCorrectMetrics() {
        UUID userId = UUID.randomUUID();
        when(orderRepository.countByUserId(userId)).thenReturn(15L);
        when(orderRepository.countByUserIdAndStatusNotIn(eq(userId), any())).thenReturn(3L);
        
        Page<Order> emptyPage = new PageImpl<>(Collections.emptyList());
        when(orderRepository.findByUserId(eq(userId), any(PageRequest.class))).thenReturn(emptyPage);
        
        // Return requests mock
        when(returnRequestRepository.findByOrderUserId(eq(userId), any(PageRequest.class))).thenReturn(new PageImpl<>(Collections.emptyList()));
        when(exchangeRequestRepository.findByOrderUserId(eq(userId), any(PageRequest.class))).thenReturn(new PageImpl<>(Collections.emptyList()));

        CustomerDashboardResponse response = dashboardService.getCustomerDashboard(userId);

        assertEquals(15L, response.getTotalOrders());
        assertEquals(3L, response.getActiveOrders());
        assertEquals(0L, response.getPendingReturnExchangeRequests());
    }

    @Test
    void getStaffDashboard_shouldReturnCorrectMetrics() {
        when(orderRepository.countByStatusIn(any())).thenReturn(5L);
        when(inventoryRepository.countLowStockProducts(10)).thenReturn(2L);
        
        StaffDashboardResponse response = dashboardService.getStaffDashboard();
        
        assertEquals(5L, response.getOrdersRequiringAttention());
        assertEquals(2L, response.getLowStockProducts());
    }
}

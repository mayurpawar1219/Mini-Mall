package com.minidmart.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.minidmart.dto.OrderDetailResponse;
import com.minidmart.dto.OrderStatusUpdateRequest;
import com.minidmart.dto.OrderSummaryResponse;
import com.minidmart.entity.OrderStatus;
import com.minidmart.entity.OrderType;
import com.minidmart.entity.Role;
import com.minidmart.entity.User;
import com.minidmart.security.CustomUserDetails;
import com.minidmart.security.JwtAuthenticationFilter;
import com.minidmart.security.JwtTokenProvider;
import com.minidmart.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminOrderController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminOrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrderService orderService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private UsernamePasswordAuthenticationToken staffAuth;
    private UsernamePasswordAuthenticationToken adminAuth;

    @BeforeEach
    void setUp() {
        User staffUser = User.builder().id(UUID.randomUUID()).email("staff@test.com").role(Role.STAFF).build();
        CustomUserDetails staffDetails = new CustomUserDetails(staffUser);
        staffAuth = new UsernamePasswordAuthenticationToken(staffDetails, null, staffDetails.getAuthorities());

        User adminUser = User.builder().id(UUID.randomUUID()).email("admin@test.com").role(Role.ADMIN).build();
        CustomUserDetails adminDetails = new CustomUserDetails(adminUser);
        adminAuth = new UsernamePasswordAuthenticationToken(adminDetails, null, adminDetails.getAuthorities());
    }

    @Test
    @DisplayName("STAFF can list all orders — 200")
    void getAllOrders_Staff_Returns200() throws Exception {
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(staffAuth);
        OrderSummaryResponse summary = OrderSummaryResponse.builder()
                .id(UUID.randomUUID())
                .orderNumber("ORD-TEST")
                .status(OrderStatus.PLACED)
                .type(OrderType.STORE_PICKUP)
                .totalAmount(BigDecimal.TEN)
                .itemCount(1)
                .createdAt(LocalDateTime.now())
                .build();
        when(orderService.getAdminOrders(eq(0), eq(20), isNull(), isNull()))
                .thenReturn(new PageImpl<>(List.of(summary)));

        mockMvc.perform(get("/api/admin/orders").principal(staffAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("ADMIN can list all orders — 200")
    void getAllOrders_Admin_Returns200() throws Exception {
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(adminAuth);
        when(orderService.getAdminOrders(eq(0), eq(20), isNull(), isNull()))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/admin/orders").principal(adminAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("STAFF can update order status — 200")
    void updateStatus_Staff_Returns200() throws Exception {
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(staffAuth);
        UUID orderId = UUID.randomUUID();
        OrderStatusUpdateRequest request = new OrderStatusUpdateRequest(OrderStatus.CONFIRMED);
        OrderDetailResponse detail = OrderDetailResponse.builder()
                .id(orderId)
                .orderNumber("ORD-TEST")
                .status(OrderStatus.CONFIRMED)
                .type(OrderType.STORE_PICKUP)
                .totalAmount(BigDecimal.TEN)
                .items(List.of())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        when(orderService.updateOrderStatus(eq(orderId), eq(OrderStatus.CONFIRMED), any()))
                .thenReturn(detail);

        mockMvc.perform(patch("/api/admin/orders/" + orderId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .principal(staffAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"));
    }

    @Test
    @DisplayName("Invalid status in request body — 400")
    void updateStatus_InvalidStatus_Returns400() throws Exception {
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(staffAuth);
        mockMvc.perform(patch("/api/admin/orders/" + UUID.randomUUID() + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"INVALID_STATUS\"}")
                        .principal(staffAuth))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Missing status in request body — 400")
    void updateStatus_MissingStatus_Returns400() throws Exception {
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(staffAuth);
        mockMvc.perform(patch("/api/admin/orders/" + UUID.randomUUID() + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .principal(staffAuth))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("ADMIN can cancel order — 200")
    void cancelOrder_Admin_Returns200() throws Exception {
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(adminAuth);
        UUID orderId = UUID.randomUUID();
        OrderDetailResponse detail = OrderDetailResponse.builder()
                .id(orderId)
                .orderNumber("ORD-TEST")
                .status(OrderStatus.CANCELLED)
                .type(OrderType.STORE_PICKUP)
                .totalAmount(BigDecimal.TEN)
                .items(List.of())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        when(orderService.cancelAdminOrder(eq(orderId), any(), anyString())).thenReturn(detail);

        mockMvc.perform(post("/api/admin/orders/" + orderId + "/cancel")
                        .principal(adminAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));
    }
}

package com.minidmart.controller;

import com.minidmart.dto.OrderDetailResponse;
import com.minidmart.dto.OrderSummaryResponse;
import com.minidmart.entity.OrderStatus;
import com.minidmart.entity.OrderType;
import com.minidmart.entity.Role;
import com.minidmart.entity.User;
import com.minidmart.exception.ResourceNotFoundException;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
@AutoConfigureMockMvc(addFilters = false)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderService orderService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private User testUser;
    private CustomUserDetails testUserDetails;
    private UsernamePasswordAuthenticationToken authentication;

    @BeforeEach
    void setUp() {
        testUser = User.builder().id(UUID.randomUUID()).email("customer@test.com").role(Role.CUSTOMER).build();
        testUserDetails = new CustomUserDetails(testUser);
        authentication = new UsernamePasswordAuthenticationToken(
                testUserDetails, null, testUserDetails.getAuthorities());
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @Test
    @DisplayName("CUSTOMER can list own orders — 200")
    void getOrders_Customer_Returns200() throws Exception {
        OrderSummaryResponse summary = OrderSummaryResponse.builder()
                .id(UUID.randomUUID())
                .orderNumber("ORD-TEST1234")
                .status(OrderStatus.PLACED)
                .type(OrderType.STORE_PICKUP)
                .totalAmount(BigDecimal.valueOf(100))
                .itemCount(2)
                .createdAt(LocalDateTime.now())
                .build();
        when(orderService.getCustomerOrders(any(), eq(0), eq(20)))
                .thenReturn(new PageImpl<>(List.of(summary)));

        mockMvc.perform(get("/api/orders").principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].orderNumber").value("ORD-TEST1234"));
    }

    @Test
    @DisplayName("CUSTOMER can view own order details — 200")
    void getOrderDetail_OwnOrder_Returns200() throws Exception {
        UUID orderId = UUID.randomUUID();
        OrderDetailResponse detail = OrderDetailResponse.builder()
                .id(orderId)
                .orderNumber("ORD-TEST1234")
                .status(OrderStatus.PLACED)
                .type(OrderType.STORE_PICKUP)
                .totalAmount(BigDecimal.valueOf(100))
                .items(List.of())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        when(orderService.getCustomerOrder(eq(orderId), any())).thenReturn(detail);

        mockMvc.perform(get("/api/orders/" + orderId).principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderNumber").value("ORD-TEST1234"));
    }

    @Test
    @DisplayName("CUSTOMER gets 404 for non-existent or non-owned order (IDOR protection)")
    void getOrderDetail_NotOwned_Returns404() throws Exception {
        UUID orderId = UUID.randomUUID();
        when(orderService.getCustomerOrder(eq(orderId), any()))
                .thenThrow(new ResourceNotFoundException("Order", "id", orderId));

        mockMvc.perform(get("/api/orders/" + orderId).principal(authentication))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("CUSTOMER can cancel own order — 200")
    void cancelOrder_Customer_Returns200() throws Exception {
        UUID orderId = UUID.randomUUID();
        OrderDetailResponse detail = OrderDetailResponse.builder()
                .id(orderId)
                .orderNumber("ORD-TEST1234")
                .status(OrderStatus.CANCELLED)
                .type(OrderType.STORE_PICKUP)
                .totalAmount(BigDecimal.valueOf(100))
                .items(List.of())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        when(orderService.cancelCustomerOrder(eq(orderId), any())).thenReturn(detail);

        mockMvc.perform(post("/api/orders/" + orderId + "/cancel").principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));
    }
}

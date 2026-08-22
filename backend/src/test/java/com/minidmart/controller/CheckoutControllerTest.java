package com.minidmart.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.minidmart.dto.ApiResponse;
import com.minidmart.dto.CheckoutConfirmRequest;
import com.minidmart.dto.OrderResponse;
import com.minidmart.entity.OrderStatus;
import com.minidmart.entity.OrderType;
import com.minidmart.entity.Role;
import com.minidmart.entity.User;
import com.minidmart.security.CustomUserDetails;
import com.minidmart.security.JwtAuthenticationFilter;
import com.minidmart.security.JwtTokenProvider;
import com.minidmart.service.CheckoutService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CheckoutController.class)
@AutoConfigureMockMvc(addFilters = false) // Disable security filters for unit testing
class CheckoutControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CheckoutService checkoutService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private User testUser;
    private CustomUserDetails testUserDetails;
    private OrderResponse mockResponse;

    @BeforeEach
    void setUp() {
        testUser = User.builder().id(UUID.randomUUID()).email("test@test.com").role(Role.CUSTOMER).build();
        testUserDetails = new CustomUserDetails(testUser);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(testUserDetails, null, testUserDetails.getAuthorities())
        );

        CheckoutConfirmRequest request = CheckoutConfirmRequest.builder()
                .type(OrderType.STORE_PICKUP)
                .build();

        OrderResponse orderResponse = OrderResponse.builder()
                .id(UUID.randomUUID())
                .orderNumber("ORD-123")
                .status(OrderStatus.PLACED)
                .build();

        when(checkoutService.checkout(any(UUID.class), any(CheckoutConfirmRequest.class))).thenReturn(orderResponse);
    }

    @Test
    void checkout_ValidRequest_ReturnsOrder() throws Exception {
        CheckoutConfirmRequest request = new CheckoutConfirmRequest();
        request.setType(OrderType.STORE_PICKUP);
        request.setPaymentIntentId("pi_test_123");

        OrderResponse orderResponse = OrderResponse.builder()
                .id(UUID.randomUUID())
                .orderNumber("ORD-123")
                .status(OrderStatus.PLACED)
                .build();

        when(checkoutService.checkout(eq(testUser.getId()), any(CheckoutConfirmRequest.class))).thenReturn(orderResponse);

        mockMvc.perform(post("/api/checkout")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .principal(new UsernamePasswordAuthenticationToken(testUserDetails, null, testUserDetails.getAuthorities())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orderNumber").value("ORD-123"));
    }
}


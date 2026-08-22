package com.minidmart.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.minidmart.dto.OrderDetailResponse;
import com.minidmart.dto.SlotBookingRequest;
import com.minidmart.entity.OrderStatus;
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
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FulfillmentController.class)
@AutoConfigureMockMvc(addFilters = false)
class FulfillmentControllerTest {

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

    private UsernamePasswordAuthenticationToken customerAuth;
    private UsernamePasswordAuthenticationToken staffAuth;

    @BeforeEach
    void setUp() {
        User customerUser = User.builder().id(UUID.randomUUID()).email("customer@test.com").role(Role.CUSTOMER).build();
        CustomUserDetails customerDetails = new CustomUserDetails(customerUser);
        customerAuth = new UsernamePasswordAuthenticationToken(customerDetails, null, customerDetails.getAuthorities());

        User staffUser = User.builder().id(UUID.randomUUID()).email("staff@test.com").role(Role.STAFF).build();
        CustomUserDetails staffDetails = new CustomUserDetails(staffUser);
        staffAuth = new UsernamePasswordAuthenticationToken(staffDetails, null, staffDetails.getAuthorities());
    }

    @Test
    @DisplayName("CUSTOMER can book pickup slot — 200")
    void bookPickupSlot_Customer_Returns200() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(customerAuth);
        UUID orderId = UUID.randomUUID();
        SlotBookingRequest request = new SlotBookingRequest(1L);
        OrderDetailResponse detail = OrderDetailResponse.builder().id(orderId).pickupSlotId(1L).build();

        when(orderService.bookPickupSlot(eq(orderId), any(), eq(1L))).thenReturn(detail);

        mockMvc.perform(post("/api/orders/" + orderId + "/pickup-slot")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .principal(customerAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("STAFF can mark order ready for pickup — 200")
    void markReadyForPickup_Staff_Returns200() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(staffAuth);
        UUID orderId = UUID.randomUUID();
        OrderDetailResponse detail = OrderDetailResponse.builder().id(orderId).status(OrderStatus.READY_FOR_PICKUP).build();

        when(orderService.updateOrderStatus(eq(orderId), eq(OrderStatus.READY_FOR_PICKUP), any())).thenReturn(detail);

        mockMvc.perform(patch("/api/admin/orders/" + orderId + "/ready-for-pickup")
                        .principal(staffAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("READY_FOR_PICKUP"));
    }

    @Test
    @DisplayName("STAFF can mark order out for delivery — 200")
    void markOutForDelivery_Staff_Returns200() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(staffAuth);
        UUID orderId = UUID.randomUUID();
        OrderDetailResponse detail = OrderDetailResponse.builder().id(orderId).status(OrderStatus.OUT_FOR_DELIVERY).build();

        when(orderService.updateOrderStatus(eq(orderId), eq(OrderStatus.OUT_FOR_DELIVERY), any())).thenReturn(detail);

        mockMvc.perform(patch("/api/admin/orders/" + orderId + "/out-for-delivery")
                        .principal(staffAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("OUT_FOR_DELIVERY"));
    }
}

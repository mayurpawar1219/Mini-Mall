package com.minidmart.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.minidmart.dto.dashboard.AdminDashboardResponse;
import com.minidmart.dto.dashboard.CustomerDashboardResponse;
import com.minidmart.dto.dashboard.StaffDashboardResponse;
import com.minidmart.security.CustomUserDetails;
import com.minidmart.security.CustomUserDetailsService;
import com.minidmart.security.JwtTokenProvider;
import com.minidmart.service.DashboardService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

@WebMvcTest(DashboardController.class)
@AutoConfigureMockMvc
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DashboardService dashboardService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    @Test
    void getCustomerDashboard_withValidCustomer_shouldReturn200() throws Exception {
        UUID userId = UUID.randomUUID();
        com.minidmart.entity.User user = com.minidmart.entity.User.builder()
                .id(userId)
                .email("test@example.com")
                .password("password")
                .role(com.minidmart.entity.Role.CUSTOMER)
                .build();
        CustomUserDetails userDetails = new CustomUserDetails(user);

        CustomerDashboardResponse response = CustomerDashboardResponse.builder()
                .totalOrders(5)
                .build();

        when(dashboardService.getCustomerDashboard(any(UUID.class))).thenReturn(response);

        mockMvc.perform(get("/api/dashboard/customer")
                        .with(user(userDetails))) // Using the CustomUserDetails to avoid cast exceptions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalOrders").value(5));
    }

    @Test
    @WithMockUser(roles = "STAFF")
    void getStaffDashboard_withStaff_shouldReturn200() throws Exception {
        StaffDashboardResponse response = StaffDashboardResponse.builder()
                .ordersRequiringAttention(10)
                .build();

        when(dashboardService.getStaffDashboard()).thenReturn(response);

        mockMvc.perform(get("/api/dashboard/staff")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.ordersRequiringAttention").value(10));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAdminDashboard_withAdmin_shouldReturn200() throws Exception {
        AdminDashboardResponse response = AdminDashboardResponse.builder().build();

        when(dashboardService.getAdminDashboard(any(), any())).thenReturn(response);

        mockMvc.perform(get("/api/dashboard/admin/stats")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}

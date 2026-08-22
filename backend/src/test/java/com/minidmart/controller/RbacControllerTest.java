package com.minidmart.controller;

import com.minidmart.config.SecurityConfig;
import com.minidmart.security.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RbacTestController.class)
@Import({SecurityConfig.class, JwtAuthenticationEntryPoint.class, JwtAccessDeniedHandler.class, JwtAuthenticationFilter.class})
class RbacControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void customer_accessingCustomerResource_shouldBeAllowed() throws Exception {
        mockMvc.perform(get("/api/test-rbac/customer"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void customer_accessingStaffResource_shouldReturn403() throws Exception {
        mockMvc.perform(get("/api/test-rbac/staff"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void customer_accessingAdminResource_shouldReturn403() throws Exception {
        mockMvc.perform(get("/api/test-rbac/admin"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "STAFF")
    void staff_accessingStaffResource_shouldBeAllowed() throws Exception {
        mockMvc.perform(get("/api/test-rbac/staff"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "STAFF")
    void staff_accessingAdminResource_shouldReturn403() throws Exception {
        mockMvc.perform(get("/api/test-rbac/admin"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void admin_accessingAdminResource_shouldBeAllowed() throws Exception {
        mockMvc.perform(get("/api/test-rbac/admin"))
                .andExpect(status().isOk());
    }

    @Test
    void unauthenticated_accessingProtectedResource_shouldReturn401() throws Exception {
        mockMvc.perform(get("/api/test-rbac/customer"))
                .andExpect(status().isUnauthorized());
    }
}

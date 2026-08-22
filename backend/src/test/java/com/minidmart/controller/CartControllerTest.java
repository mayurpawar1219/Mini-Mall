package com.minidmart.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.minidmart.dto.CartItemRequest;
import com.minidmart.dto.CartResponse;
import com.minidmart.entity.Role;
import com.minidmart.entity.User;
import com.minidmart.security.CustomUserDetails;
import com.minidmart.security.JwtAuthenticationFilter;
import com.minidmart.security.JwtTokenProvider;
import com.minidmart.service.CartService;
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
import java.util.ArrayList;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CartController.class)
@AutoConfigureMockMvc(addFilters = false) // Disable security filters for simple controller testing
class CartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CartService cartService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private User testUser;
    private CustomUserDetails testUserDetails;
    private CartResponse mockResponse;

    @BeforeEach
    void setUp() {
        testUser = User.builder().id(UUID.randomUUID()).email("test@test.com").role(Role.CUSTOMER).build();
        testUserDetails = new CustomUserDetails(testUser);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(testUserDetails, null, testUserDetails.getAuthorities())
        );

        mockResponse = CartResponse.builder()
                .cartId(1L)
                .items(new ArrayList<>())
                .totalAmount(BigDecimal.ZERO)
                .build();
    }

    @Test
    void getCart_ReturnsCart() throws Exception {
        when(cartService.getCart(testUser.getId())).thenReturn(mockResponse);

        mockMvc.perform(get("/api/cart")
                .principal(new UsernamePasswordAuthenticationToken(testUserDetails, null, testUserDetails.getAuthorities())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void addItem_ValidRequest_ReturnsCart() throws Exception {
        CartItemRequest request = new CartItemRequest(100L, 2);
        
        when(cartService.addItemToCart(eq(testUser.getId()), any(CartItemRequest.class))).thenReturn(mockResponse);

        mockMvc.perform(post("/api/cart/items")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .principal(new UsernamePasswordAuthenticationToken(testUserDetails, null, testUserDetails.getAuthorities())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}


package com.minidmart.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.minidmart.dto.ProductRequest;
import com.minidmart.dto.ProductResponse;
import com.minidmart.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProductService productService;

    @MockBean
    private com.minidmart.security.JwtTokenProvider jwtTokenProvider;
    @MockBean
    private com.minidmart.security.CustomUserDetailsService customUserDetailsService;

    @Test
    void getAllProducts_shouldReturn200() throws Exception {
        ProductResponse res = ProductResponse.builder().id(1L).name("Test Product").build();
        when(productService.getAllProducts(true)).thenReturn(List.of(res));

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].name").value("Test Product"));
    }

    @Test
    void createProduct_withValidRequest_shouldReturn201() throws Exception {
        ProductRequest request = ProductRequest.builder()
                .sku("SKU-123")
                .name("New Product")
                .price(new BigDecimal("10.00"))
                .categoryId(1L)
                .stockQuantity(100)
                .imageUrl("http://example.com/image.jpg")
                .build();
        ProductResponse response = ProductResponse.builder().id(1L).name("New Product").build();

        when(productService.createProduct(any(ProductRequest.class), any())).thenReturn(response);

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("New Product"));
    }
}

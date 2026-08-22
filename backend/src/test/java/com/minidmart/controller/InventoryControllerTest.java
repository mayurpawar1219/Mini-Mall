package com.minidmart.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.minidmart.dto.InventoryAdjustmentRequest;
import com.minidmart.dto.InventoryResponse;
import com.minidmart.service.InventoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(InventoryController.class)
@AutoConfigureMockMvc(addFilters = false)
class InventoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private InventoryService inventoryService;

    @MockBean
    private com.minidmart.security.JwtTokenProvider jwtTokenProvider;
    @MockBean
    private com.minidmart.security.CustomUserDetailsService customUserDetailsService;

    @Test
    void getInventory_shouldReturn200() throws Exception {
        InventoryResponse res = InventoryResponse.builder().productId(1L).availableQuantity(50).build();
        when(inventoryService.getInventoryByProductId(1L)).thenReturn(res);

        mockMvc.perform(get("/api/inventory/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.availableQuantity").value(50));
    }

    @Test
    void adjustInventory_withValidRequest_shouldReturn200() throws Exception {
        InventoryAdjustmentRequest request = InventoryAdjustmentRequest.builder().quantityChange(10).build();
        InventoryResponse response = InventoryResponse.builder().productId(1L).availableQuantity(60).build();

        when(inventoryService.adjustInventory(eq(1L), any(InventoryAdjustmentRequest.class), any())).thenReturn(response);

        mockMvc.perform(patch("/api/inventory/1/stock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.availableQuantity").value(60));
    }
}

package com.minidmart.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.minidmart.dto.CategoryRequest;
import com.minidmart.dto.CategoryResponse;
import com.minidmart.service.CategoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CategoryController.class)
@AutoConfigureMockMvc(addFilters = false)
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CategoryService categoryService;

    // We also need these to satisfy the context for JwtAuthenticationFilter dependencies
    @MockBean
    private com.minidmart.security.JwtTokenProvider jwtTokenProvider;
    @MockBean
    private com.minidmart.security.CustomUserDetailsService customUserDetailsService;

    @Test
    void getAllCategories_shouldReturn200() throws Exception {
        CategoryResponse res = CategoryResponse.builder().id(1L).name("Test Category").build();
        when(categoryService.getAllCategories()).thenReturn(List.of(res));

        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].name").value("Test Category"));
    }

    @Test
    void createCategory_withValidRequest_shouldReturn201() throws Exception {
        CategoryRequest request = CategoryRequest.builder().name("New Category").build();
        CategoryResponse response = CategoryResponse.builder().id(1L).name("New Category").build();

        when(categoryService.createCategory(any(CategoryRequest.class), any())).thenReturn(response);

        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("New Category"));
    }
}

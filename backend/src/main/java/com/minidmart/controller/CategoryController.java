package com.minidmart.controller;

import com.minidmart.dto.ApiResponse;
import com.minidmart.dto.CategoryRequest;
import com.minidmart.dto.CategoryResponse;
import com.minidmart.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getAllCategories() {
        return ResponseEntity.ok(ApiResponse.success(categoryService.getAllCategories()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoryResponse>> getCategoryById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(categoryService.getCategoryById(id)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CategoryResponse>> createCategory(
            @Valid @RequestBody CategoryRequest request,
            java.security.Principal principal) {
        String adminId = principal != null ? principal.getName() : "test-admin";
        CategoryResponse response = categoryService.createCategory(request, adminId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CategoryResponse>> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody CategoryRequest request,
            java.security.Principal principal) {
        String adminId = principal != null ? principal.getName() : "test-admin";
        return ResponseEntity.ok(ApiResponse.success(categoryService.updateCategory(id, request, adminId)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(
            @PathVariable Long id,
            java.security.Principal principal) {
        String adminId = principal != null ? principal.getName() : "test-admin";
        categoryService.deleteCategory(id, adminId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}

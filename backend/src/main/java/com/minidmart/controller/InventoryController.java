package com.minidmart.controller;

import com.minidmart.dto.ApiResponse;
import com.minidmart.dto.InventoryAdjustmentRequest;
import com.minidmart.dto.InventoryResponse;
import com.minidmart.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping("/{productId}")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    public ResponseEntity<ApiResponse<InventoryResponse>> getInventory(@PathVariable Long productId) {
        return ResponseEntity.ok(ApiResponse.success(inventoryService.getInventoryByProductId(productId)));
    }

    @PatchMapping("/{productId}/stock")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    public ResponseEntity<ApiResponse<InventoryResponse>> adjustInventory(
            @PathVariable Long productId,
            @Valid @RequestBody InventoryAdjustmentRequest request,
            java.security.Principal principal) {
        String staffId = principal != null ? principal.getName() : "test-staff";
        return ResponseEntity.ok(ApiResponse.success(inventoryService.adjustInventory(productId, request, staffId)));
    }
}

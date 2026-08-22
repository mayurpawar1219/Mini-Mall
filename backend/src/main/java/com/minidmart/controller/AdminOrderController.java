package com.minidmart.controller;

import com.minidmart.dto.ApiResponse;
import com.minidmart.dto.OrderDetailResponse;
import com.minidmart.dto.OrderStatusUpdateRequest;
import com.minidmart.dto.OrderSummaryResponse;
import com.minidmart.entity.OrderStatus;
import com.minidmart.entity.OrderType;
import com.minidmart.security.CustomUserDetails;
import com.minidmart.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Staff/Admin order management endpoints.
 * Provides operational access to all orders with filtering and status management.
 */
@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {

    private final OrderService orderService;

    /**
     * List all orders with optional filtering by status and/or type.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    public ResponseEntity<ApiResponse<Page<OrderSummaryResponse>>> getAllOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) OrderType type) {
        return ResponseEntity.ok(ApiResponse.success(
                orderService.getAdminOrders(page, size, status, type)));
    }

    /**
     * Get full details of any order by ID.
     */
    @GetMapping("/{orderId}")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    public ResponseEntity<ApiResponse<OrderDetailResponse>> getOrder(
            @PathVariable UUID orderId) {
        return ResponseEntity.ok(ApiResponse.success(
                orderService.getAdminOrder(orderId)));
    }

    /**
     * Update order status. Validates the transition against the state machine.
     */
    @PatchMapping("/{orderId}/status")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    public ResponseEntity<ApiResponse<OrderDetailResponse>> updateOrderStatus(
            @PathVariable UUID orderId,
            @Valid @RequestBody OrderStatusUpdateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success(
                orderService.updateOrderStatus(orderId, request.getStatus(), userDetails.getId())));
    }

    /**
     * Cancel an eligible order. Restores inventory.
     */
    @PostMapping("/{orderId}/cancel")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    public ResponseEntity<ApiResponse<OrderDetailResponse>> cancelOrder(
            @PathVariable UUID orderId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        String role = userDetails.getUser().getRole().name();
        return ResponseEntity.ok(ApiResponse.success(
                orderService.cancelAdminOrder(orderId, userDetails.getId(), role)));
    }
}

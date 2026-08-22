package com.minidmart.controller;

import com.minidmart.dto.ApiResponse;
import com.minidmart.dto.OrderDetailResponse;
import com.minidmart.dto.OrderSummaryResponse;
import com.minidmart.security.CustomUserDetails;
import com.minidmart.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Customer-facing order endpoints.
 * All operations are scoped to the authenticated user's own orders.
 */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /**
     * List the authenticated customer's orders (paginated, newest first).
     */
    @GetMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<Page<OrderSummaryResponse>>> getMyOrders(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                orderService.getCustomerOrders(userDetails.getId(), page, size)));
    }

    /**
     * Get details of a specific order belonging to the authenticated customer.
     * Returns 404 if order does not exist or does not belong to the user.
     */
    @GetMapping("/{orderId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<OrderDetailResponse>> getMyOrder(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID orderId) {
        return ResponseEntity.ok(ApiResponse.success(
                orderService.getCustomerOrder(orderId, userDetails.getId())));
    }

    /**
     * Cancel the authenticated customer's own order.
     * Only allowed for PLACED or CONFIRMED orders.
     */
    @PostMapping("/{orderId}/cancel")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<OrderDetailResponse>> cancelMyOrder(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID orderId) {
        return ResponseEntity.ok(ApiResponse.success(
                orderService.cancelCustomerOrder(orderId, userDetails.getId())));
    }
}

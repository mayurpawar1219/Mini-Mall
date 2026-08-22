package com.minidmart.controller;

import com.minidmart.dto.OrderDetailResponse;
import com.minidmart.dto.SlotBookingRequest;
import com.minidmart.entity.OrderStatus;
import com.minidmart.security.CustomUserDetails;
import com.minidmart.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class FulfillmentController {

    private final OrderService orderService;

    // ---- Customer Booking ----

    @PostMapping("/orders/{id}/pickup-slot")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> bookPickupSlot(@PathVariable UUID id,
                                            @Valid @RequestBody SlotBookingRequest request,
                                            @AuthenticationPrincipal CustomUserDetails userDetails) {
        OrderDetailResponse response = orderService.bookPickupSlot(id, userDetails.getUser().getId(), request.getSlotId());
        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", response
        ));
    }

    // ---- Staff/Admin Operations ----

    @PatchMapping("/admin/orders/{id}/ready-for-pickup")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    public ResponseEntity<?> markReadyForPickup(@PathVariable UUID id,
                                                @AuthenticationPrincipal CustomUserDetails userDetails) {
        OrderDetailResponse response = orderService.updateOrderStatus(id, OrderStatus.READY_FOR_PICKUP, userDetails.getUser().getId());
        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", response
        ));
    }

    @PostMapping("/admin/orders/{id}/pickup-confirm")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    public ResponseEntity<?> confirmPickup(@PathVariable UUID id,
                                           @AuthenticationPrincipal CustomUserDetails userDetails) {
        OrderDetailResponse response = orderService.updateOrderStatus(id, OrderStatus.PICKED_UP, userDetails.getUser().getId());
        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", response
        ));
    }

    @PatchMapping("/admin/orders/{id}/out-for-delivery")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    public ResponseEntity<?> markOutForDelivery(@PathVariable UUID id,
                                                @AuthenticationPrincipal CustomUserDetails userDetails) {
        OrderDetailResponse response = orderService.updateOrderStatus(id, OrderStatus.OUT_FOR_DELIVERY, userDetails.getUser().getId());
        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", response
        ));
    }

    @PostMapping("/admin/orders/{id}/delivery-confirm")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    public ResponseEntity<?> confirmDelivery(@PathVariable UUID id,
                                             @AuthenticationPrincipal CustomUserDetails userDetails) {
        OrderDetailResponse response = orderService.updateOrderStatus(id, OrderStatus.DELIVERED, userDetails.getUser().getId());
        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", response
        ));
    }
}

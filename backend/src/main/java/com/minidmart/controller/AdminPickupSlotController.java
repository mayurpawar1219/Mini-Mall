package com.minidmart.controller;

import com.minidmart.dto.PickupSlotRequest;
import com.minidmart.dto.PickupSlotResponse;
import com.minidmart.security.CustomUserDetails;
import com.minidmart.service.PickupSlotService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/pickup-slots")
@RequiredArgsConstructor
public class AdminPickupSlotController {

    private final PickupSlotService pickupSlotService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAllSlots(@RequestParam(required = false) LocalDate date) {
        List<PickupSlotResponse> slots = pickupSlotService.getAllSlotsByDate(date);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", slots
        ));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createSlot(@Valid @RequestBody PickupSlotRequest request,
                                        @AuthenticationPrincipal CustomUserDetails userDetails) {
        PickupSlotResponse response = pickupSlotService.createSlot(request, userDetails.getUser().getId());
        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", response
        ));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateSlot(@PathVariable Long id,
                                        @Valid @RequestBody PickupSlotRequest request,
                                        @AuthenticationPrincipal CustomUserDetails userDetails) {
        PickupSlotResponse response = pickupSlotService.updateSlot(id, request, userDetails.getUser().getId());
        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", response
        ));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteSlot(@PathVariable Long id,
                                        @AuthenticationPrincipal CustomUserDetails userDetails) {
        pickupSlotService.deleteSlot(id, userDetails.getUser().getId());
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Pickup slot deleted successfully"
        ));
    }
}

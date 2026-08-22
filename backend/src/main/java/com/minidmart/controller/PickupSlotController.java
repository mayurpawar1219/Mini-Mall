package com.minidmart.controller;

import com.minidmart.dto.PickupSlotResponse;
import com.minidmart.service.PickupSlotService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.web.bind.annotation.RequestParam;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/pickup-slots")
@RequiredArgsConstructor
public class PickupSlotController {

    private final PickupSlotService pickupSlotService;

    @GetMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> getAvailableSlots(@RequestParam LocalDate date) {
        List<PickupSlotResponse> slots = pickupSlotService.getAvailableSlots(date);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", slots
        ));
    }
}

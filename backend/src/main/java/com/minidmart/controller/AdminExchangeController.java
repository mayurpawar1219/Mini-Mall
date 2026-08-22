package com.minidmart.controller;

import com.minidmart.dto.RequestStatusUpdateDto;
import com.minidmart.dto.ExchangeResponse;
import com.minidmart.security.CustomUserDetails;
import com.minidmart.service.ExchangeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/exchanges")
@RequiredArgsConstructor
public class AdminExchangeController {

    private final ExchangeService exchangeService;

    @GetMapping
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    public ResponseEntity<?> getAllExchanges(@RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "20") int size) {
        Page<ExchangeResponse> exchanges = exchangeService.getAllExchanges(page, size);
        return ResponseEntity.ok(Map.of("success", true, "data", exchanges));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    public ResponseEntity<?> updateExchangeStatus(@PathVariable Long id,
                                                  @Valid @RequestBody RequestStatusUpdateDto request,
                                                  @AuthenticationPrincipal CustomUserDetails userDetails) {
        ExchangeResponse response = exchangeService.updateStatus(id, request, userDetails.getUser().getId());
        return ResponseEntity.ok(Map.of("success", true, "data", response));
    }
}

package com.minidmart.controller;

import com.minidmart.dto.ExchangeCreateRequest;
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
@RequestMapping("/api")
@RequiredArgsConstructor
public class CustomerExchangeController {

    private final ExchangeService exchangeService;

    @PostMapping("/exchanges")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> createExchangeRequest(@Valid @RequestBody ExchangeCreateRequest request,
                                                   @AuthenticationPrincipal CustomUserDetails userDetails) {
        ExchangeResponse response = exchangeService.createExchangeRequest(request, userDetails.getUser().getId());
        return ResponseEntity.ok(Map.of("success", true, "data", response));
    }

    @GetMapping("/exchanges")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> getMyExchanges(@RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "20") int size,
                                            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Page<ExchangeResponse> exchanges = exchangeService.getCustomerExchanges(userDetails.getUser().getId(), page, size);
        return ResponseEntity.ok(Map.of("success", true, "data", exchanges));
    }

    @DeleteMapping("/exchanges/{id}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> cancelExchangeRequest(@PathVariable Long id,
                                                   @AuthenticationPrincipal CustomUserDetails userDetails) {
        exchangeService.cancelCustomerExchange(id, userDetails.getUser().getId());
        return ResponseEntity.ok(Map.of("success", true, "message", "Exchange request cancelled successfully"));
    }
}

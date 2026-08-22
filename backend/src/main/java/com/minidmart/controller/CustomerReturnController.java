package com.minidmart.controller;

import com.minidmart.dto.EligibleItemResponse;
import com.minidmart.dto.ReturnCreateRequest;
import com.minidmart.dto.ReturnResponse;
import com.minidmart.security.CustomUserDetails;
import com.minidmart.service.ReturnService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CustomerReturnController {

    private final ReturnService returnService;

    @GetMapping("/orders/{orderId}/return-eligible-items")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> getEligibleReturnItems(@PathVariable UUID orderId,
                                                    @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<EligibleItemResponse> items = returnService.getEligibleItems(orderId, userDetails.getUser().getId());
        return ResponseEntity.ok(Map.of("success", true, "data", items));
    }

    @PostMapping("/returns")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> createReturnRequest(@Valid @RequestBody ReturnCreateRequest request,
                                                 @AuthenticationPrincipal CustomUserDetails userDetails) {
        ReturnResponse response = returnService.createReturnRequest(request, userDetails.getUser().getId());
        return ResponseEntity.ok(Map.of("success", true, "data", response));
    }

    @GetMapping("/returns")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> getMyReturns(@RequestParam(defaultValue = "0") int page,
                                          @RequestParam(defaultValue = "20") int size,
                                          @AuthenticationPrincipal CustomUserDetails userDetails) {
        Page<ReturnResponse> returns = returnService.getCustomerReturns(userDetails.getUser().getId(), page, size);
        return ResponseEntity.ok(Map.of("success", true, "data", returns));
    }

    @DeleteMapping("/returns/{id}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> cancelReturnRequest(@PathVariable Long id,
                                                 @AuthenticationPrincipal CustomUserDetails userDetails) {
        returnService.cancelCustomerReturn(id, userDetails.getUser().getId());
        return ResponseEntity.ok(Map.of("success", true, "message", "Return request cancelled successfully"));
    }
}

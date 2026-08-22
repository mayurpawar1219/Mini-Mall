package com.minidmart.controller;

import com.minidmart.dto.RequestStatusUpdateDto;
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

import java.util.Map;

@RestController
@RequestMapping("/api/admin/returns")
@RequiredArgsConstructor
public class AdminReturnController {

    private final ReturnService returnService;

    @GetMapping
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    public ResponseEntity<?> getAllReturns(@RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "20") int size) {
        Page<ReturnResponse> returns = returnService.getAllReturns(page, size);
        return ResponseEntity.ok(Map.of("success", true, "data", returns));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    public ResponseEntity<?> updateReturnStatus(@PathVariable Long id,
                                                @Valid @RequestBody RequestStatusUpdateDto request,
                                                @AuthenticationPrincipal CustomUserDetails userDetails) {
        ReturnResponse response = returnService.updateStatus(id, request, userDetails.getUser().getId());
        return ResponseEntity.ok(Map.of("success", true, "data", response));
    }
}

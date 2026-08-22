package com.minidmart.controller;

import com.minidmart.dto.ApiResponse;
import com.minidmart.dto.PasswordChangeRequest;
import com.minidmart.dto.ProfileUpdateRequest;
import com.minidmart.dto.UserResponse;
import com.minidmart.security.CustomUserDetails;
import com.minidmart.service.ProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Customer profile management endpoints.
 * All operations are scoped to the authenticated user only.
 */
@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
@Tag(name = "Profile", description = "Customer profile management")
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping
    @Operation(summary = "Get current user's profile")
    public ResponseEntity<ApiResponse<UserResponse>> getProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success(
                profileService.getProfile(userDetails.getId())));
    }

    @PutMapping
    @Operation(summary = "Update current user's profile")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            @Valid @RequestBody ProfileUpdateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success(
                "Profile updated successfully",
                profileService.updateProfile(userDetails.getId(), request)));
    }

    @PutMapping("/password")
    @Operation(summary = "Change current user's password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody PasswordChangeRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        profileService.changePassword(userDetails.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Password changed successfully", null));
    }
}

package com.minidmart.controller;

import com.minidmart.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller strictly for testing RBAC rules.
 * Intentionally excluded from Swagger/OpenAPI docs if possible.
 */
@RestController
@RequestMapping("/api/test-rbac")
public class RbacTestController {

    @GetMapping("/customer")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('STAFF') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> customerResource() {
        return ResponseEntity.ok(ApiResponse.success("Accessible by CUSTOMER"));
    }

    @GetMapping("/staff")
    @PreAuthorize("hasRole('STAFF') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> staffResource() {
        return ResponseEntity.ok(ApiResponse.success("Accessible by STAFF"));
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> adminResource() {
        return ResponseEntity.ok(ApiResponse.success("Accessible by ADMIN"));
    }
}

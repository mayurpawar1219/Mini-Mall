package com.minidmart.controller;

import com.minidmart.dto.ApiResponse;
import com.minidmart.dto.dashboard.AdminDashboardResponse;
import com.minidmart.dto.dashboard.CustomerDashboardResponse;
import com.minidmart.dto.dashboard.StaffDashboardResponse;
import com.minidmart.security.CustomUserDetails;
import com.minidmart.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/customer")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<CustomerDashboardResponse>> getCustomerDashboard(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        CustomerDashboardResponse response = dashboardService.getCustomerDashboard(userDetails.getId());
        return ResponseEntity.ok(ApiResponse.success("Customer dashboard retrieved successfully", response));
    }

    @GetMapping("/staff")
    @PreAuthorize("hasRole('STAFF')")
    public ResponseEntity<ApiResponse<StaffDashboardResponse>> getStaffDashboard() {
        StaffDashboardResponse response = dashboardService.getStaffDashboard();
        return ResponseEntity.ok(ApiResponse.success("Staff dashboard retrieved successfully", response));
    }

    @GetMapping("/admin/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AdminDashboardResponse>> getAdminDashboard(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        AdminDashboardResponse response = dashboardService.getAdminDashboard(startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success("Admin dashboard retrieved successfully", response));
    }

    @GetMapping("/admin/sales-trends")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Object>> getSalesTrends(
            @RequestParam(required = false, defaultValue = "7d") String period) {
        return ResponseEntity.ok(ApiResponse.success(dashboardService.getSalesTrends(period)));
    }

    @GetMapping("/admin/low-stock")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<Object>> getLowStockProducts() {
        return ResponseEntity.ok(ApiResponse.success(dashboardService.getLowStockProducts()));
    }
}

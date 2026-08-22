package com.minidmart.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerDashboardResponse {
    private long totalOrders;
    private long activeOrders;
    private long completedOrders;
    private long cancelledOrders;
    private long pendingReturnExchangeRequests;
    private java.util.List<com.minidmart.dto.OrderSummaryResponse> recentOrders;
}

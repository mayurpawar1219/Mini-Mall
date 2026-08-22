package com.minidmart.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffDashboardResponse {
    private long ordersRequiringAttention;
    private FulfillmentStatisticsDto fulfillmentOverview;
    private long pendingReturnRequests;
    private long pendingExchangeRequests;
    private long lowStockProducts;

    private long activeOrders;
    private long pendingPrep;
    private long pickupToday;
    private long deliveryToday;
}

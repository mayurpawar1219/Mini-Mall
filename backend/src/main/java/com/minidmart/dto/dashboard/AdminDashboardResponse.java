package com.minidmart.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import com.minidmart.entity.AuditLog;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminDashboardResponse {
    private OrderStatisticsDto orderMetrics;
    private SalesStatisticsDto salesMetrics;
    private InventoryStatisticsDto inventoryMetrics;
    private FulfillmentStatisticsDto fulfillmentMetrics;
    private ReturnExchangeStatisticsDto returnExchangeMetrics;
    private UserStatisticsDto userMetrics;
    private List<AuditLog> recentActivity;

    private java.math.BigDecimal totalRevenue;
    private long totalOrders;
    private long activeCustomers;
    private long totalProducts;
}

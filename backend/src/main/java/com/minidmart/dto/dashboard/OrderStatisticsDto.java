package com.minidmart.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatisticsDto {
    private long totalOrders;
    private long ordersToday;
    private long ordersThisWeek;
    private long ordersThisMonth;
    private java.util.Map<String, Long> byStatus;
    private java.util.Map<String, Long> byType;
}

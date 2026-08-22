package com.minidmart.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalesStatisticsDto {
    private BigDecimal totalOrderValue;
    private BigDecimal salesToday;
    private BigDecimal salesThisWeek;
    private BigDecimal salesThisMonth;
    private BigDecimal averageOrderValue;
}

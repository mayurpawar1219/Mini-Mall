package com.minidmart.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryStatisticsDto {
    private long totalProducts;
    private long lowStockProducts;
    private long outOfStockProducts;
}

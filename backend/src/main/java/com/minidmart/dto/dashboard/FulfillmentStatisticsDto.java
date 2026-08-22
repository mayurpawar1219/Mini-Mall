package com.minidmart.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FulfillmentStatisticsDto {
    private long storePickupCount;
    private long scheduledPickupCount;
    private long homeDeliveryCount;
    private long pendingFulfillment;
    private long completedFulfillment;
}

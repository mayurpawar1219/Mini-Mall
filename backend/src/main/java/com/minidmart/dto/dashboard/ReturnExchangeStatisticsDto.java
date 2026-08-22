package com.minidmart.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReturnExchangeStatisticsDto {
    private long pendingReturns;
    private long approvedReturns;
    private long completedReturns;
    private long pendingExchanges;
    private long approvedExchanges;
    private long completedExchanges;
}

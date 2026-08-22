package com.minidmart.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EligibleItemResponse {
    private Long orderItemId;
    private String orderNumber;
    private Long productId;
    private String productName;
    private BigDecimal priceAtPurchase;
    private int quantity;
    private boolean eligibleForReturn;
    private boolean eligibleForExchange;
    private String ineligibilityReason;
}

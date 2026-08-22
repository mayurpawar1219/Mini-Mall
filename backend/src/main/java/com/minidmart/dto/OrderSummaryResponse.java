package com.minidmart.dto;

import com.minidmart.entity.OrderStatus;
import com.minidmart.entity.OrderType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderSummaryResponse {
    private UUID id;
    private String orderNumber;
    private OrderStatus status;
    private OrderType type;
    private BigDecimal totalAmount;
    private int itemCount;
    private LocalDateTime createdAt;
}

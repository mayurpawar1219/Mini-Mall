package com.minidmart.dto;

import com.minidmart.entity.OrderStatus;
import com.minidmart.entity.OrderType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderDetailResponse {
    private UUID id;
    private String orderNumber;
    private OrderStatus status;
    private OrderType type;
    private BigDecimal totalAmount;
    private List<OrderItemResponse> items;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private String customerName;
    private String customerEmail;

    // Delivery info (populated only for HOME_DELIVERY orders)
    private String deliveryAddress;
    private String deliveryCity;
    private String deliveryPostalCode;
    private String deliveryPhone;
    
    private Long pickupSlotId;
    private LocalDateTime pickupSlotStartTime;
    private LocalDateTime pickupSlotEndTime;
}

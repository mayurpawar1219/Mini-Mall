package com.minidmart.dto;

import com.minidmart.entity.OrderType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutConfirmRequest {
    
    @NotNull(message = "Order type is required")
    private OrderType type;
    
    @NotNull(message = "Payment Intent ID is required")
    private String paymentIntentId;
    
    // Pickups
    private Long pickupSlotId;
    
    // Deliveries
    private String deliveryAddress;
    private String deliveryCity;
    private String deliveryPostalCode;
    private String deliveryPhone;
}

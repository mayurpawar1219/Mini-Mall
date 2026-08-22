package com.minidmart.dto;

import com.minidmart.entity.RequestStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExchangeResponse {
    private Long id;
    private String orderNumber;
    private Long originalItemId;
    private String originalProductName;
    private Long replacementProductId;
    private String replacementProductName;
    private int quantity;
    private String reason;
    private RequestStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime resolvedAt;
}

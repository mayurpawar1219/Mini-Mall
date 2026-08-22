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
public class ReturnResponse {
    private Long id;
    private String orderNumber;
    private Long orderItemId;
    private String productName;
    private int quantity;
    private String reason;
    private RequestStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime resolvedAt;
}

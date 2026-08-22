package com.minidmart.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReturnCreateRequest {
    @NotNull(message = "Order item ID is required")
    private Long orderItemId;

    @NotBlank(message = "Reason is required")
    private String reason;
}

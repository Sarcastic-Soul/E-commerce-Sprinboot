package com.anish.e_commerce.dto;

import com.anish.e_commerce.model.OrderStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class OrderStatusUpdateRequest {

    @NotNull(message = "status is required")
    private OrderStatus status;
}

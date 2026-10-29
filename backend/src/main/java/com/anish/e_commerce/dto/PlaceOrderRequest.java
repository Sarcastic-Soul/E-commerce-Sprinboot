package com.anish.e_commerce.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PlaceOrderRequest {

    @NotNull(message = "addressId is required")
    private Long addressId;

    private String couponCode;
}

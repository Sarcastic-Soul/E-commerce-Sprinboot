package com.anish.e_commerce.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CouponValidateRequest {

    @NotBlank(message = "Coupon code is required")
    private String code;
}

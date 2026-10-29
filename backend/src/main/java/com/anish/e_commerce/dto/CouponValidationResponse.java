package com.anish.e_commerce.dto;

import java.math.BigDecimal;

public record CouponValidationResponse(
    String code,
    BigDecimal subtotal,
    BigDecimal discount,
    BigDecimal total,
    String message
) {}

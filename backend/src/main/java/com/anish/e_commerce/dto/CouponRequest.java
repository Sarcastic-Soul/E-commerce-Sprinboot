package com.anish.e_commerce.dto;

import com.anish.e_commerce.model.Coupon.CouponType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class CouponRequest {

    @NotBlank(message = "Code is required")
    @Pattern(regexp = "^[A-Za-z0-9_-]{3,32}$", message = "Code must be 3-32 letters, digits, _ or -")
    private String code;

    @NotNull(message = "Type is required")
    private CouponType type;

    @NotNull(message = "Value is required")
    @DecimalMin(value = "0.01", message = "Value must be positive")
    private BigDecimal value;

    @DecimalMin(value = "0", message = "Minimum order amount cannot be negative")
    private BigDecimal minOrderAmount;

    @Min(value = 1, message = "Max uses must be at least 1")
    private Integer maxUses;

    private LocalDateTime expiresAt;

    private Boolean active;
}

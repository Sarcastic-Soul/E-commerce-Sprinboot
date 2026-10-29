package com.anish.e_commerce.controller;

import com.anish.e_commerce.dto.CouponRequest;
import com.anish.e_commerce.dto.CouponValidateRequest;
import com.anish.e_commerce.dto.CouponValidationResponse;
import com.anish.e_commerce.model.Coupon;
import com.anish.e_commerce.service.CouponService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    @PostMapping("/coupons/validate")
    public ResponseEntity<CouponValidationResponse> validate(
        @Valid @RequestBody CouponValidateRequest request,
        Authentication authentication
    ) {
        return ResponseEntity.ok(
            couponService.validateForCart(authentication.getName(), request.getCode())
        );
    }

    @GetMapping("/admin/coupons")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Coupon>> list() {
        return ResponseEntity.ok(couponService.findAll());
    }

    @PostMapping("/admin/coupons")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Coupon> create(@Valid @RequestBody CouponRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(couponService.create(request));
    }

    @PutMapping("/admin/coupons/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Coupon> update(
        @PathVariable Long id,
        @Valid @RequestBody CouponRequest request
    ) {
        return ResponseEntity.ok(couponService.update(id, request));
    }

    @DeleteMapping("/admin/coupons/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        couponService.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}

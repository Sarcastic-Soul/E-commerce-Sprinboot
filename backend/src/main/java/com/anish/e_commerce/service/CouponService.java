package com.anish.e_commerce.service;

import com.anish.e_commerce.dto.CartItemResponse;
import com.anish.e_commerce.dto.CouponRequest;
import com.anish.e_commerce.dto.CouponValidationResponse;
import com.anish.e_commerce.exception.BusinessException;
import com.anish.e_commerce.exception.ResourceNotFoundException;
import com.anish.e_commerce.model.Coupon;
import com.anish.e_commerce.model.Coupon.CouponType;
import com.anish.e_commerce.repo.CartItemRepo;
import com.anish.e_commerce.repo.CouponRepo;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CouponService {

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private final CouponRepo couponRepo;
    private final CartItemRepo cartItemRepo;

    public static String normalize(String code) {
        return code == null ? null : code.trim().toUpperCase(Locale.ROOT);
    }

    /** Previews the coupon against the caller's current cart. */
    @Transactional(readOnly = true)
    public CouponValidationResponse validateForCart(String username, String code) {
        List<CartItemResponse> items = cartItemRepo.findItemsByUsername(username);
        if (items.isEmpty()) {
            throw new BusinessException("Cart is empty");
        }
        BigDecimal subtotal = items
            .stream()
            .map(i -> i.getPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .setScale(2, RoundingMode.HALF_UP);

        Coupon coupon = requireApplicable(code, subtotal);
        BigDecimal discount = discountFor(coupon, subtotal);
        return new CouponValidationResponse(
            coupon.getCode(),
            subtotal,
            discount,
            subtotal.subtract(discount),
            "Coupon applied: you save ₹" + discount.toPlainString()
        );
    }

    /**
     * Looks the code up and checks every rule against the given subtotal. Throws a
     * BusinessException with a message the user can act on if any rule fails.
     */
    @Transactional(readOnly = true)
    public Coupon requireApplicable(String code, BigDecimal subtotal) {
        Coupon coupon = couponRepo
            .findByCode(normalize(code))
            .orElseThrow(() -> new BusinessException("Invalid coupon code"));

        if (!coupon.isActive()) {
            throw new BusinessException("This coupon is no longer active");
        }
        if (
            coupon.getExpiresAt() != null &&
            !coupon.getExpiresAt().isAfter(LocalDateTime.now())
        ) {
            throw new BusinessException("This coupon has expired");
        }
        if (
            coupon.getMaxUses() != null &&
            coupon.getUsedCount() >= coupon.getMaxUses()
        ) {
            throw new BusinessException("This coupon has been fully used");
        }
        BigDecimal min = coupon.getMinOrderAmount() == null
            ? BigDecimal.ZERO
            : coupon.getMinOrderAmount();
        if (subtotal.compareTo(min) < 0) {
            throw new BusinessException(
                "This coupon needs a minimum order of ₹" +
                    min.setScale(2, RoundingMode.HALF_UP).toPlainString()
            );
        }
        return coupon;
    }

    /** Scale 2, HALF_UP, and never more than the subtotal itself. */
    public BigDecimal discountFor(Coupon coupon, BigDecimal subtotal) {
        BigDecimal discount = coupon.getType() == CouponType.PERCENT
            ? subtotal.multiply(coupon.getValue()).divide(HUNDRED, 2, RoundingMode.HALF_UP)
            : coupon.getValue().setScale(2, RoundingMode.HALF_UP);
        return discount.min(subtotal).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Called when an order becomes PAID. The payment has already been taken by
     * then, so losing the race for the last use is logged rather than thrown.
     */
    @Transactional
    public void recordUse(String code) {
        if (couponRepo.incrementUsage(normalize(code)) == 0) {
            log.warn("Coupon {} was used past its maxUses by a paid order", code);
        }
    }

    // ---------- admin CRUD ----------

    public List<Coupon> findAll() {
        return couponRepo.findAllByOrderByIdDesc();
    }

    @Transactional
    public Coupon create(CouponRequest request) {
        String code = normalize(request.getCode());
        if (couponRepo.existsByCode(code)) {
            throw new BusinessException("A coupon with code " + code + " already exists");
        }
        Coupon coupon = new Coupon();
        coupon.setCode(code);
        apply(coupon, request);
        return couponRepo.save(coupon);
    }

    @Transactional
    public Coupon update(Long id, CouponRequest request) {
        Coupon coupon = couponRepo
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Coupon not found with id: " + id));
        String code = normalize(request.getCode());
        if (!code.equals(coupon.getCode()) && couponRepo.existsByCode(code)) {
            throw new BusinessException("A coupon with code " + code + " already exists");
        }
        coupon.setCode(code);
        apply(coupon, request);
        return couponRepo.save(coupon);
    }

    /** Deactivates rather than deletes: paid orders still reference the code. */
    @Transactional
    public void deactivate(Long id) {
        Coupon coupon = couponRepo
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Coupon not found with id: " + id));
        coupon.setActive(false);
        couponRepo.save(coupon);
    }

    private void apply(Coupon coupon, CouponRequest request) {
        BigDecimal value = request.getValue().setScale(2, RoundingMode.HALF_UP);
        if (
            request.getType() == CouponType.PERCENT &&
            (value.compareTo(BigDecimal.ONE) < 0 || value.compareTo(new BigDecimal("90")) > 0)
        ) {
            throw new BusinessException("Percent coupons must be between 1 and 90");
        }
        coupon.setType(request.getType());
        coupon.setValue(value);
        coupon.setMinOrderAmount(
            request.getMinOrderAmount() == null
                ? BigDecimal.ZERO
                : request.getMinOrderAmount().setScale(2, RoundingMode.HALF_UP)
        );
        coupon.setMaxUses(request.getMaxUses());
        coupon.setExpiresAt(request.getExpiresAt());
        coupon.setActive(request.getActive() == null || request.getActive());
    }
}

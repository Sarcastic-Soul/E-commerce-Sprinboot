package com.anish.e_commerce.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.anish.e_commerce.dto.CartItemResponse;
import com.anish.e_commerce.dto.CouponRequest;
import com.anish.e_commerce.dto.CouponValidationResponse;
import com.anish.e_commerce.exception.BusinessException;
import com.anish.e_commerce.model.Coupon;
import com.anish.e_commerce.model.Coupon.CouponType;
import com.anish.e_commerce.repo.CartItemRepo;
import com.anish.e_commerce.repo.CouponRepo;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CouponServiceTest {

    @Mock
    private CouponRepo couponRepo;

    @Mock
    private CartItemRepo cartItemRepo;

    @InjectMocks
    private CouponService couponService;

    private static Coupon coupon(CouponType type, String value, String min) {
        Coupon c = new Coupon();
        c.setCode("TEST");
        c.setType(type);
        c.setValue(new BigDecimal(value));
        c.setMinOrderAmount(new BigDecimal(min));
        return c;
    }

    @Test
    void percentDiscount_IsRoundedHalfUpToTwoPlaces() {
        Coupon tenPercent = coupon(CouponType.PERCENT, "10", "0");
        // 10% of 199.95 = 19.995 -> 20.00
        assertEquals(new BigDecimal("20.00"), couponService.discountFor(tenPercent, new BigDecimal("199.95")));
        // 10% of 199.94 = 19.994 -> 19.99
        assertEquals(new BigDecimal("19.99"), couponService.discountFor(tenPercent, new BigDecimal("199.94")));
    }

    @Test
    void flatDiscount_NeverExceedsSubtotal() {
        Coupon fifty = coupon(CouponType.FLAT, "50", "0");
        assertEquals(new BigDecimal("50.00"), couponService.discountFor(fifty, new BigDecimal("600.00")));
        assertEquals(new BigDecimal("30.00"), couponService.discountFor(fifty, new BigDecimal("30.00")));
    }

    @Test
    void lookupIsCaseInsensitive() {
        Coupon c = coupon(CouponType.PERCENT, "10", "0");
        when(couponRepo.findByCode("WELCOME10")).thenReturn(Optional.of(c));

        assertSame(c, couponService.requireApplicable("  welcome10 ", new BigDecimal("100")));
    }

    @Test
    void unknownCodeIsRejected() {
        when(couponRepo.findByCode("NOPE")).thenReturn(Optional.empty());
        BusinessException e = assertThrows(
            BusinessException.class,
            () -> couponService.requireApplicable("nope", BigDecimal.TEN)
        );
        assertEquals("Invalid coupon code", e.getMessage());
    }

    @Test
    void expiredCouponIsRejected() {
        Coupon c = coupon(CouponType.PERCENT, "10", "0");
        c.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        when(couponRepo.findByCode("TEST")).thenReturn(Optional.of(c));

        BusinessException e = assertThrows(
            BusinessException.class,
            () -> couponService.requireApplicable("test", BigDecimal.TEN)
        );
        assertTrue(e.getMessage().contains("expired"));
    }

    @Test
    void fullyUsedCouponIsRejected() {
        Coupon c = coupon(CouponType.PERCENT, "10", "0");
        c.setMaxUses(3);
        c.setUsedCount(3);
        when(couponRepo.findByCode("TEST")).thenReturn(Optional.of(c));

        BusinessException e = assertThrows(
            BusinessException.class,
            () -> couponService.requireApplicable("test", BigDecimal.TEN)
        );
        assertTrue(e.getMessage().contains("fully used"));
    }

    @Test
    void inactiveCouponIsRejected() {
        Coupon c = coupon(CouponType.PERCENT, "10", "0");
        c.setActive(false);
        when(couponRepo.findByCode("TEST")).thenReturn(Optional.of(c));

        assertThrows(BusinessException.class, () -> couponService.requireApplicable("test", BigDecimal.TEN));
    }

    @Test
    void belowMinimumIsRejectedWithTheMinimumInTheMessage() {
        Coupon c = coupon(CouponType.FLAT, "50", "500");
        when(couponRepo.findByCode("TEST")).thenReturn(Optional.of(c));

        BusinessException e = assertThrows(
            BusinessException.class,
            () -> couponService.requireApplicable("test", new BigDecimal("499.99"))
        );
        assertTrue(e.getMessage().contains("500.00"));

        // Exactly the minimum is fine.
        assertSame(c, couponService.requireApplicable("test", new BigDecimal("500.00")));
    }

    @Test
    void validateForCart_ComputesAgainstCurrentCart() {
        when(cartItemRepo.findItemsByUsername("bob")).thenReturn(
            List.of(
                new CartItemResponse(1, "A", new BigDecimal("69.99"), "u", 2),
                new CartItemResponse(2, "B", new BigDecimal("10.00"), "u", 1)
            )
        );
        Coupon c = coupon(CouponType.PERCENT, "10", "0");
        c.setCode("WELCOME10");
        when(couponRepo.findByCode("WELCOME10")).thenReturn(Optional.of(c));

        CouponValidationResponse r = couponService.validateForCart("bob", "welcome10");

        assertEquals("WELCOME10", r.code());
        assertEquals(new BigDecimal("149.98"), r.subtotal());
        assertEquals(new BigDecimal("15.00"), r.discount());
        assertEquals(new BigDecimal("134.98"), r.total());
    }

    @Test
    void validateForCart_EmptyCartIsRejected() {
        when(cartItemRepo.findItemsByUsername("bob")).thenReturn(List.of());
        assertThrows(BusinessException.class, () -> couponService.validateForCart("bob", "X"));
        verifyNoInteractions(couponRepo);
    }

    @Test
    void create_RejectsPercentOutsideOneToNinety() {
        CouponRequest r = new CouponRequest();
        r.setCode("big");
        r.setType(CouponType.PERCENT);
        r.setValue(new BigDecimal("95"));
        when(couponRepo.existsByCode("BIG")).thenReturn(false);

        assertThrows(BusinessException.class, () -> couponService.create(r));
        verify(couponRepo, never()).save(any());
    }

    @Test
    void create_StoresCodeUpperCase() {
        CouponRequest r = new CouponRequest();
        r.setCode("summer");
        r.setType(CouponType.FLAT);
        r.setValue(new BigDecimal("25"));
        when(couponRepo.existsByCode("SUMMER")).thenReturn(false);
        when(couponRepo.save(any(Coupon.class))).thenAnswer(i -> i.getArgument(0));

        Coupon saved = couponService.create(r);

        assertEquals("SUMMER", saved.getCode());
        assertEquals(new BigDecimal("25.00"), saved.getValue());
        assertEquals(BigDecimal.ZERO, saved.getMinOrderAmount());
        assertTrue(saved.isActive());
    }

    @Test
    void recordUse_UsesTheConditionalIncrement() {
        when(couponRepo.incrementUsage("WELCOME10")).thenReturn(0);
        // Losing the race is logged, not thrown: the payment is already taken.
        assertDoesNotThrow(() -> couponService.recordUse("welcome10"));
        verify(couponRepo).incrementUsage("WELCOME10");
    }
}

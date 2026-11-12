package com.anish.e_commerce.controller;

import com.anish.e_commerce.config.RateLimitFilter;
import com.anish.e_commerce.service.OrderExpiryJob;
import com.anish.e_commerce.service.RefreshTokenService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints for the hosting platform's scheduler, not for users. Vercel Cron
 * calls them with GET and "Authorization: Bearer $CRON_SECRET". With no secret
 * configured they stay disabled.
 */
@RestController
@RequestMapping("/api/internal")
public class InternalController {

    private final OrderExpiryJob orderExpiryJob;
    private final RefreshTokenService refreshTokenService;
    private final RateLimitFilter rateLimitFilter;
    private final String cronSecret;

    public InternalController(
        OrderExpiryJob orderExpiryJob,
        RefreshTokenService refreshTokenService,
        RateLimitFilter rateLimitFilter,
        @Value("${app.cron-secret:}") String cronSecret
    ) {
        this.orderExpiryJob = orderExpiryJob;
        this.refreshTokenService = refreshTokenService;
        this.rateLimitFilter = rateLimitFilter;
        this.cronSecret = cronSecret;
    }

    /**
     * Daily housekeeping: expires unpaid orders and deletes expired refresh tokens
     * and rate-limit rows. Each step is idempotent, so a repeated or missed call
     * (Vercel Cron is best effort) does no harm.
     */
    @GetMapping("/cleanup")
    public ResponseEntity<Map<String, Object>> cleanup(
        @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization
    ) {
        if (cronSecret.isBlank()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        if (!matches(authorization, "Bearer " + cronSecret)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(Map.of(
            "expiredOrders", orderExpiryJob.sweep(),
            "deletedRefreshTokens", refreshTokenService.deleteExpired(),
            "deletedRateLimitBuckets", rateLimitFilter.removeExpiredBuckets()
        ));
    }

    /** Constant-time compare, so the secret cannot be guessed byte by byte. */
    private static boolean matches(String given, String expected) {
        return given != null && MessageDigest.isEqual(
            given.getBytes(StandardCharsets.UTF_8),
            expected.getBytes(StandardCharsets.UTF_8)
        );
    }
}

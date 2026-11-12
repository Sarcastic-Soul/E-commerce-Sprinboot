package com.anish.e_commerce.service;

import com.anish.e_commerce.model.RefreshToken;
import com.anish.e_commerce.repo.RefreshTokenRepo;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class RefreshTokenService {

    private final RefreshTokenRepo refreshTokenRepo;
    private final long refreshTokenDurationMs;

    public RefreshTokenService(
            RefreshTokenRepo refreshTokenRepo,
            @Value("${jwt.refresh.expiration}") long refreshTokenDurationMs) {
        this.refreshTokenRepo = refreshTokenRepo;
        this.refreshTokenDurationMs = refreshTokenDurationMs;
    }

    public RefreshToken createRefreshToken(Long userId) {
        RefreshToken refreshToken = RefreshToken.builder()
                .token(UUID.randomUUID().toString())
                .userId(userId)
                .expiresAt(Instant.now().plusMillis(refreshTokenDurationMs))
                .build();

        return refreshTokenRepo.save(refreshToken);
    }

    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepo.findById(token);
    }

    /**
     * Returns the token if it is still valid. An expired one is deleted and null is
     * returned, so {@code findByToken(t).map(this::verifyExpiration)} is empty.
     */
    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.isExpired()) {
            refreshTokenRepo.delete(token);
            return null;
        }
        return token;
    }

    public void deleteByUserId(Long userId) {
        refreshTokenRepo.deleteByUserId(userId);
    }

    public void deleteByToken(String token) {
        refreshTokenRepo.deleteById(token);
    }

    /**
     * Removes tokens past their expiry; returns how many were deleted. Runs daily
     * on a long-running server; on serverless the cleanup endpoint calls it.
     */
    @Scheduled(cron = "0 30 3 * * *")
    public int deleteExpired() {
        return refreshTokenRepo.deleteExpired(Instant.now());
    }
}

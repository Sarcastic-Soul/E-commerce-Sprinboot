package com.anish.e_commerce.service;

import com.anish.e_commerce.model.RefreshToken;
import com.anish.e_commerce.repo.RefreshTokenRepo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

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
        // Redis @TimeToLive usually works in seconds
        long expirationInSeconds = refreshTokenDurationMs / 1000;

        RefreshToken refreshToken = RefreshToken.builder()
                .token(UUID.randomUUID().toString())
                .userId(userId)
                .expiration(expirationInSeconds)
                .build();

        return refreshTokenRepo.save(refreshToken);
    }

    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepo.findByToken(token);
    }

    public RefreshToken verifyExpiration(RefreshToken token) {
        // Since we are using Redis with @TimeToLive, the token will be automatically
        // evicted from the database once it expires. If it exists here, it is valid.
        return token;
    }

    public void deleteByUserId(Long userId) {
        refreshTokenRepo.deleteByUserId(userId);
    }

    public void deleteByToken(String token) {
        refreshTokenRepo.deleteById(token);
    }
}

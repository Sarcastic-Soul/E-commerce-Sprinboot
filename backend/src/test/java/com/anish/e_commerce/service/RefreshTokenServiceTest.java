package com.anish.e_commerce.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.anish.e_commerce.model.RefreshToken;
import com.anish.e_commerce.repo.RefreshTokenRepo;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    private static final long DURATION_MS = 604_800_000L; // 7 days

    @Mock
    private RefreshTokenRepo refreshTokenRepo;

    private RefreshTokenService refreshTokenService;

    @BeforeEach
    void setUp() {
        refreshTokenService = new RefreshTokenService(
            refreshTokenRepo,
            DURATION_MS
        );
    }

    @Test
    void createRefreshToken_SavesTokenWithExpirationInSecondsNotMillis() {
        when(refreshTokenRepo.save(any(RefreshToken.class))).thenAnswer(
            i -> i.getArgument(0)
        );

        RefreshToken token = refreshTokenService.createRefreshToken(7L);

        assertNotNull(token.getToken());
        assertEquals(7L, token.getUserId());
        // Redis' @TimeToLive is read in seconds; the configured duration is in ms.
        assertEquals(DURATION_MS / 1000, token.getExpiration());

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(
            RefreshToken.class
        );
        verify(refreshTokenRepo, times(1)).save(captor.capture());
        assertEquals(token.getToken(), captor.getValue().getToken());
    }

    @Test
    void createRefreshToken_GeneratesDistinctTokensPerCall() {
        when(refreshTokenRepo.save(any(RefreshToken.class))).thenAnswer(
            i -> i.getArgument(0)
        );

        RefreshToken first = refreshTokenService.createRefreshToken(1L);
        RefreshToken second = refreshTokenService.createRefreshToken(1L);

        assertNotEquals(first.getToken(), second.getToken());
    }

    @Test
    void findByToken_DelegatesToRepo() {
        RefreshToken stored = RefreshToken.builder()
            .token("abc")
            .userId(3L)
            .expiration(60L)
            .build();
        when(refreshTokenRepo.findByToken("abc")).thenReturn(
            Optional.of(stored)
        );

        Optional<RefreshToken> result = refreshTokenService.findByToken("abc");

        assertTrue(result.isPresent());
        assertEquals(3L, result.get().getUserId());
    }

    @Test
    void findByToken_ReturnsEmptyWhenExpiredOrMissing() {
        // Redis evicts on @TimeToLive, so an expired/missing token is simply absent.
        when(refreshTokenRepo.findByToken("gone")).thenReturn(Optional.empty());

        assertTrue(refreshTokenService.findByToken("gone").isEmpty());
    }

    @Test
    void verifyExpiration_ReturnsTokenUnchanged() {
        RefreshToken token = RefreshToken.builder()
            .token("abc")
            .userId(3L)
            .expiration(60L)
            .build();

        assertSame(token, refreshTokenService.verifyExpiration(token));
    }

    @Test
    void deleteByToken_DelegatesToRepo() {
        refreshTokenService.deleteByToken("abc");
        verify(refreshTokenRepo, times(1)).deleteById("abc");
    }

    @Test
    void deleteByUserId_DelegatesToRepo() {
        refreshTokenService.deleteByUserId(5L);
        verify(refreshTokenRepo, times(1)).deleteByUserId(5L);
    }
}

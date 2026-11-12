package com.anish.e_commerce.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.anish.e_commerce.model.RefreshToken;
import com.anish.e_commerce.repo.RefreshTokenRepo;
import java.time.Duration;
import java.time.Instant;
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
        refreshTokenService = new RefreshTokenService(refreshTokenRepo, DURATION_MS);
    }

    private static RefreshToken token(String value, Instant expiresAt) {
        return RefreshToken.builder().token(value).userId(3L).expiresAt(expiresAt).build();
    }

    @Test
    void createRefreshToken_SavesTokenExpiringAfterConfiguredDuration() {
        when(refreshTokenRepo.save(any(RefreshToken.class))).thenAnswer(i -> i.getArgument(0));
        Instant before = Instant.now();

        RefreshToken token = refreshTokenService.createRefreshToken(7L);

        assertNotNull(token.getToken());
        assertEquals(7L, token.getUserId());
        Instant expected = before.plusMillis(DURATION_MS);
        assertTrue(Duration.between(expected, token.getExpiresAt()).abs().toSeconds() < 5);

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepo, times(1)).save(captor.capture());
        assertEquals(token.getToken(), captor.getValue().getToken());
    }

    @Test
    void createRefreshToken_GeneratesDistinctTokensPerCall() {
        when(refreshTokenRepo.save(any(RefreshToken.class))).thenAnswer(i -> i.getArgument(0));

        RefreshToken first = refreshTokenService.createRefreshToken(1L);
        RefreshToken second = refreshTokenService.createRefreshToken(1L);

        assertNotEquals(first.getToken(), second.getToken());
    }

    @Test
    void findByToken_DelegatesToRepo() {
        when(refreshTokenRepo.findById("abc")).thenReturn(
            Optional.of(token("abc", Instant.now().plusSeconds(60)))
        );

        Optional<RefreshToken> result = refreshTokenService.findByToken("abc");

        assertTrue(result.isPresent());
        assertEquals(3L, result.get().getUserId());
    }

    @Test
    void findByToken_ReturnsEmptyWhenMissing() {
        when(refreshTokenRepo.findById("gone")).thenReturn(Optional.empty());

        assertTrue(refreshTokenService.findByToken("gone").isEmpty());
    }

    @Test
    void verifyExpiration_ReturnsValidTokenUnchanged() {
        RefreshToken valid = token("abc", Instant.now().plusSeconds(60));

        assertSame(valid, refreshTokenService.verifyExpiration(valid));
        verify(refreshTokenRepo, never()).delete(any());
    }

    @Test
    void verifyExpiration_DeletesExpiredTokenAndReturnsNull() {
        RefreshToken expired = token("old", Instant.now().minusSeconds(1));

        assertNull(refreshTokenService.verifyExpiration(expired));
        verify(refreshTokenRepo).delete(expired);
        // Through Optional.map, as AuthController uses it, an expired token is empty.
        assertTrue(Optional.of(expired).map(refreshTokenService::verifyExpiration).isEmpty());
    }

    @Test
    void deleteByToken_DelegatesToRepo() {
        refreshTokenService.deleteByToken("abc");
        verify(refreshTokenRepo, times(1)).deleteById("abc");
    }

    @Test
    void deleteByUserId_DeletesEveryTokenOfThatUser() {
        refreshTokenService.deleteByUserId(5L);
        verify(refreshTokenRepo, times(1)).deleteByUserId(5L);
    }

    @Test
    void deleteExpired_DelegatesWithCurrentTime() {
        when(refreshTokenRepo.deleteExpired(any())).thenReturn(4);
        assertEquals(4, refreshTokenService.deleteExpired());
    }
}

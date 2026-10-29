package com.anish.e_commerce.jwt;

import static org.junit.jupiter.api.Assertions.*;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;
import java.util.Date;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JwtUtilTest {

    private static final String SECRET =
        "01234567890123456789012345678901"; // 32 bytes, satisfies HS256

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(SECRET, 900_000L); // 15 min
    }

    @Test
    void generateToken_RoundTripsUsernameAndRoles() {
        String token = jwtUtil.generateToken(
            "alice",
            Set.of("USER", "ADMIN")
        );

        assertTrue(jwtUtil.isTokenValid(token));
        assertEquals("alice", jwtUtil.extractUsername(token));
        assertTrue(jwtUtil.extractRoles(token).containsAll(
            List.of("USER", "ADMIN")
        ));
    }

    @Test
    void isTokenValid_FalseForGarbageToken() {
        assertFalse(jwtUtil.isTokenValid("not-a-jwt"));
    }

    @Test
    void isTokenValid_FalseForExpiredToken() {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes());
        String expired = Jwts.builder()
            .subject("alice")
            .issuedAt(new Date(System.currentTimeMillis() - 100_000))
            .expiration(new Date(System.currentTimeMillis() - 50_000))
            .signWith(key, Jwts.SIG.HS256)
            .compact();

        assertFalse(jwtUtil.isTokenValid(expired));
    }

    @Test
    void isTokenValid_FalseWhenSignedWithADifferentKey() {
        String tokenFromAnotherSecret = new JwtUtil(
            "different-32-byte-secret-value!!",
            900_000L
        ).generateToken("alice", Set.of("USER"));

        assertFalse(jwtUtil.isTokenValid(tokenFromAnotherSecret));
    }

    @Test
    void extractRoles_EmptyWhenClaimMissing() {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes());
        String noRolesClaim = Jwts.builder()
            .subject("bob")
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + 60_000))
            .signWith(key, Jwts.SIG.HS256)
            .compact();

        assertTrue(jwtUtil.extractRoles(noRolesClaim).isEmpty());
    }

    @Test
    void constructor_FallsBackToGeneratedKeyWhenSecretTooShort() {
        // Must not throw, and the generated key must still produce valid tokens.
        JwtUtil devUtil = new JwtUtil("short", 900_000L);
        String token = devUtil.generateToken("alice", Set.of("USER"));

        assertTrue(devUtil.isTokenValid(token));
    }
}

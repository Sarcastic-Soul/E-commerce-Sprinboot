package com.anish.e_commerce.jwt;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/**
 * Builds the httpOnly cookies that carry the access and refresh tokens. Keeping
 * both tokens out of JS-readable storage (was: localStorage) closes the XSS
 * exfiltration path — a script injected via a compromised dependency or a stored-
 * XSS bug could previously read both tokens directly.
 *
 * secure/sameSite are configurable because the right values differ by environment:
 * cross-origin prod deploy needs SameSite=None + Secure (both required together, and
 * None is not sent at all over plain http), while local http dev needs Secure=false
 * with SameSite=Lax (still sent cross-port on localhost, since "site" is the
 * registrable domain, not the port).
 */
@Component
public class JwtCookieProperties {

    public static final String ACCESS_COOKIE = "access_token";
    public static final String REFRESH_COOKIE = "refresh_token";

    private final boolean secure;
    private final String sameSite;
    private final long accessMaxAgeMs;
    private final long refreshMaxAgeMs;

    public JwtCookieProperties(
        @Value("${app.jwt.cookie.secure:true}") boolean secure,
        @Value("${app.jwt.cookie.same-site:None}") String sameSite,
        @Value("${jwt.expiration}") long accessMaxAgeMs,
        @Value("${jwt.refresh.expiration}") long refreshMaxAgeMs
    ) {
        this.secure = secure;
        this.sameSite = sameSite;
        this.accessMaxAgeMs = accessMaxAgeMs;
        this.refreshMaxAgeMs = refreshMaxAgeMs;
    }

    public ResponseCookie accessCookie(String token) {
        return build(ACCESS_COOKIE, token, Duration.ofMillis(accessMaxAgeMs));
    }

    public ResponseCookie refreshCookie(String token) {
        return build(REFRESH_COOKIE, token, Duration.ofMillis(refreshMaxAgeMs));
    }

    public ResponseCookie clearAccessCookie() {
        return build(ACCESS_COOKIE, "", Duration.ZERO);
    }

    public ResponseCookie clearRefreshCookie() {
        return build(REFRESH_COOKIE, "", Duration.ZERO);
    }

    private ResponseCookie build(String name, String value, Duration maxAge) {
        return ResponseCookie.from(name, value)
            .httpOnly(true)
            .secure(secure)
            .sameSite(sameSite)
            .path("/")
            .maxAge(maxAge)
            .build();
    }
}

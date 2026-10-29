package com.anish.e_commerce.controller;

import com.anish.e_commerce.dto.ApiErrorResponse;
import com.anish.e_commerce.exception.ResourceNotFoundException;
import com.anish.e_commerce.jwt.JwtCookieProperties;
import com.anish.e_commerce.jwt.JwtUtil;
import com.anish.e_commerce.model.RefreshToken;
import com.anish.e_commerce.model.User;
import com.anish.e_commerce.repo.UserRepo;
import com.anish.e_commerce.service.RefreshTokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import lombok.*;
import org.springframework.http.*;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepo repo;
    private final PasswordEncoder encoder;
    private final AuthenticationManager authManager;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;
    private final JwtCookieProperties cookieProperties;

    @PostMapping("/signup")
    public ResponseEntity<?> signup(
        @Valid @RequestBody AuthRequest request,
        HttpServletRequest httpRequest
    ) {
        if (repo.findByUsername(request.getUsername()).isPresent()) {
            return ResponseEntity.badRequest().body(
                errorBody(HttpStatus.BAD_REQUEST, "Username already exists", httpRequest)
            );
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(encoder.encode(request.getPassword()));
        // First user becomes ADMIN, rest are USER
        if (repo.count() == 0) {
            user.setRoles(Set.of("ADMIN"));
        } else {
            user.setRoles(Set.of("USER"));
        }

        repo.save(user);
        return ResponseEntity.ok(
            Map.of("message", "User registered successfully")
        );
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(
        @Valid @RequestBody AuthRequest request,
        HttpServletResponse httpResponse,
        HttpServletRequest httpRequest
    ) {
        try {
            authManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    request.getUsername(),
                    request.getPassword()
                )
            );

            User user = repo
                .findByUsername(request.getUsername())
                .orElseThrow(() ->
                    new ResourceNotFoundException("User not found")
                );
            RefreshToken refreshToken = refreshTokenService.createRefreshToken(
                user.getId()
            );
            issueTokens(httpResponse, user, refreshToken.getToken());

            return ResponseEntity.ok(userInfo(user.getUsername(), user.getRoles()));
        } catch (AuthenticationException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                errorBody(HttpStatus.UNAUTHORIZED, "Invalid credentials", httpRequest)
            );
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshtoken(
        @CookieValue(value = JwtCookieProperties.REFRESH_COOKIE, required = false) String refreshToken,
        HttpServletResponse httpResponse,
        HttpServletRequest httpRequest
    ) {
        if (refreshToken == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                errorBody(HttpStatus.FORBIDDEN, "No refresh token present.", httpRequest)
            );
        }

        Optional<RefreshToken> stored = refreshTokenService
            .findByToken(refreshToken)
            .map(refreshTokenService::verifyExpiration);

        if (stored.isEmpty()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                errorBody(
                    HttpStatus.FORBIDDEN,
                    "Refresh token is not in database or expired!",
                    httpRequest
                )
            );
        }

        User user = repo
            .findById(stored.get().getUserId())
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        issueTokens(httpResponse, user, refreshToken);
        return ResponseEntity.ok(userInfo(user.getUsername(), user.getRoles()));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(
        @CookieValue(value = JwtCookieProperties.REFRESH_COOKIE, required = false) String refreshToken,
        HttpServletResponse httpResponse
    ) {
        if (refreshToken != null) {
            refreshTokenService.deleteByToken(refreshToken);
        }
        clearTokens(httpResponse);
        return ResponseEntity.ok(Map.of("message", "Log out successful"));
    }

    /** Lets the SPA re-derive who is logged in from the httpOnly cookie alone. */
    @GetMapping("/me")
    public ResponseEntity<?> me(
        Authentication authentication,
        HttpServletRequest httpRequest
    ) {
        // Spring Security's AnonymousAuthenticationFilter fills the context with an
        // anonymous, "authenticated" token whenever nothing else did — including on
        // permitAll routes like this one — so isAuthenticated() alone is never
        // false here. AnonymousAuthenticationToken is the actual "nobody is logged
        // in" signal.
        if (
            authentication == null ||
            !authentication.isAuthenticated() ||
            authentication instanceof AnonymousAuthenticationToken
        ) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                errorBody(HttpStatus.UNAUTHORIZED, "Not authenticated.", httpRequest)
            );
        }
        Set<String> roles = authentication
            .getAuthorities()
            .stream()
            .map(GrantedAuthority::getAuthority)
            .map(a -> a.startsWith("ROLE_") ? a.substring(5) : a)
            .collect(Collectors.toSet());
        return ResponseEntity.ok(userInfo(authentication.getName(), roles));
    }

    /**
     * Forces the deferred CSRF token to resolve (and its cookie to be written) on a
     * plain GET, so the SPA can call this once on load and have the XSRF-TOKEN
     * cookie ready before the first mutating request needs it.
     */
    @GetMapping("/csrf")
    public ResponseEntity<Void> csrf(
        org.springframework.security.web.csrf.CsrfToken token
    ) {
        return ResponseEntity.ok().build();
    }

    private void issueTokens(
        HttpServletResponse httpResponse,
        User user,
        String refreshToken
    ) {
        String accessToken = jwtUtil.generateToken(
            user.getUsername(),
            user.getRoles()
        );
        httpResponse.addHeader(
            HttpHeaders.SET_COOKIE,
            cookieProperties.accessCookie(accessToken).toString()
        );
        httpResponse.addHeader(
            HttpHeaders.SET_COOKIE,
            cookieProperties.refreshCookie(refreshToken).toString()
        );
    }

    private void clearTokens(HttpServletResponse httpResponse) {
        httpResponse.addHeader(
            HttpHeaders.SET_COOKIE,
            cookieProperties.clearAccessCookie().toString()
        );
        httpResponse.addHeader(
            HttpHeaders.SET_COOKIE,
            cookieProperties.clearRefreshCookie().toString()
        );
    }

    private Map<String, Object> userInfo(String username, Set<String> roles) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("username", username);
        body.put("roles", roles);
        return body;
    }

    private ApiErrorResponse errorBody(
        HttpStatus status,
        String message,
        HttpServletRequest request
    ) {
        return ApiErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(status.value())
            .error(status.getReasonPhrase())
            .message(message)
            .path(request.getRequestURI())
            .build();
    }

    @Data
    public static class AuthRequest {

        @NotBlank(message = "Username cannot be blank")
        @Size(
            min = 3,
            max = 20,
            message = "Username must be between 3 and 20 characters"
        )
        private String username;

        @NotBlank(message = "Password cannot be blank")
        @Size(min = 6, message = "Password must be at least 6 characters")
        private String password;
    }
}

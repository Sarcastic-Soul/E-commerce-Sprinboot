package com.anish.e_commerce.controller;

import com.anish.e_commerce.jwt.JwtUtil;
import com.anish.e_commerce.model.RefreshToken;
import com.anish.e_commerce.model.User;
import com.anish.e_commerce.repo.UserRepo;
import com.anish.e_commerce.service.RefreshTokenService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.*;
import lombok.*;
import org.springframework.http.*;
import org.springframework.security.authentication.*;
import org.springframework.security.core.AuthenticationException;
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

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@Valid @RequestBody AuthRequest request) {
        if (repo.findByUsername(request.getUsername()).isPresent()) {
            return ResponseEntity.badRequest().body(
                Map.of("error", "Username already exists")
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
    public ResponseEntity<?> login(@Valid @RequestBody AuthRequest request) {
        try {
            authManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    request.getUsername(),
                    request.getPassword()
                )
            );

            User user = repo
                .findByUsername(request.getUsername())
                .orElseThrow();
            String token = jwtUtil.generateToken(
                user.getUsername(),
                user.getRoles()
            );
            RefreshToken refreshToken = refreshTokenService.createRefreshToken(
                user.getId()
            );

            Map<String, Object> res = new HashMap<>();
            res.put("token", token);
            res.put("refreshToken", refreshToken.getToken());
            res.put("roles", user.getRoles());
            return ResponseEntity.ok(res);
        } catch (AuthenticationException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                Map.of("error", "Invalid credentials")
            );
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshtoken(
        @Valid @RequestBody TokenRefreshRequest request
    ) {
        return refreshTokenService
            .findByToken(request.getRefreshToken())
            .map(refreshTokenService::verifyExpiration)
            .map(RefreshToken::getUserId)
            .map(userId -> {
                User user = repo
                    .findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));
                String token = jwtUtil.generateToken(
                    user.getUsername(),
                    user.getRoles()
                );
                Map<String, Object> res = new HashMap<>();
                res.put("token", token);
                res.put("refreshToken", request.getRefreshToken());
                res.put("roles", user.getRoles());
                return ResponseEntity.ok(res);
            })
            .orElse(
                ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                    Map.of(
                        "error",
                        "Refresh token is not in database or expired!"
                    )
                )
            );
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(
        @Valid @RequestBody TokenRefreshRequest request
    ) {
        refreshTokenService.deleteByToken(request.getRefreshToken());
        return ResponseEntity.ok(Map.of("message", "Log out successful"));
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

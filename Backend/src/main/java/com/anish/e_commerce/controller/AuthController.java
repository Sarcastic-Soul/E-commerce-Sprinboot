package com.anish.e_commerce.controller;

import com.anish.e_commerce.model.User;
import com.anish.e_commerce.repo.UserRepo;
import com.anish.e_commerce.jwt.JwtUtil;
import lombok.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.security.authentication.*;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepo repo;
    private final PasswordEncoder encoder;
    private final AuthenticationManager authManager;
    private final JwtUtil jwtUtil;

    @PostMapping("/signup")
    public ResponseEntity<String> signup(@RequestBody AuthRequest request) {
        if (repo.findByUsername(request.getUsername()).isPresent()) {
            return ResponseEntity.badRequest().body("Username already exists");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(encoder.encode(request.getPassword()));
        // 👉 First user becomes ADMIN, rest are USER
        if (repo.count() == 0) {
            user.setRoles(Set.of("ADMIN"));
        } else {
            user.setRoles(Set.of("USER"));
        }

        repo.save(user);
        return ResponseEntity.ok("User registered successfully");
    }


    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody AuthRequest request) {
        try {
            authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );

            User user = repo.findByUsername(request.getUsername()).orElseThrow();
            String token = jwtUtil.generateToken(user.getUsername(), user.getRoles());

            Map<String, Object> res = new HashMap<>();
            res.put("token", token);
            res.put("roles", user.getRoles());
            return ResponseEntity.ok(res);
        } catch (AuthenticationException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid credentials"));
        }
    }

    @Data
    public static class AuthRequest {
        private String username;
        private String password;
    }
}

package com.anish.e_commerce.config;

import com.anish.e_commerce.jwt.JwtAuthFilter;
import com.anish.e_commerce.service.UserDetailsServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.*;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.*;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final UserDetailsServiceImpl userDetailsService;
    private final RateLimitFilter rateLimitFilter;

    private static final ObjectMapper ERROR_MAPPER = new ObjectMapper();

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
        throws Exception {
        return http
            .cors(Customizer.withDefaults())
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth ->
                auth
                    // 0. Allow Swagger UI & OpenAPI Docs
                    .requestMatchers(
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html"
                    )
                    .permitAll()
                    // 1. Public API Endpoints
                    .requestMatchers("/api/auth/**")
                    .permitAll()
                    .requestMatchers("/api/products/**", "/api/product/**")
                    .permitAll()
                    // 2. Secured API Endpoints
                    .requestMatchers("/api/admin/**")
                    .hasRole("ADMIN")
                    .requestMatchers("/api/**")
                    .authenticated()
                    // 3. THE CATCH-ALL
                    .requestMatchers("/**")
                    .permitAll()
            )
            // Without this, Spring falls back to Http403ForbiddenEntryPoint and answers
            // 403 even when the caller simply has no (or an expired) token. Clients
            // cannot then tell "refresh your token" from "you may never do this", so
            // be explicit: 401 = not authenticated, 403 = authenticated but not allowed.
            .exceptionHandling(ex ->
                ex
                    .authenticationEntryPoint((request, response, authEx) ->
                        writeError(
                            request,
                            response,
                            HttpStatus.UNAUTHORIZED,
                            "Authentication required. Your session may have expired."
                        )
                    )
                    .accessDeniedHandler((request, response, deniedEx) ->
                        writeError(
                            request,
                            response,
                            HttpStatus.FORBIDDEN,
                            "You do not have permission to access this resource."
                        )
                    )
            )
            .sessionManagement(sess ->
                sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .userDetailsService(userDetailsService)
            .addFilterBefore(
                rateLimitFilter,
                UsernamePasswordAuthenticationFilter.class
            )
            .addFilterBefore(
                jwtAuthFilter,
                UsernamePasswordAuthenticationFilter.class
            )
            .build();
    }

    /** Emits the same JSON shape as ApiErrorResponse / RateLimitFilter. */
    private static void writeError(
        HttpServletRequest request,
        HttpServletResponse response,
        HttpStatus status,
        String message
    ) throws IOException {
        response.setStatus(status.value());
        response.setContentType("application/json");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        body.put("path", request.getRequestURI());

        response.getWriter().write(ERROR_MAPPER.writeValueAsString(body));
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authManager(AuthenticationConfiguration config)
        throws Exception {
        return config.getAuthenticationManager();
    }
}

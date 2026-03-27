package com.anish.e_commerce.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final Map<String, Bucket> cache = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private Bucket createNewBucket() {
        Bandwidth limit = Bandwidth.classic(
            30,
            Refill.greedy(30, Duration.ofMinutes(1))
        );
        return Bucket.builder().addLimit(limit).build();
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        String path = request.getRequestURI();

        if (
            path.startsWith("/api/auth/") ||
            path.startsWith("/api/products/") ||
            path.startsWith("/api/product/")
        ) {
            String ip = request.getHeader("X-Forwarded-For");
            if (ip == null || ip.isEmpty()) {
                ip = request.getRemoteAddr();
            }

            Bucket bucket = cache.computeIfAbsent(ip, k -> createNewBucket());

            if (!bucket.tryConsume(1)) {
                // 1. Set Status and Content Type
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType("application/json");

                // 2. Add the Retry-After Header
                response.setHeader("Retry-After", "60");

                // 3. Build a standardized JSON response matching our ApiErrorResponse
                Map<String, Object> errorDetails = new HashMap<>();
                errorDetails.put("timestamp", LocalDateTime.now().toString());
                errorDetails.put(
                    "status",
                    HttpStatus.TOO_MANY_REQUESTS.value()
                );
                errorDetails.put("error", "Too Many Requests");
                errorDetails.put(
                    "message",
                    "Rate limit exceeded. Please try again in 60 seconds."
                );
                errorDetails.put("path", path);

                // Write the JSON to the response
                response
                    .getWriter()
                    .write(objectMapper.writeValueAsString(errorDetails));
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}

package com.anish.e_commerce.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    // Simple in-memory cache to store a Bucket for each IP address.
    private final Map<String, Bucket> cache = new ConcurrentHashMap<>();

    // Define the rate limit rules here
    private Bucket createNewBucket() {
        // Example: Allow 30 requests per minute per IP
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

        // 1. Only apply rate limits to our specific Public APIs
        if (
            path.startsWith("/api/auth/") ||
            path.startsWith("/api/products/") ||
            path.startsWith("/api/product/")
        ) {
            // Get the client's IP address
            String ip = request.getHeader("X-Forwarded-For");
            if (ip == null || ip.isEmpty()) {
                ip = request.getRemoteAddr();
            }

            // Fetch the bucket for this IP, or create a new one if it doesn't exist
            Bucket bucket = cache.computeIfAbsent(ip, k -> createNewBucket());

            // 2. Try to consume 1 token for this request
            if (!bucket.tryConsume(1)) {
                // 3. If out of tokens, reject the request
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType("application/json");
                response
                    .getWriter()
                    .write(
                        """
                        {
                          "error": "Too many requests",
                          "status": 429
                        }
                        """
                    );
                return;
            }
        }

        // 4. If they have tokens (or it's not a public API), continue normally
        filterChain.doFilter(request, response);
    }
}

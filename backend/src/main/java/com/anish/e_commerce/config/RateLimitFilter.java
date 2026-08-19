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
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    /**
     * Upper bound on tracked client IPs. Without a cap the map grows for every
     * distinct (and trivially forgeable) address we ever see, which is a slow
     * memory leak and a cheap way to exhaust the heap.
     */
    private static final int MAX_TRACKED_CLIENTS = 10_000;

    /** Access-ordered LRU: the least recently seen client is dropped once full. */
    private final Map<String, Bucket> cache = Collections.synchronizedMap(
        new LinkedHashMap<>(256, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, Bucket> eldest) {
                return size() > MAX_TRACKED_CLIENTS;
            }
        }
    );

    private final ObjectMapper objectMapper = new ObjectMapper();

    private Bucket createNewBucket() {
        Bandwidth limit = Bandwidth.classic(
            30,
            Refill.greedy(30, Duration.ofMinutes(1))
        );
        return Bucket.builder().addLimit(limit).build();
    }

    /**
     * X-Forwarded-For is appended to by each proxy, so the value our own reverse
     * proxy added is the *last* entry. Anything to the left of it was supplied by
     * the client and can be forged, which would let a caller mint a fresh bucket
     * per request and bypass the limit entirely.
     */
    private String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            String[] hops = forwardedFor.split(",");
            for (int i = hops.length - 1; i >= 0; i--) {
                String hop = hops[i].trim();
                if (!hop.isEmpty()) {
                    return hop;
                }
            }
        }
        return request.getRemoteAddr();
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
            String ip = resolveClientIp(request);

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

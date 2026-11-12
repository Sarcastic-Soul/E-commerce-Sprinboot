package com.anish.e_commerce.config;

import tools.jackson.databind.ObjectMapper;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.jdbc.PrimaryKeyMapper;
import io.github.bucket4j.postgresql.Bucket4jPostgreSQL;
import io.github.bucket4j.postgresql.PostgreSQLSelectForUpdateBasedProxyManager;
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
import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Slf4j
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

    /**
     * Auth and catalog need opposite limits. 10/min is a brute-force budget; catalog
     * browsing blows past 30/min on its own — the search box fires one request per
     * 500ms debounce pause, plus one per filter change and per page turn.
     */
    private static final int AUTH_LIMIT = 10;
    private static final int CATALOG_LIMIT = 120;

    /**
     * Where the login/signup buckets live: "memory" (this JVM) or "postgres".
     * Memory is fine for one long-running server. On serverless, instances come and
     * go and several can run at once, each with its own counters, so a brute-force
     * run could spread across them; "postgres" shares one count. Catalog buckets stay
     * in memory everywhere: that limit only stops spam, so being per instance is
     * fine, and it avoids a database write on every product request.
     */
    private final String authStore;

    /**
     * Header holding the real client IP, set by the platform's edge (Vercel sets
     * x-real-ip and overwrites any client-supplied value). Blank means fall back to
     * the last X-Forwarded-For hop.
     */
    private final String clientIpHeader;

    // Lazy: @WebMvcTest slices build this filter without a DataSource.
    private final ObjectProvider<DataSource> dataSource;
    private volatile PostgreSQLSelectForUpdateBasedProxyManager<String> sharedBuckets;

    static final String BUCKET_TABLE = "rate_limit_bucket";

    public RateLimitFilter(
        @Value("${app.rate-limit.auth-store:memory}") String authStore,
        @Value("${app.client-ip-header:}") String clientIpHeader,
        ObjectProvider<DataSource> dataSource
    ) {
        this.authStore = authStore;
        this.clientIpHeader = clientIpHeader;
        this.dataSource = dataSource;
    }

    private static Bandwidth limitFor(int capacity) {
        return Bandwidth.builder()
            .capacity(capacity)
            .refillGreedy(capacity, Duration.ofMinutes(1))
            .build();
    }

    /**
     * Built on first use so neither the table check nor the pool is touched at boot.
     * The table is not a JPA entity (Bucket4j owns its format), so it is created here.
     */
    private PostgreSQLSelectForUpdateBasedProxyManager<String> sharedBuckets() {
        PostgreSQLSelectForUpdateBasedProxyManager<String> manager = sharedBuckets;
        if (manager == null) {
            synchronized (this) {
                if (sharedBuckets == null) {
                    DataSource ds = dataSource.getObject();
                    new JdbcTemplate(ds).execute(
                        "CREATE TABLE IF NOT EXISTS " + BUCKET_TABLE +
                            " (id VARCHAR(128) PRIMARY KEY, state BYTEA, expires_at BIGINT)"
                    );
                    sharedBuckets = Bucket4jPostgreSQL.selectForUpdateBasedBuilder(ds)
                        .primaryKeyMapper(PrimaryKeyMapper.STRING)
                        .table(BUCKET_TABLE)
                        // Rows count as expired once the bucket would be full again.
                        .expirationAfterWrite(
                            ExpirationAfterWriteStrategy.basedOnTimeForRefillingBucketUpToMax(
                                Duration.ofMinutes(1)
                            )
                        )
                        .build();
                }
                manager = sharedBuckets;
            }
        }
        return manager;
    }

    /** Returns true when the request may go ahead. */
    private boolean tryConsume(String scope, String key, int capacity) {
        if ("auth".equals(scope) && "postgres".equalsIgnoreCase(authStore)) {
            try {
                return sharedBuckets()
                    .getProxy(key, () -> BucketConfiguration.builder().addLimit(limitFor(capacity)).build())
                    .tryConsume(1);
            } catch (RuntimeException e) {
                // A rate limiter must not take login down with it: if the database
                // hiccups, let the request through (login itself will then fail or
                // succeed on its own) and say so in the logs.
                log.warn("Shared rate limit check skipped: {}", e.getMessage());
                return true;
            }
        }
        return cache
            .computeIfAbsent(key, k -> Bucket.builder().addLimit(limitFor(capacity)).build())
            .tryConsume(1);
    }

    /**
     * Deletes expired bucket rows. Called hourly on a long-running server and by the
     * daily cron endpoint on serverless; returns how many rows were removed.
     */
    @Scheduled(fixedDelayString = "PT1H", initialDelayString = "PT10M")
    public int removeExpiredBuckets() {
        if (!"postgres".equalsIgnoreCase(authStore)) return 0;
        int total = 0;
        int removed;
        do {
            removed = sharedBuckets().removeExpired(500);
            total += removed;
        } while (removed == 500);
        return total;
    }

    /**
     * X-Forwarded-For is appended to by each proxy, so the value our own reverse
     * proxy added is the *last* entry. Anything to the left of it was supplied by
     * the client and can be forged, which would let a caller mint a fresh bucket
     * per request and bypass the limit entirely.
     */
    private String resolveClientIp(HttpServletRequest request) {
        if (!clientIpHeader.isBlank()) {
            String fromEdge = request.getHeader(clientIpHeader);
            if (fromEdge != null && !fromEdge.isBlank()) {
                return fromEdge.trim();
            }
        }
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

        Integer capacity = null;
        String scope = null;
        // Only the password endpoints get the brute-force budget. /me and /csrf run
        // on every page load, so counting them logged people out after a few reloads.
        if (path.equals("/api/auth/login") || path.equals("/api/auth/signup")) {
            capacity = AUTH_LIMIT;
            scope = "auth";
        } else if (
            path.startsWith("/api/products/") ||
            path.startsWith("/api/product/")
        ) {
            capacity = CATALOG_LIMIT;
            scope = "catalog";
        }

        if (capacity != null) {
            // Scope is part of the key so the two budgets stay separate per client.
            String key = scope + ":" + resolveClientIp(request);
            if (!tryConsume(scope, key, capacity)) {
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

package com.anish.e_commerce.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Order.status used to be a free-form string and paid orders were "COMPLETED".
 * It is an enum now, and reading a row with a value the enum does not have throws,
 * so rewrite the legacy value before anything loads orders. Idempotent: after the
 * first run there is nothing left to update.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
@Slf4j
public class LegacyOrderStatusMigration implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        try {
            int updated = jdbcTemplate.update(
                "UPDATE orders SET status = 'PAID', paid_at = COALESCE(paid_at, created_at) " +
                    "WHERE status = 'COMPLETED'"
            );
            if (updated > 0) {
                log.info("Migrated {} legacy COMPLETED order(s) to PAID", updated);
            }
        } catch (Exception e) {
            log.warn("Legacy order status migration skipped: {}", e.getMessage());
        }
    }
}

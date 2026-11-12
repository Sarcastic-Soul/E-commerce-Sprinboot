package com.anish.e_commerce.service;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Releases the stock held by checkouts that were never paid for. A separate bean
 * so each expireOrder call goes through the transactional proxy.
 *
 * Three triggers share the same sweep:
 * - the fixed-delay schedule below, on a long-running server (Render, local);
 * - {@link #sweepIfDue()} before each checkout, which is what keeps stock correct
 *   on serverless, where no process stays up to run a timer;
 * - the daily cleanup endpoint in InternalController, a safety net there.
 * The schedule only runs while SchedulingConfig is active (app.scheduling.enabled).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderExpiryJob {

    /** How often a checkout may trigger a sweep on one instance. */
    private static final long ON_DEMAND_INTERVAL_MS = Duration.ofMinutes(5).toMillis();

    private final OrderService orderService;
    private final AtomicLong lastOnDemandSweep = new AtomicLong();

    @Scheduled(fixedDelayString = "PT5M", initialDelayString = "PT1M")
    public void expireStalePendingOrders() {
        sweep();
    }

    /**
     * Runs a sweep unless this instance already ran one in the last few minutes.
     * Never throws: a failed sweep must not block the checkout that triggered it.
     */
    public void sweepIfDue() {
        long now = System.currentTimeMillis();
        long last = lastOnDemandSweep.get();
        if (now - last < ON_DEMAND_INTERVAL_MS || !lastOnDemandSweep.compareAndSet(last, now)) {
            return;
        }
        try {
            sweep();
        } catch (Exception e) {
            log.warn("On-demand order expiry sweep failed: {}", e.getMessage());
        }
    }

    /** Returns how many orders were expired. */
    public int sweep() {
        List<Long> ids = orderService.findStalePendingOrderIds();
        int expired = 0;
        for (Long id : ids) {
            try {
                if (orderService.expireOrder(id)) expired++;
            } catch (Exception e) {
                // Most likely a payment callback won the race (optimistic lock).
                log.warn("Could not expire order {}: {}", id, e.getMessage());
            }
        }
        if (expired > 0) {
            log.info("Expired {} unpaid order(s) and released their stock", expired);
        }
        return expired;
    }
}

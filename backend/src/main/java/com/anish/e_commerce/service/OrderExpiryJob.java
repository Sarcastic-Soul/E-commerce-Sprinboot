package com.anish.e_commerce.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Releases the stock held by checkouts that were never paid for. A separate bean
 * so each expireOrder call goes through the transactional proxy.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderExpiryJob {

    private final OrderService orderService;

    @Scheduled(fixedDelayString = "PT5M", initialDelayString = "PT1M")
    public void expireStalePendingOrders() {
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
    }
}

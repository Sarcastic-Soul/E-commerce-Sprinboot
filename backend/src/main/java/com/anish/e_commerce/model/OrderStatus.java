package com.anish.e_commerce.model;

import java.util.Set;

/**
 * PENDING -> PAID -> SHIPPED -> DELIVERED, with CANCELLED / REJECTED / EXPIRED as
 * side exits. Stored as STRING; the legacy value COMPLETED is rewritten to PAID at
 * startup by LegacyOrderStatusMigration.
 */
public enum OrderStatus {
    PENDING,
    PAID,
    SHIPPED,
    DELIVERED,
    CANCELLED,
    REJECTED,
    EXPIRED;

    /** Statuses where money was actually taken and not given back. */
    public static final Set<OrderStatus> PURCHASED = Set.of(PAID, SHIPPED, DELIVERED);

    public boolean isCancellableByUser() {
        return this == PENDING || this == PAID;
    }
}

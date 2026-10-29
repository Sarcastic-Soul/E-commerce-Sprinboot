package com.anish.e_commerce.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.*;
import org.hibernate.annotations.BatchSize;

@Entity
@Table(
    name = "orders",
    indexes = {
        // Postgres does not index FK columns automatically; both of these were
        // sequential scans, and razorpay_order_id is hit on every payment callback.
        @Index(name = "idx_orders_user_id", columnList = "user_id"),
        @Index(
            name = "idx_orders_razorpay_order_id",
            columnList = "razorpay_order_id"
        ),
        // Admin filter and the PENDING-expiry sweep both look orders up by status.
        @Index(name = "idx_orders_status", columnList = "status"),
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Optimistic lock: the expiry sweep, a payment callback and a cancel can race on
    // the same PENDING order, and each of them moves stock. Only one may win.
    // The default lets `ddl-auto=update` add the column to a table that has rows.
    @Version
    @Column(columnDefinition = "bigint default 0 not null")
    private long version;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // What the user pays: subtotal - discount. Legacy rows only have this one.
    private BigDecimal totalAmount;

    // Nullable: orders placed before coupons existed have neither.
    private BigDecimal subtotal;
    private BigDecimal discount;
    private String couponCode;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    private LocalDateTime createdAt;
    private LocalDateTime paidAt;
    private LocalDateTime shippedAt;
    private LocalDateTime deliveredAt;
    private LocalDateTime cancelledAt;

    private String razorpayOrderId;

    // Kept so a cancel of a PAID order can ask Razorpay for a refund.
    private String razorpayPaymentId;

    @Embedded
    private ShippingAddress shippingAddress;

    // Lazy: EAGER fired an extra query per order on every read. Batch-fetched so a
    // page of orders loads its items in one extra query, not one per order.
    @OneToMany(
        mappedBy = "order",
        cascade = CascadeType.ALL,
        orphanRemoval = true
    )
    @BatchSize(size = 50)
    private List<OrderItem> items = new ArrayList<>();
}

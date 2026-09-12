package com.anish.e_commerce.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.*;

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

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private BigDecimal totalAmount;

    private String status;

    private LocalDateTime createdAt;

    private String razorpayOrderId;

    // Lazy: EAGER fired an extra query per order on every read. The one path that
    // serializes this outside a transaction uses an @EntityGraph in OrderRepo.
    @OneToMany(
        mappedBy = "order",
        cascade = CascadeType.ALL,
        orphanRemoval = true
    )
    private List<OrderItem> items = new ArrayList<>();
}

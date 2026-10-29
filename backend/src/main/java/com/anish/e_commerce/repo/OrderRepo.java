package com.anish.e_commerce.repo;

import com.anish.e_commerce.model.Order;
import com.anish.e_commerce.model.OrderStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderRepo extends JpaRepository<Order, Long> {
    /**
     * `items` is lazy, and open-in-view is off, so the collection has to be fetched
     * inside the transaction here — this result is mapped to DTOs right after.
     */
    @EntityGraph(attributePaths = { "items", "items.product" })
    List<Order> findByUser_UsernameOrderByCreatedAtDesc(String username);

    Optional<Order> findByRazorpayOrderId(String razorpayOrderId);

    Page<Order> findByStatus(OrderStatus status, Pageable pageable);

    List<Order> findByStatusAndCreatedAtBefore(
        OrderStatus status,
        LocalDateTime cutoff
    );

    @EntityGraph(attributePaths = { "items", "items.product", "user" })
    List<Order> findAllByOrderByCreatedAtDesc();

    long countByUser_Username(String username);

    @Query(
        "SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.status IN :statuses"
    )
    BigDecimal sumRevenue(@Param("statuses") Collection<OrderStatus> statuses);

    /**
     * Aggregate in the database instead of loading every order. Grouping by
     * year/month/day (rather than a formatted string) keeps the ORDER BY chronological
     * and stops dates from different years collapsing into one bucket. The count is
     * every order placed; revenue only counts orders that were actually paid for.
     */
    @Query(
        "SELECT YEAR(o.createdAt), MONTH(o.createdAt), DAY(o.createdAt), " +
            "COUNT(o), COALESCE(SUM(CASE WHEN o.status IN :statuses THEN o.totalAmount ELSE 0 END), 0) " +
            "FROM Order o " +
            "GROUP BY YEAR(o.createdAt), MONTH(o.createdAt), DAY(o.createdAt) " +
            "ORDER BY YEAR(o.createdAt), MONTH(o.createdAt), DAY(o.createdAt)"
    )
    List<Object[]> findDailyOrderStats(
        @Param("statuses") Collection<OrderStatus> statuses
    );

    /** Review eligibility: did this user buy (and keep paying for) this product? */
    @Query(
        "SELECT CASE WHEN COUNT(oi) > 0 THEN true ELSE false END FROM OrderItem oi " +
            "WHERE oi.order.user.username = :username " +
            "AND oi.product.id = :productId " +
            "AND oi.order.status IN :statuses"
    )
    boolean hasPurchased(
        @Param("username") String username,
        @Param("productId") int productId,
        @Param("statuses") Collection<OrderStatus> statuses
    );
}

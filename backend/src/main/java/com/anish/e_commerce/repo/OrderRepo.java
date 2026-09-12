package com.anish.e_commerce.repo;

import com.anish.e_commerce.model.Order;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderRepo extends JpaRepository<Order, Long> {
    /**
     * `items` is lazy, and open-in-view is off, so the collection has to be fetched
     * inside the transaction here — this result is serialized straight to JSON.
     */
    @EntityGraph(attributePaths = { "items", "items.product" })
    List<Order> findByUser_UsernameOrderByCreatedAtDesc(String username);

    java.util.Optional<Order> findByRazorpayOrderId(String razorpayOrderId);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o")
    BigDecimal sumTotalRevenue();

    /**
     * Aggregate in the database instead of loading every order. Grouping by
     * year/month/day (rather than a formatted string) keeps the ORDER BY chronological
     * and stops dates from different years collapsing into one bucket.
     */
    @Query(
        "SELECT YEAR(o.createdAt), MONTH(o.createdAt), DAY(o.createdAt), " +
            "COUNT(o), COALESCE(SUM(o.totalAmount), 0) " +
            "FROM Order o " +
            "GROUP BY YEAR(o.createdAt), MONTH(o.createdAt), DAY(o.createdAt) " +
            "ORDER BY YEAR(o.createdAt), MONTH(o.createdAt), DAY(o.createdAt)"
    )
    List<Object[]> findDailyOrderStats();
}

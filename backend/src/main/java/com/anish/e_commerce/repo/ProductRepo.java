package com.anish.e_commerce.repo;

import com.anish.e_commerce.model.Product;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepo extends JpaRepository<Product, Integer> {
    Page<Product> findByActiveTrue(Pageable pageable);

    Optional<Product> findFirstByName(String name);

    @Query(
        "SELECT p FROM Product p WHERE p.active = true " +
            "AND (:keyword IS NULL OR :keyword = '' OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(p.brand) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "AND (:category IS NULL OR p.category = :category) " +
            "AND (:minPrice IS NULL OR p.price >= :minPrice) " +
            "AND (:maxPrice IS NULL OR p.price <= :maxPrice)" +
            "AND (:available IS NULL OR p.available = :available) "
    )
    List<Product> filterProducts(
        @Param("keyword") String keyword,
        @Param("category") Product.ProductCategory category,
        @Param("minPrice") java.math.BigDecimal minPrice,
        @Param("maxPrice") java.math.BigDecimal maxPrice,
        @Param("available") Boolean available,
        Pageable pageable
    );

    @Query(
        "SELECT p FROM Product p WHERE p.active = true AND (" +
            "LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(p.brand) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(p.category) LIKE LOWER(CONCAT('%', :keyword, '%')))"
    )
    List<Product> searchProducts(
        @Param("keyword") String keyword,
        Pageable pageable
    );

    long countByActiveTrue();

    @Query(
        "SELECT COUNT(p) FROM Product p WHERE p.active = true AND (p.available = false OR p.quantity = 0)"
    )
    long countOutOfStock();

    @Query(
        "SELECT p FROM Product p WHERE p.active = true AND p.quantity <= :threshold " +
            "ORDER BY p.quantity ASC, p.name ASC"
    )
    List<Product> findLowStock(@Param("threshold") int threshold);

    List<Product> findAllByOrderByIdAsc();

    /**
     * Atomic reserve: the WHERE re-checks stock in the same statement that takes it,
     * so two checkouts racing for the last unit cannot both succeed (a read-then-write
     * in Java could). Returns 0 when the stock is not there.
     */
    @Modifying
    @Query(
        "UPDATE Product p SET p.quantity = p.quantity - :qty " +
            "WHERE p.id = :id AND p.quantity >= :qty AND p.active = true AND p.available = true"
    )
    int reserveStock(@Param("id") int id, @Param("qty") int qty);

    @Modifying
    @Query(
        "UPDATE Product p SET p.available = false WHERE p.id = :id AND p.quantity <= 0"
    )
    int markSoldOutIfEmpty(@Param("id") int id);

    @Modifying
    @Query(
        "UPDATE Product p SET p.quantity = p.quantity + :qty, p.available = true WHERE p.id = :id"
    )
    int restoreStock(@Param("id") int id, @Param("qty") int qty);

    @Modifying
    @Query(
        "UPDATE Product p SET p.averageRating = :avg, p.reviewCount = :count WHERE p.id = :id"
    )
    int updateRating(
        @Param("id") int id,
        @Param("avg") double avg,
        @Param("count") int count
    );
}

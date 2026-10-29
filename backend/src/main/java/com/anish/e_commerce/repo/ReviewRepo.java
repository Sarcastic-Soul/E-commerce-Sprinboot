package com.anish.e_commerce.repo;

import com.anish.e_commerce.model.Review;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReviewRepo extends JpaRepository<Review, Long> {
    @EntityGraph(attributePaths = { "user" })
    Page<Review> findByProduct_IdOrderByCreatedAtDescIdDesc(
        int productId,
        Pageable pageable
    );

    Optional<Review> findByUser_UsernameAndProduct_Id(
        String username,
        int productId
    );

    long countByUser_Username(String username);

    /**
     * One row of [average, count]; average is 0 when there are none. A List so
     * Spring Data does not wrap the row array in another array.
     */
    @Query(
        "SELECT COALESCE(AVG(r.rating), 0), COUNT(r) FROM Review r " +
            "WHERE r.product.id = :productId"
    )
    List<Object[]> ratingSummary(@Param("productId") int productId);
}

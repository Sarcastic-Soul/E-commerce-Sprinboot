package com.anish.e_commerce.repo;

import com.anish.e_commerce.model.Coupon;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CouponRepo extends JpaRepository<Coupon, Long> {
    Optional<Coupon> findByCode(String code);

    boolean existsByCode(String code);

    List<Coupon> findAllByOrderByIdDesc();

    /**
     * Conditional so two orders paying at once cannot both take the last use:
     * the row is only bumped while it is still under maxUses. Returns 0 if not.
     */
    @Modifying
    @Query(
        "UPDATE Coupon c SET c.usedCount = c.usedCount + 1 " +
            "WHERE c.code = :code AND (c.maxUses IS NULL OR c.usedCount < c.maxUses)"
    )
    int incrementUsage(@Param("code") String code);
}

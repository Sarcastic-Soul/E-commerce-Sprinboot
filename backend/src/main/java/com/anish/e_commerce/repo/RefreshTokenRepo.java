package com.anish.e_commerce.repo;

import com.anish.e_commerce.model.RefreshToken;
import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface RefreshTokenRepo extends JpaRepository<RefreshToken, String> {

    @Transactional
    @Modifying
    @Query("delete from RefreshToken t where t.userId = :userId")
    int deleteByUserId(Long userId);

    @Transactional
    @Modifying
    @Query("delete from RefreshToken t where t.expiresAt < :now")
    int deleteExpired(Instant now);
}

package com.anish.e_commerce.repo;

import com.anish.e_commerce.model.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CartRepo extends JpaRepository<Cart, Long> {
    @Query("SELECT c FROM Cart c LEFT JOIN FETCH c.items WHERE c.user.username = :username")
    Optional<Cart> findByUser_Username(@Param("username") String username);
}

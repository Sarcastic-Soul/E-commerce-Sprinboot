package com.anish.e_commerce.repo;

import com.anish.e_commerce.dto.CartItemResponse;
import com.anish.e_commerce.model.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CartItemRepo extends JpaRepository<CartItem, Long> {
    // In CartItemRepo
    @Query("SELECT new com.anish.e_commerce.dto.CartItemResponse(p.id, p.name,p.price, p.imageUrl, ci.quantity) " +
            "FROM CartItem ci JOIN ci.product p WHERE ci.cart.user.username = :username")
    List<CartItemResponse> findItemsByUsername(@Param("username") String username);
}
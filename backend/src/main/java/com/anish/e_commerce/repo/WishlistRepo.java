package com.anish.e_commerce.repo;

import com.anish.e_commerce.model.Wishlist;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WishlistRepo extends JpaRepository<Wishlist, Long> {
    List<Wishlist> findByUserId(Long userId);
    List<Wishlist> findByProductId(int productId);
    boolean existsByUserIdAndProductId(Long userId, int productId);

    void deleteByUserIdAndProductId(Long userId, int productId);
}

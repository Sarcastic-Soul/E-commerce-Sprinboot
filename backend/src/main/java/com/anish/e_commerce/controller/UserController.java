package com.anish.e_commerce.controller;

import com.anish.e_commerce.model.Notification;
import com.anish.e_commerce.model.Product;
import com.anish.e_commerce.model.User;
import com.anish.e_commerce.model.Wishlist;
import com.anish.e_commerce.repo.ProductRepo;
import com.anish.e_commerce.repo.UserRepo;
import com.anish.e_commerce.repo.WishlistRepo;
import com.anish.e_commerce.service.NotificationService;
import jakarta.transaction.Transactional;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final WishlistRepo wishlistRepo;
    private final UserRepo userRepo;
    private final ProductRepo productRepo;
    private final NotificationService notificationService;

    // Helper to get logged-in user
    private User getCurrentUser(Authentication auth) {
        return userRepo.findByUsername(auth.getName()).orElseThrow();
    }

    @Transactional
    @PostMapping("/wishlist/{productId}")
    public ResponseEntity<String> toggleWishlist(
        @PathVariable int productId,
        Authentication auth
    ) {
        User user = getCurrentUser(auth);

        if (wishlistRepo.existsByUserIdAndProductId(user.getId(), productId)) {
            // If it exists, remove it (toggle off)
            wishlistRepo.deleteByUserIdAndProductId(user.getId(), productId);
            return ResponseEntity.ok("Removed from wishlist");
        } else {
            // If it doesn't exist, add it (toggle on)
            Product product = productRepo.findById(productId).orElseThrow();
            Wishlist wishlist = new Wishlist();
            wishlist.setUser(user);
            wishlist.setProduct(product);
            wishlistRepo.save(wishlist);
            return ResponseEntity.ok("Added to wishlist");
        }
    }

    @GetMapping("/wishlist")
    public ResponseEntity<List<Wishlist>> getWishlist(Authentication auth) {
        return ResponseEntity.ok(
            wishlistRepo.findByUserId(getCurrentUser(auth).getId())
        );
    }

    @GetMapping("/notifications")
    public ResponseEntity<List<Notification>> getNotifications(
        Authentication auth
    ) {
        return ResponseEntity.ok(
            notificationService.getUserNotifications(
                getCurrentUser(auth).getId()
            )
        );
    }

    @PutMapping("/notifications/{id}/read")
    public ResponseEntity<Void> markNotificationRead(@PathVariable Long id) {
        notificationService.markAsRead(id);
        return ResponseEntity.ok().build();
    }
}

package com.anish.e_commerce.controller;

import com.anish.e_commerce.dto.AddressRequest;
import com.anish.e_commerce.dto.AddressResponse;
import com.anish.e_commerce.dto.PasswordChangeRequest;
import com.anish.e_commerce.dto.ProfileResponse;
import com.anish.e_commerce.exception.ResourceNotFoundException;
import com.anish.e_commerce.model.Notification;
import com.anish.e_commerce.model.Product;
import com.anish.e_commerce.model.User;
import com.anish.e_commerce.model.Wishlist;
import com.anish.e_commerce.repo.ProductRepo;
import com.anish.e_commerce.repo.UserRepo;
import com.anish.e_commerce.repo.WishlistRepo;
import com.anish.e_commerce.service.AccountService;
import com.anish.e_commerce.service.AddressService;
import com.anish.e_commerce.service.NotificationService;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map; // Added Map import
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
    private final AddressService addressService;
    private final AccountService accountService;

    // Helper to get logged-in user
    private User getCurrentUser(Authentication auth) {
        return userRepo
            .findByUsername(auth.getName())
            .orElseThrow(() ->
                new ResourceNotFoundException("User not found: " + auth.getName())
            );
    }

    @Transactional
    @PostMapping("/wishlist/{productId}")
    public ResponseEntity<?> toggleWishlist(
        @PathVariable int productId,
        Authentication auth
    ) {
        User user = getCurrentUser(auth);

        if (wishlistRepo.existsByUserIdAndProductId(user.getId(), productId)) {
            wishlistRepo.deleteByUserIdAndProductId(user.getId(), productId);
            return ResponseEntity.ok(
                Map.of("message", "Removed from wishlist")
            );
        } else {
            Product product = productRepo
                .findById(productId)
                .orElseThrow(() ->
                    new ResourceNotFoundException("Product not found with id: " + productId)
                );
            Wishlist wishlist = new Wishlist();
            wishlist.setUser(user);
            wishlist.setProduct(product);
            wishlistRepo.save(wishlist);
            return ResponseEntity.ok(Map.of("message", "Added to wishlist"));
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
    public ResponseEntity<Void> markNotificationRead(
        @PathVariable Long id,
        Authentication auth
    ) {
        notificationService.markAsRead(id, getCurrentUser(auth).getId());
        return ResponseEntity.ok().build();
    }

    // ---------- profile ----------

    @GetMapping("/profile")
    public ResponseEntity<ProfileResponse> getProfile(Authentication auth) {
        return ResponseEntity.ok(accountService.profile(auth.getName()));
    }

    @PutMapping("/password")
    public ResponseEntity<Void> changePassword(
        @Valid @RequestBody PasswordChangeRequest request,
        Authentication auth
    ) {
        accountService.changePassword(auth.getName(), request);
        return ResponseEntity.noContent().build();
    }

    // ---------- address book ----------

    @GetMapping("/addresses")
    public ResponseEntity<List<AddressResponse>> getAddresses(Authentication auth) {
        return ResponseEntity.ok(addressService.list(auth.getName()));
    }

    @PostMapping("/addresses")
    public ResponseEntity<AddressResponse> addAddress(
        @Valid @RequestBody AddressRequest request,
        Authentication auth
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
            addressService.create(auth.getName(), request)
        );
    }

    @PutMapping("/addresses/{id}")
    public ResponseEntity<AddressResponse> updateAddress(
        @PathVariable Long id,
        @Valid @RequestBody AddressRequest request,
        Authentication auth
    ) {
        return ResponseEntity.ok(addressService.update(id, auth.getName(), request));
    }

    @DeleteMapping("/addresses/{id}")
    public ResponseEntity<Void> deleteAddress(
        @PathVariable Long id,
        Authentication auth
    ) {
        addressService.delete(id, auth.getName());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/addresses/{id}/default")
    public ResponseEntity<AddressResponse> setDefaultAddress(
        @PathVariable Long id,
        Authentication auth
    ) {
        return ResponseEntity.ok(addressService.setDefault(id, auth.getName()));
    }
}

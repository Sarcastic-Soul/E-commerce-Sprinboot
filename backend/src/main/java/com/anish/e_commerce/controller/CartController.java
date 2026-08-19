package com.anish.e_commerce.controller;

import com.anish.e_commerce.dto.CartItemResponse;
import com.anish.e_commerce.service.CartService;
import jakarta.validation.constraints.Min;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
@Validated
// A cart belongs to exactly one user: the {username} in the path must be the
// caller's own, otherwise anyone logged in could read or edit someone else's cart.
@PreAuthorize("#username == authentication.name")
public class CartController {

    private final CartService cartService;

    @GetMapping("/{username}")
    public ResponseEntity<List<CartItemResponse>> getCartItems(
        @PathVariable String username
    ) {
        return ResponseEntity.ok(cartService.getCartItemsByUsername(username));
    }

    @PostMapping("/{username}/add")
    public ResponseEntity<List<CartItemResponse>> addOrUpdateItem(
        @PathVariable String username,
        @RequestParam int productId,
        @RequestParam @Min(
            value = 1,
            message = "Quantity must be at least 1"
        ) int quantity
    ) {
        return ResponseEntity.ok(
            cartService.addOrUpdateItem(username, productId, quantity)
        );
    }

    @DeleteMapping("/{username}/remove")
    public ResponseEntity<List<CartItemResponse>> removeItem(
        @PathVariable String username,
        @RequestParam int productId
    ) {
        return ResponseEntity.ok(cartService.removeItem(username, productId));
    }

    @DeleteMapping("/{username}/clear")
    public ResponseEntity<?> clearCart(@PathVariable String username) {
        cartService.clearCart(username);
        return ResponseEntity.ok(Map.of("message", "Cart cleared"));
    }
}

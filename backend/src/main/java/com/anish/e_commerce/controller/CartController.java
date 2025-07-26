package com.anish.e_commerce.controller;

import com.anish.e_commerce.dto.CartItemResponse;
import com.anish.e_commerce.model.Cart;
import com.anish.e_commerce.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping("/{username}")
    public ResponseEntity<List<CartItemResponse>> getCartItems(@PathVariable String username) {
        return ResponseEntity.ok(cartService.getCartItemsByUsername(username));
    }

    @PostMapping("/{username}/add")
    public ResponseEntity<List<CartItemResponse>> addOrUpdateItem(@PathVariable String username,
                                                @RequestParam int productId,
                                                @RequestParam int quantity) {
        return ResponseEntity.ok(cartService.addOrUpdateItem(username, productId, quantity));
    }

    @DeleteMapping("/{username}/remove")
    public ResponseEntity<List<CartItemResponse>> removeItem(@PathVariable String username,
                                           @RequestParam int productId) {
        return ResponseEntity.ok(cartService.removeItem(username, productId));
    }

    @DeleteMapping("/{username}/clear")
    public ResponseEntity<?> clearCart(@PathVariable String username) {
        cartService.clearCart(username);
        return ResponseEntity.ok("Cart cleared");
    }
}
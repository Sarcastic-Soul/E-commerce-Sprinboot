package com.anish.e_commerce.service;

import com.anish.e_commerce.dto.CartItemResponse;
import com.anish.e_commerce.model.Cart;
import com.anish.e_commerce.model.CartItem;
import com.anish.e_commerce.model.Product;
import com.anish.e_commerce.repo.CartItemRepo;
import com.anish.e_commerce.repo.CartRepo;
import com.anish.e_commerce.repo.ProductRepo;
import com.anish.e_commerce.repo.UserRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepo cartRepo;
    private final CartItemRepo cartItemRepo;
    private final ProductRepo productRepo;
    private final UserRepo userRepo;

    public List<CartItemResponse> getCartItemsByUsername(String username) {
        return cartItemRepo.findItemsByUsername(username);
    }

    public Cart getCartByUsername(String username) {
        return cartRepo.findByUser_Username(username).orElseGet(() -> {
            Cart newCart = new Cart();
            newCart.setUser(userRepo.findByUsername(username).orElseThrow());
            newCart.setItems(new ArrayList<>());
            return cartRepo.save(newCart);
        });
    }

    public List<CartItemResponse> addOrUpdateItem(String username, int productId, int quantity) {
        Product product = productRepo.findById(productId)
                .orElseThrow(() -> new RuntimeException("Invalid Product ID"));

        Cart cart = getCartByUsername(username);
        Optional<CartItem> existingItem = cart.getItems().stream()
                .filter(item -> item.getProduct().getId() == productId)
                .findFirst();

        if (existingItem.isPresent()) {
            existingItem.get().setQuantity(quantity);
        } else {
            CartItem newItem = new CartItem();
            newItem.setCart(cart);
            newItem.setProduct(product);
            newItem.setQuantity(quantity);
            cart.getItems().add(newItem);
        }

        cartRepo.save(cart);
        return cartItemRepo.findItemsByUsername(username);
    }

    public List<CartItemResponse> removeItem(String username, int productId) {
        Cart cart = getCartByUsername(username);
        cart.getItems().removeIf(item -> item.getProduct().getId() == productId);
        cartRepo.save(cart);
        return cartItemRepo.findItemsByUsername(username);
    }

    public void clearCart(String username) {
        Cart cart = getCartByUsername(username);
        cart.getItems().clear();
        cartRepo.save(cart);
    }

}

package com.anish.e_commerce.service;

import com.anish.e_commerce.model.*;
import com.anish.e_commerce.repo.OrderRepo;
import com.anish.e_commerce.repo.ProductRepo;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepo orderRepo;
    private final ProductRepo productRepo;
    private final CartService cartService;

    @Transactional
    public Order placeOrder(String username) {
        Cart cart = cartService.getCartByUsername(username);

        if (cart.getItems().isEmpty()) {
            throw new RuntimeException("Cart is empty");
        }

        Order order = new Order();
        order.setUser(cart.getUser());
        order.setCreatedAt(LocalDateTime.now());
        order.setStatus("COMPLETED");

        BigDecimal totalAmount = BigDecimal.ZERO;

        for (CartItem item : cart.getItems()) {
            Product product = item.getProduct();

            if (product.getQuantity() < item.getQuantity()) {
                throw new RuntimeException(
                    "Sorry, " +
                        product.getName() +
                        " does not have enough stock."
                );
            }

            int newQuantity = product.getQuantity() - item.getQuantity();
            product.setQuantity(newQuantity);

            if (newQuantity == 0) {
                product.setAvailable(false);
            }

            productRepo.save(product);
        }

        order.setTotalAmount(totalAmount);
        Order savedOrder = orderRepo.save(order);

        cartService.clearCart(username);

        return savedOrder;
    }

    public List<Order> getUserOrders(String username) {
        return orderRepo.findByUser_UsernameOrderByCreatedAtDesc(username);
    }
}

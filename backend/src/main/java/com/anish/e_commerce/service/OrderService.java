package com.anish.e_commerce.service;

import com.anish.e_commerce.model.*;
import com.anish.e_commerce.repo.OrderRepo;
import com.anish.e_commerce.repo.UserRepo;
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
    private final CartService cartService;
    private final UserRepo userRepo;

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

        for (CartItem cartItem : cart.getItems()) {
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProduct(cartItem.getProduct());
            orderItem.setQuantity(cartItem.getQuantity());

            BigDecimal itemPrice = cartItem.getProduct().getPrice();
            orderItem.setPriceAtPurchase(itemPrice);

            // Calculate item total: price * quantity
            BigDecimal itemTotal = itemPrice.multiply(
                BigDecimal.valueOf(cartItem.getQuantity())
            );

            // Add to grand total
            totalAmount = totalAmount.add(itemTotal);

            order.getItems().add(orderItem);
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

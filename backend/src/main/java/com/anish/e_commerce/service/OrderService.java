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
    private final PaymentService paymentService;

    @Transactional
    public Order placeOrder(String username) {
        Cart cart = cartService.getCartByUsername(username);

        if (cart.getItems().isEmpty()) {
            throw new RuntimeException("Cart is empty");
        }

        Order order = new Order();
        order.setUser(cart.getUser());
        order.setCreatedAt(LocalDateTime.now());
        order.setStatus("PENDING");

        BigDecimal totalAmount = BigDecimal.ZERO;

        for (CartItem item : cart.getItems()) {
            Product product = item.getProduct();

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProduct(product);
            orderItem.setQuantity(item.getQuantity());
            orderItem.setPriceAtPurchase(product.getPrice());
            order.getItems().add(orderItem);

            totalAmount = totalAmount.add(
                product
                    .getPrice()
                    .multiply(BigDecimal.valueOf(item.getQuantity()))
            );

            if (product.getQuantity() < item.getQuantity()) {
                throw new RuntimeException(
                    "Sorry, " +
                        product.getName() +
                        " does not have enough stock."
                );
            }

            // Stock is NO LONGER deducted here. We wait for payment confirmation.
        }

        order.setTotalAmount(totalAmount);
        Order savedOrder = orderRepo.save(order);

        try {
            String razorpayOrderId = paymentService.createRazorpayOrder(
                totalAmount,
                savedOrder.getId().toString()
            );
            savedOrder.setRazorpayOrderId(razorpayOrderId);
            orderRepo.save(savedOrder);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize payment", e);
        }

        // Cart is NO LONGER cleared here. We wait for payment confirmation.
        return savedOrder;
    }

    @Transactional
    public void processSuccessfulPayment(String razorpayOrderId) {
        Order order = orderRepo
            .findByRazorpayOrderId(razorpayOrderId)
            .orElseThrow(() -> new RuntimeException("Order not found"));

        if ("COMPLETED".equals(order.getStatus())) {
            return; // Prevent duplicate deductions if called multiple times
        }

        order.setStatus("COMPLETED");

        // Deduct stock based on the user's current cart items
        Cart cart = cartService.getCartByUsername(
            order.getUser().getUsername()
        );
        for (CartItem item : cart.getItems()) {
            Product product = item.getProduct();
            int newQuantity = product.getQuantity() - item.getQuantity();

            if (newQuantity < 0) {
                newQuantity = 0; // Prevent negative stock
            }

            product.setQuantity(newQuantity);
            if (newQuantity == 0) {
                product.setAvailable(false);
            }
            productRepo.save(product);
        }

        orderRepo.save(order);

        // Clear the cart for the user
        cartService.clearCart(order.getUser().getUsername());
    }

    @Transactional
    public void processFailedPayment(String razorpayOrderId) {
        Order order = orderRepo
            .findByRazorpayOrderId(razorpayOrderId)
            .orElseThrow(() -> new RuntimeException("Order not found"));

        if ("PENDING".equals(order.getStatus())) {
            order.setStatus("REJECTED");
            orderRepo.save(order);
        }
    }

    public List<Order> getUserOrders(String username) {
        return orderRepo.findByUser_UsernameOrderByCreatedAtDesc(username);
    }
}

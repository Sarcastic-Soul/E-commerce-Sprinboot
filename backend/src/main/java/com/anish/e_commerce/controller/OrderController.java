package com.anish.e_commerce.controller;

import com.anish.e_commerce.model.Order;
import com.anish.e_commerce.service.OrderService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/place")
    public ResponseEntity<?> placeOrder(Authentication authentication) {
        Order order = orderService.placeOrder(authentication.getName());
        return ResponseEntity.ok(order);
    }

    @GetMapping("/history")
    public ResponseEntity<List<Order>> getOrderHistory(
        Authentication authentication
    ) {
        return ResponseEntity.ok(
            orderService.getUserOrders(authentication.getName())
        );
    }
}

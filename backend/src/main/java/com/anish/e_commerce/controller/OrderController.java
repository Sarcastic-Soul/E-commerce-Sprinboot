package com.anish.e_commerce.controller;

import com.anish.e_commerce.dto.OrderResponse;
import com.anish.e_commerce.dto.PlaceOrderRequest;
import com.anish.e_commerce.service.OrderExpiryJob;
import com.anish.e_commerce.service.OrderService;
import jakarta.validation.Valid;
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
    private final OrderExpiryJob orderExpiryJob;

    @PostMapping("/place")
    public ResponseEntity<OrderResponse> placeOrder(
        @Valid @RequestBody PlaceOrderRequest request,
        Authentication authentication
    ) {
        // Release stock held by abandoned checkouts first, so it can be bought
        // again. Done here, outside placeOrder's transaction, so each expiry
        // commits on its own.
        orderExpiryJob.sweepIfDue();
        return ResponseEntity.ok(
            orderService.placeOrder(authentication.getName(), request)
        );
    }

    @GetMapping("/history")
    public ResponseEntity<List<OrderResponse>> getOrderHistory(
        Authentication authentication
    ) {
        return ResponseEntity.ok(
            orderService.getUserOrders(authentication.getName())
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrder(
        @PathVariable Long id,
        Authentication authentication
    ) {
        return ResponseEntity.ok(
            orderService.getOrder(id, authentication.getName(), isAdmin(authentication))
        );
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<OrderResponse> cancelOrder(
        @PathVariable Long id,
        Authentication authentication
    ) {
        return ResponseEntity.ok(
            orderService.cancelOrder(id, authentication.getName())
        );
    }

    static boolean isAdmin(Authentication authentication) {
        return authentication != null && authentication
            .getAuthorities()
            .stream()
            .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }
}

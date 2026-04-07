package com.anish.e_commerce.controller;

import com.anish.e_commerce.service.OrderService;
import com.anish.e_commerce.service.PaymentService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final OrderService orderService;

    @PostMapping("/verify")
    public ResponseEntity<?> verifyPayment(
        @RequestBody Map<String, String> payload
    ) {
        String razorpayOrderId = payload.get("razorpay_order_id");
        String razorpayPaymentId = payload.get("razorpay_payment_id");
        String razorpaySignature = payload.get("razorpay_signature");

        boolean isValidSignature = paymentService.verifySignature(
            razorpayOrderId,
            razorpayPaymentId,
            razorpaySignature
        );

        if (isValidSignature) {
            try {
                orderService.processSuccessfulPayment(razorpayOrderId);
                return ResponseEntity.ok(
                    Map.of(
                        "status",
                        "success",
                        "message",
                        "Payment verified and order completed."
                    )
                );
            } catch (RuntimeException e) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    Map.of("error", e.getMessage())
                );
            }
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                Map.of("error", "Payment signature verification failed.")
            );
        }
    }

    @PostMapping("/fail")
    public ResponseEntity<?> failPayment(
        @RequestBody Map<String, String> payload
    ) {
        String razorpayOrderId = payload.get("razorpay_order_id");

        if (razorpayOrderId == null || razorpayOrderId.isEmpty()) {
            return ResponseEntity.badRequest().body(
                Map.of("error", "Razorpay Order ID is required")
            );
        }

        try {
            orderService.processFailedPayment(razorpayOrderId);
            return ResponseEntity.ok(
                Map.of(
                    "status",
                    "success",
                    "message",
                    "Payment failed. Order marked as rejected."
                )
            );
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                Map.of("error", e.getMessage())
            );
        }
    }
}

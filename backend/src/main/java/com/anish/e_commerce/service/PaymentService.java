package com.anish.e_commerce.service;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@Slf4j
public class PaymentService {

    private final RazorpayClient razorpayClient;
    private final String keySecret;

    public PaymentService(
            @Value("${razorpay.key.id}") String keyId,
            @Value("${razorpay.key.secret}") String keySecret) throws RazorpayException {
        this.razorpayClient = new RazorpayClient(keyId, keySecret);
        this.keySecret = keySecret;
    }

    public String createRazorpayOrder(BigDecimal amount, String receiptId) throws RazorpayException {
        JSONObject orderRequest = new JSONObject();
        int amountInSubunits = toSubunits(amount);
        orderRequest.put("amount", amountInSubunits);
        orderRequest.put("currency", "INR");
        orderRequest.put("receipt", receiptId);

        Order order = razorpayClient.orders.create(orderRequest);
        return order.get("id");
    }

    /**
     * Best effort: a failed refund (test keys, network, already refunded) is logged
     * and reported as false, never thrown — the caller's cancel must still go through.
     */
    public boolean refund(String paymentId, BigDecimal amount) {
        try {
            JSONObject request = new JSONObject();
            request.put("amount", toSubunits(amount));
            razorpayClient.payments.refund(paymentId, request);
            log.info("Refunded payment {} ({})", paymentId, amount);
            return true;
        } catch (Exception e) {
            log.warn("Refund failed for payment {}: {}", paymentId, e.getMessage());
            return false;
        }
    }

    // Razorpay expects the amount in subunits (paise for INR). Rounded, not
    // truncated, so 179.985 never silently loses a paisa.
    private static int toSubunits(BigDecimal amount) {
        return amount
            .multiply(new BigDecimal("100"))
            .setScale(0, RoundingMode.HALF_UP)
            .intValueExact();
    }

    public boolean verifySignature(String orderId, String paymentId, String signature) {
        try {
            JSONObject options = new JSONObject();
            options.put("razorpay_order_id", orderId);
            options.put("razorpay_payment_id", paymentId);
            options.put("razorpay_signature", signature);

            return Utils.verifyPaymentSignature(options, keySecret);
        } catch (RazorpayException e) {
            return false;
        }
    }
}

package com.anish.e_commerce.service;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
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
        // Razorpay expects the amount in subunits (paise for INR). So, multiply by 100.
        int amountInSubunits = amount.multiply(new BigDecimal("100")).intValue();
        orderRequest.put("amount", amountInSubunits);
        orderRequest.put("currency", "INR");
        orderRequest.put("receipt", receiptId);

        Order order = razorpayClient.orders.create(orderRequest);
        return order.get("id");
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

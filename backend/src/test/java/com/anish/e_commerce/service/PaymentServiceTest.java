package com.anish.e_commerce.service;

import static org.junit.jupiter.api.Assertions.*;

import com.razorpay.RazorpayException;
import java.nio.charset.StandardCharsets;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PaymentServiceTest {

    private static final String KEY_SECRET = "test_key_secret";

    private PaymentService paymentService;

    @BeforeEach
    void setUp() throws RazorpayException {
        // A syntactically valid (if fake) key id is enough: RazorpayClient's
        // constructor does not call the network, only createRazorpayOrder does.
        paymentService = new PaymentService("rzp_test_fake_key_id", KEY_SECRET);
    }

    /** Mirrors how Razorpay itself derives the signature: HMAC-SHA256(orderId|paymentId, secret). */
    private static String hmacSha256Hex(String data, String secret)
        throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(
            new SecretKeySpec(
                secret.getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"
            )
        );
        byte[] raw = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        StringBuilder hex = new StringBuilder();
        for (byte b : raw) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }

    @Test
    void verifySignature_TrueForACorrectlySignedPayload() throws Exception {
        String orderId = "order_abc123";
        String paymentId = "pay_xyz789";
        String signature = hmacSha256Hex(
            orderId + "|" + paymentId,
            KEY_SECRET
        );

        assertTrue(
            paymentService.verifySignature(orderId, paymentId, signature)
        );
    }

    @Test
    void verifySignature_FalseForATamperedSignature() throws Exception {
        String orderId = "order_abc123";
        String paymentId = "pay_xyz789";
        String signature = hmacSha256Hex(
            orderId + "|" + paymentId,
            KEY_SECRET
        );
        String tampered = signature.substring(0, signature.length() - 1) + (
            signature.endsWith("0") ? "1" : "0"
        );

        assertFalse(
            paymentService.verifySignature(orderId, paymentId, tampered)
        );
    }

    @Test
    void verifySignature_FalseForASignatureFromADifferentOrder() throws Exception {
        String signatureForOtherOrder = hmacSha256Hex(
            "order_other|pay_xyz789",
            KEY_SECRET
        );

        assertFalse(
            paymentService.verifySignature(
                "order_abc123",
                "pay_xyz789",
                signatureForOtherOrder
            )
        );
    }

    @Test
    void verifySignature_FalseRatherThanThrowingOnGarbageInput() {
        assertFalse(
            paymentService.verifySignature(
                "order_abc123",
                "pay_xyz789",
                "not-a-valid-signature"
            )
        );
    }
}

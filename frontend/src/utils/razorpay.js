import api from "../api/axios";

let scriptPromise = null;

/** Loads checkout.js once per page, no matter how many times Pay is pressed. */
export function loadRazorpayScript() {
    if (window.Razorpay) return Promise.resolve(true);
    if (scriptPromise) return scriptPromise;
    scriptPromise = new Promise((resolve) => {
        const script = document.createElement("script");
        script.src = "https://checkout.razorpay.com/v1/checkout.js";
        script.onload = () => resolve(true);
        script.onerror = () => {
            scriptPromise = null;
            resolve(false);
        };
        document.body.appendChild(script);
    });
    return scriptPromise;
}

async function markFailed(razorpayOrderId) {
    try {
        await api.post("/payments/fail", {
            razorpay_order_id: razorpayOrderId,
        });
    } catch (err) {
        console.error("Failed to update order status", err);
    }
}

// The public key ID comes from the backend, so rotating keys needs no rebuild.
let keyIdPromise;
export function fetchKeyId() {
    keyIdPromise ??= api
        .get("/payments/config")
        .then((res) => res.data.keyId)
        .catch((err) => {
            keyIdPromise = undefined; // let the next attempt retry
            throw err;
        });
    return keyIdPromise;
}

/**
 * Opens the Razorpay modal for an order returned by POST /orders/place.
 * `order.totalAmount` is already the discounted amount.
 *
 * onSuccess() runs after /payments/verify confirms; onFailure(message) runs when
 * the user closes the modal, the payment fails, or verification fails.
 */
export function openRazorpay({ order, keyId, username, darkMode, onSuccess, onFailure }) {
    // payment.failed and ondismiss can both fire for one attempt; report once.
    let settled = false;
    const fail = async (message) => {
        if (settled) return;
        settled = true;
        await markFailed(order.razorpayOrderId);
        onFailure(message);
    };

    const options = {
        key: keyId,
        amount: Math.round(Number(order.totalAmount) * 100),
        currency: "INR",
        name: "BrutShop",
        description: `Order #${order.id}`,
        order_id: order.razorpayOrderId,
        handler: async function (response) {
            settled = true;
            try {
                const verifyRes = await api.post("/payments/verify", {
                    razorpay_order_id: response.razorpay_order_id,
                    razorpay_payment_id: response.razorpay_payment_id,
                    razorpay_signature: response.razorpay_signature,
                });
                if (verifyRes.data.status === "success") onSuccess();
                else onFailure("Payment verification failed");
            } catch {
                onFailure("Payment verification failed");
            }
        },
        prefill: {
            name: order.shippingAddress?.fullName || username || "Guest",
            email: `${username || "guest"}@example.com`,
            contact: order.shippingAddress?.phone || "9999999999",
        },
        theme: { color: darkMode ? "#1f2937" : "#000000" },
        modal: {
            ondismiss: () => fail("Payment cancelled"),
        },
    };

    const paymentObject = new window.Razorpay(options);
    paymentObject.on("payment.failed", (response) =>
        fail(response.error?.description || "Payment failed"),
    );
    paymentObject.open();
}

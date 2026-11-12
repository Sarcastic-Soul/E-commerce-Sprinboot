import api from "../api/axios";
import type { Order, PaymentConfig, PaymentVerifyResult } from "../api/types";

// Minimal typings for Razorpay's checkout.js (loaded from their CDN at runtime).
interface RazorpaySuccess {
    razorpay_order_id: string;
    razorpay_payment_id: string;
    razorpay_signature: string;
}

interface RazorpayFailure {
    error?: { description?: string };
}

interface RazorpayOptions {
    key: string;
    amount: number;
    currency: string;
    name: string;
    description: string;
    order_id: string;
    handler: (response: RazorpaySuccess) => void | Promise<void>;
    prefill: { name: string; email: string; contact: string };
    theme: { color: string };
    modal: { ondismiss: () => void };
}

interface RazorpayInstance {
    on(event: "payment.failed", cb: (response: RazorpayFailure) => void): void;
    open(): void;
}

declare global {
    interface Window {
        Razorpay?: new (options: RazorpayOptions) => RazorpayInstance;
    }
}

let scriptPromise: Promise<boolean> | null = null;

/** Loads checkout.js once per page, no matter how many times Pay is pressed. */
export function loadRazorpayScript(): Promise<boolean> {
    if (window.Razorpay) return Promise.resolve(true);
    if (scriptPromise) return scriptPromise;
    scriptPromise = new Promise<boolean>((resolve) => {
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

async function markFailed(razorpayOrderId: string | null): Promise<void> {
    try {
        await api.post("/payments/fail", {
            razorpay_order_id: razorpayOrderId,
        });
    } catch (err) {
        console.error("Failed to update order status", err);
    }
}

// The public key ID comes from the backend, so rotating keys needs no rebuild.
let keyIdPromise: Promise<string> | undefined;
export function fetchKeyId(): Promise<string> {
    keyIdPromise ??= api
        .get<PaymentConfig>("/payments/config")
        .then((res) => res.data.keyId)
        .catch((err: unknown) => {
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
export interface OpenRazorpayArgs {
    order: Order;
    keyId: string;
    username: string | undefined;
    darkMode: boolean;
    onSuccess: () => void;
    onFailure: (message: string) => void;
}

export function openRazorpay({
    order,
    keyId,
    username,
    darkMode,
    onSuccess,
    onFailure,
}: OpenRazorpayArgs): void {
    const Razorpay = window.Razorpay;
    if (!Razorpay || !order.razorpayOrderId) {
        onFailure("Payment could not be started");
        return;
    }
    const razorpayOrderId = order.razorpayOrderId;

    // payment.failed and ondismiss can both fire for one attempt; report once.
    let settled = false;
    const fail = async (message: string) => {
        if (settled) return;
        settled = true;
        await markFailed(razorpayOrderId);
        onFailure(message);
    };

    const options: RazorpayOptions = {
        key: keyId,
        amount: Math.round(Number(order.totalAmount) * 100),
        currency: "INR",
        name: "BrutShop",
        description: `Order #${order.id}`,
        order_id: razorpayOrderId,
        handler: async (response) => {
            settled = true;
            try {
                const verifyRes = await api.post<PaymentVerifyResult>("/payments/verify", {
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
            ondismiss: () => void fail("Payment cancelled"),
        },
    };

    const paymentObject = new Razorpay(options);
    paymentObject.on("payment.failed", (response) =>
        void fail(response.error?.description || "Payment failed"),
    );
    paymentObject.open();
}

import { X, Plus, Minus } from "lucide-react";
import { useEffect, useRef, useState } from "react";
import { useTheme } from "../context/ThemeContext";
import { useAuth } from "../context/AuthContext";
import api from "../api/axios";
import { toast } from "sonner";
import { useCart } from "../context/CartContext.jsx";

export default function Cart({ closeCart }) {
    const { darkMode } = useTheme();
    const { user } = useAuth();
    const cartRef = useRef(null);
    const { setCartItemCount } = useCart();

    const [cartItems, setCartItems] = useState([]);
    const [cartTotal, setCartTotal] = useState(0);

    const fetchCart = async () => {
        try {
            const res = await api.get(`/cart/${user.username}`);
            setCartItems(res.data || []);
            calculateTotal(res.data || []);
        } catch (err) {
            toast.error("Failed to fetch cart");
        }
    };

    const calculateTotal = (items) => {
        const total = items.reduce(
            (sum, item) => sum + item.price * item.quantity,
            0,
        );
        setCartTotal(total);
    };

    const updateQuantity = async (productId, delta) => {
        const item = cartItems.find((item) => item.id === productId);
        if (!item) return;

        const newQty = item.quantity + delta;
        if (newQty <= 0) return;
        try {
            const res = await api.post(`/cart/${user.username}/add`, null, {
                params: { productId, quantity: newQty },
            });
            setCartItems(res.data);
            calculateTotal(res.data);
        } catch (err) {
            toast.error("Failed to update quantity");
        }
    };

    const removeFromCart = async (productId) => {
        try {
            const res = await api.delete(`/cart/${user.username}/remove`, {
                params: { productId },
            });
            setCartItems(res.data);
            calculateTotal(res.data);
            const totalCount = res.data.reduce(
                (acc, item) => acc + item.quantity,
                0,
            );
            setCartItemCount(totalCount);
        } catch (err) {
            toast.error("Failed to remove item");
        }
    };

    const clearCart = async () => {
        try {
            await api.delete(`/cart/${user.username}/clear`);
            setCartItems([]);
            setCartTotal(0);
            setCartItemCount(0);
        } catch (err) {
            toast.error("Failed to clear cart");
        }
    };

    const loadRazorpayScript = () => {
        return new Promise((resolve) => {
            const script = document.createElement("script");
            script.src = "https://checkout.razorpay.com/v1/checkout.js";
            script.onload = () => resolve(true);
            script.onerror = () => resolve(false);
            document.body.appendChild(script);
        });
    };

    const handleCheckout = async () => {
        const isLoaded = await loadRazorpayScript();
        if (!isLoaded) {
            toast.error("Razorpay SDK failed to load. Are you online?");
            return;
        }

        try {
            const response = await api.post("/orders/place");
            const orderData = response.data;

            const options = {
                key:
                    import.meta.env.VITE_RAZORPAY_KEY_ID ||
                    "rzp_test_your_dummy_key_id",
                amount: Math.round(orderData.totalAmount * 100),
                currency: "INR",
                name: "BrutShop",
                description: "Test Transaction",
                order_id: orderData.razorpayOrderId,
                handler: async function (response) {
                    try {
                        const verifyRes = await api.post("/payments/verify", {
                            razorpay_order_id: response.razorpay_order_id,
                            razorpay_payment_id: response.razorpay_payment_id,
                            razorpay_signature: response.razorpay_signature,
                        });

                        if (verifyRes.data.status === "success") {
                            toast.success(
                                "Payment successful! Order placed. 🚀",
                            );
                            setCartItems([]);
                            setCartTotal(0);
                            setCartItemCount(0);
                            closeCart();
                        }
                    } catch (err) {
                        toast.error("Payment verification failed");
                    }
                },
                prefill: {
                    name: user?.username || "Guest",
                    email: `${user?.username || "guest"}@example.com`,
                    contact: "9999999999",
                },
                theme: {
                    color: darkMode ? "#1f2937" : "#000000",
                },
                modal: {
                    ondismiss: async function () {
                        try {
                            await api.post("/payments/fail", {
                                razorpay_order_id: orderData.razorpayOrderId,
                            });
                        } catch (err) {
                            console.error(
                                "Failed to update order status on dismiss",
                                err,
                            );
                        }
                        toast.error("Payment cancelled");
                    },
                },
            };

            const paymentObject = new window.Razorpay(options);
            paymentObject.on("payment.failed", async function (response) {
                try {
                    await api.post("/payments/fail", {
                        razorpay_order_id: orderData.razorpayOrderId,
                    });
                } catch (err) {
                    console.error("Failed to update order status", err);
                }
                toast.error(response.error.description || "Payment failed");
            });
            paymentObject.open();
        } catch (err) {
            toast.error(
                err.response?.data?.message || "Failed to initialize checkout",
            );
        }
    };

    useEffect(() => {
        fetchCart();

        const timer = setTimeout(() => {
            if (cartRef.current) cartRef.current.classList.add("translate-x-0");
        }, 10);

        const handleClickOutside = (e) => {
            if (cartRef.current && !cartRef.current.contains(e.target)) {
                closeCart();
            }
        };

        document.addEventListener("mousedown", handleClickOutside);
        return () => {
            clearTimeout(timer);
            document.removeEventListener("mousedown", handleClickOutside);
        };
    }, [closeCart]);

    return (
        <div
            ref={cartRef}
            className={`w-full max-w-md fixed left-0 inset-y-0 z-50 transform -translate-x-full transition-transform duration-500 ease-out
                ${darkMode ? "bg-gray-800 text-white border-gray-700" : "bg-yellow-50 text-gray-900 border-black"}
                border-r-4 h-full overflow-y-auto shadow-lg`}
        >
            <div className="p-6">
                <div className="flex justify-between items-center mb-6">
                    <h2 className="text-2xl font-black">YOUR CART</h2>
                    <button
                        onClick={closeCart}
                        className={`p-2 rounded-full ${darkMode ? "bg-gray-700 hover:bg-gray-600" : "bg-black hover:bg-gray-800 text-white"}`}
                    >
                        <X size={24} />
                    </button>
                </div>

                {cartItems.length === 0 ? (
                    <div className="text-center py-10">
                        <p className="text-xl font-bold mb-4">
                            Your cart is empty
                        </p>
                        <button
                            onClick={closeCart}
                            className={`px-4 py-2 rounded font-bold border-2
                            ${darkMode ? "border-white" : "border-black"}
                            ${darkMode ? "bg-blue-600 hover:bg-blue-700" : "bg-black hover:bg-gray-800 text-white"}
                            shadow-[4px_4px_0px_0px_rgba(0,0,0,1)] active:translate-x-[2px] active:translate-y-[2px]`}
                        >
                            Continue Shopping
                        </button>
                    </div>
                ) : (
                    <>
                        <div className="space-y-4 mb-6">
                            {cartItems.map((item) => (
                                <div
                                    key={item.id}
                                    className={`p-4 flex rounded-lg border-2 ${darkMode ? "bg-gray-700 border-gray-600" : "bg-white border-black"}`}
                                >
                                    <img
                                        src={
                                            item.imageUrl ||
                                            "https://picsum.photos/400"
                                        }
                                        alt={item.name}
                                        className="w-20 h-20 object-cover rounded mr-4 border-2 border-black"
                                    />
                                    <div className="flex-1">
                                        <div className="flex justify-between">
                                            <h3 className="font-bold">
                                                {item.name}
                                            </h3>
                                            <button
                                                onClick={() =>
                                                    removeFromCart(item.id)
                                                }
                                            >
                                                <X
                                                    size={16}
                                                    className="text-gray-500 hover:text-red-600"
                                                />
                                            </button>
                                        </div>
                                        <p className="font-bold">
                                            ₹{item.price.toFixed(2)}
                                        </p>
                                        <div className="flex items-center mt-2">
                                            <button
                                                onClick={() =>
                                                    updateQuantity(item.id, -1)
                                                }
                                                disabled={item.quantity <= 1}
                                                className={`p-1 rounded ${darkMode ? "bg-gray-600" : "bg-gray-200"} hover:bg-gray-400`}
                                            >
                                                <Minus size={14} />
                                            </button>
                                            <span className="mx-2 font-bold">
                                                {item.quantity}
                                            </span>
                                            <button
                                                onClick={() =>
                                                    updateQuantity(item.id, 1)
                                                }
                                                className={`p-1 rounded ${darkMode ? "bg-gray-600" : "bg-gray-200"} hover:bg-gray-400`}
                                            >
                                                <Plus size={14} />
                                            </button>
                                        </div>
                                    </div>
                                </div>
                            ))}
                        </div>

                        <div
                            className={`p-4 mb-6 rounded-lg ${darkMode ? "bg-gray-700" : "bg-black text-white"}`}
                        >
                            <div className="flex justify-between items-center">
                                <span className="text-lg">Total:</span>
                                <span className="text-2xl font-black">
                                    ₹{cartTotal.toFixed(2)}
                                </span>
                            </div>
                        </div>

                        <button
                            onClick={handleCheckout}
                            className={`w-full py-3 px-6 rounded-lg font-bold text-lg mb-4
                            ${darkMode ? "bg-green-600 hover:bg-green-700" : "bg-green-500 hover:bg-green-600"} text-white
                            transition border-4 border-black shadow-[8px_8px_0px_0px_black] active:translate-x-[4px] active:translate-y-[4px]`}
                        >
                            Checkout
                        </button>

                        <button
                            onClick={clearCart}
                            className={`w-full py-2 px-4 rounded font-bold border-2
                            ${darkMode ? "border-white" : "border-black"} hover:opacity-70`}
                        >
                            Clear Cart
                        </button>
                    </>
                )}
            </div>
        </div>
    );
}

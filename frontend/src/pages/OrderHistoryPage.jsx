import { useEffect, useState } from "react";
import api from "../api/axios";
import { useTheme } from "../context/ThemeContext";
import LoadingState from "../components/LoadingState";

export default function OrderHistoryPage() {
    const [orders, setOrders] = useState([]);
    const [loading, setLoading] = useState(true);
    const { darkMode } = useTheme();

    useEffect(() => {
        const fetchOrders = async () => {
            try {
                const res = await api.get("/orders/history");
                setOrders(res.data);
            } catch (err) {
                console.error("Failed to fetch orders");
            } finally {
                setLoading(false);
            }
        };
        fetchOrders();
    }, []);

    if (loading) return <LoadingState />;

    return (
        <div className="max-w-4xl mx-auto mt-8">
            <h2 className="text-4xl font-black mb-8 border-b-4 border-black pb-2 inline-block">
                ORDER HISTORY
            </h2>

            {orders.length === 0 ? (
                <p className="text-xl font-bold">No orders placed yet.</p>
            ) : (
                <div className="space-y-6">
                    {orders.map((order) => (
                        <div
                            key={order.id}
                            className={`p-6 rounded-lg border-4 border-black shadow-[8px_8px_0px_0px_rgba(0,0,0,1)] ${darkMode ? "bg-gray-800" : "bg-white"}`}
                        >
                            <div className="flex justify-between border-b-2 border-black pb-4 mb-4">
                                <div>
                                    <p className="font-bold text-gray-500">
                                        Order #{order.id}
                                    </p>
                                    <p className="text-sm">
                                        {new Date(
                                            order.createdAt,
                                        ).toLocaleDateString()}
                                    </p>
                                </div>
                                <div className="text-right">
                                    <p className="font-black text-xl">
                                        ${order.totalAmount.toFixed(2)}
                                    </p>
                                    <span className="inline-block px-3 py-1 bg-green-200 text-green-800 font-bold text-xs rounded-full border-2 border-green-800 mt-1">
                                        {order.status}
                                    </span>
                                </div>
                            </div>
                            <div className="space-y-2">
                                {order.items.map((item) => (
                                    <div
                                        key={item.id}
                                        className="flex justify-between text-sm font-medium"
                                    >
                                        <span>
                                            {item.quantity}x {item.product.name}
                                        </span>
                                        <span>
                                            $
                                            {(
                                                item.priceAtPurchase *
                                                item.quantity
                                            ).toFixed(2)}
                                        </span>
                                    </div>
                                ))}
                            </div>
                        </div>
                    ))}
                </div>
            )}
        </div>
    );
}

import { useEffect, useState } from "react";
import api from "../api/axios";
import { useTheme } from "../context/ThemeContext";
import LoadingState from "../components/LoadingState";
import { useNavigate } from "react-router-dom";
import { ArrowLeft } from "lucide-react";

export default function OrderHistoryPage() {
    const [orders, setOrders] = useState([]);
    const [loading, setLoading] = useState(true);
    const { darkMode } = useTheme();
    const navigate = useNavigate();

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

    const getStatusColor = (status) => {
        switch (status) {
            case "COMPLETED":
                return "bg-green-200 text-green-800 border-green-800";
            case "PENDING":
                return "bg-yellow-200 text-yellow-800 border-yellow-800";
            case "REJECTED":
                return "bg-red-200 text-red-800 border-red-800";
            default:
                return "bg-gray-200 text-gray-800 border-gray-800";
        }
    };

    if (loading) return <LoadingState />;

    return (
        <div className="max-w-4xl mx-auto mt-8">
            <div className="flex items-center gap-4 mb-8">
                <button
                    onClick={() => navigate("/")}
                    className={`p-2 rounded-full border-2 border-black transition-transform active:translate-x-[2px] active:translate-y-[2px] shadow-[4px_4px_0px_0px_rgba(0,0,0,1)] active:shadow-none
                    ${darkMode ? "bg-gray-800 text-white" : "bg-white text-black"}`}
                >
                    <ArrowLeft size={24} />
                </button>
                <h1 className="text-4xl font-black border-b-4 border-black pb-2 inline-block">
                    ORDER HISTORY
                </h1>
            </div>

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
                                        ₹{order.totalAmount.toFixed(2)}
                                    </p>
                                    <span
                                        className={`inline-block px-3 py-1 font-bold text-xs rounded-full border-2 mt-1 ${getStatusColor(order.status)}`}
                                    >
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
                                            ₹
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

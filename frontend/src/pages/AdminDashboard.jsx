import { useEffect, useState } from "react";
import {
    LineChart,
    Line,
    XAxis,
    YAxis,
    CartesianGrid,
    Tooltip,
    ResponsiveContainer,
} from "recharts";
import {
    Users,
    Package,
    ShoppingCart,
    Activity,
    DollarSign,
    AlertCircle,
} from "lucide-react";
import api from "../api/axios";
import { useTheme } from "../context/ThemeContext";
import LoadingState from "../components/LoadingState";

export default function AdminDashboard() {
    const [stats, setStats] = useState(null);
    const [loading, setLoading] = useState(true);
    const { darkMode } = useTheme();

    useEffect(() => {
        const fetchStats = async () => {
            try {
                const res = await api.get("/admin/stats");
                setStats(res.data);
            } catch (err) {
                console.error("Failed to fetch admin stats", err);
            } finally {
                setLoading(false);
            }
        };
        fetchStats();
    }, []);

    if (loading) return <LoadingState />;
    if (!stats)
        return (
            <div className="text-center mt-10 font-bold text-xl">
                Failed to load dashboard.
            </div>
        );

    const statCards = [
        {
            title: "TOTAL USERS",
            value: stats.totalUsers,
            icon: Users,
            color: "bg-blue-400",
        },
        {
            title: "TOTAL PRODUCTS",
            value: stats.totalProducts,
            icon: Package,
            color: "bg-pink-400",
        },
        {
            title: "TOTAL ORDERS",
            value: stats.totalOrders,
            icon: ShoppingCart,
            color: "bg-green-400",
        },
        {
            title: "TOTAL REVENUE",
            value: `$${stats.totalRevenue ? stats.totalRevenue.toFixed(2) : "0.00"}`,
            icon: DollarSign,
            color: "bg-yellow-400",
        },
        {
            title: "OUT OF STOCK",
            value: stats.outOfStockProducts,
            icon: AlertCircle,
            color: stats.outOfStockProducts > 0 ? "bg-red-400" : "bg-gray-400",
        },
    ];

    return (
        <div className="max-w-6xl mx-auto mt-8">
            <h2 className="text-4xl font-black mb-8 border-b-4 border-black pb-2 inline-flex items-center gap-3">
                <Activity size={40} /> ADMIN DASHBOARD
            </h2>

            {/* Stat Cards */}
            <div className="grid grid-cols-1 md:grid-cols-3 gap-8 mb-12">
                {statCards.map((card, idx) => (
                    <div
                        key={idx}
                        className={`p-6 border-4 border-black rounded-lg shadow-[8px_8px_0px_0px_rgba(0,0,0,1)] flex items-center justify-between ${card.color} text-black`}
                    >
                        <div>
                            <p className="font-black text-sm tracking-widest opacity-80 mb-1">
                                {card.title}
                            </p>
                            <p className="text-5xl font-black">{card.value}</p>
                        </div>
                        <card.icon
                            size={60}
                            strokeWidth={2.5}
                            className="opacity-80"
                        />
                    </div>
                ))}
            </div>

            {/* Chart Section */}
            <div
                className={`p-8 border-4 border-black rounded-lg shadow-[8px_8px_0px_0px_rgba(0,0,0,1)] ${darkMode ? "bg-gray-800 text-white" : "bg-white text-black"}`}
            >
                <h3 className="text-2xl font-black mb-6">ORDERS OVER TIME</h3>
                <div className="h-80 w-full">
                    {stats.ordersOverTime.length > 0 ? (
                        <ResponsiveContainer width="100%" height="100%">
                            <LineChart data={stats.ordersOverTime}>
                                <CartesianGrid
                                    strokeDasharray="3 3"
                                    stroke={darkMode ? "#555" : "#ccc"}
                                />
                                <XAxis
                                    dataKey="date"
                                    stroke={darkMode ? "#fff" : "#000"}
                                    tick={{ fontWeight: "bold" }}
                                />
                                <YAxis
                                    allowDecimals={false}
                                    stroke={darkMode ? "#fff" : "#000"}
                                    tick={{ fontWeight: "bold" }}
                                />
                                <Tooltip
                                    contentStyle={{
                                        backgroundColor: darkMode
                                            ? "#1f2937"
                                            : "#fff",
                                        border: "4px solid black",
                                        fontWeight: "bold",
                                        color: darkMode ? "#fff" : "#000",
                                    }}
                                />
                                <Line
                                    type="monotone"
                                    dataKey="count"
                                    stroke="#ef4444"
                                    strokeWidth={4}
                                    activeDot={{
                                        r: 8,
                                        stroke: "black",
                                        strokeWidth: 2,
                                    }}
                                />
                            </LineChart>
                        </ResponsiveContainer>
                    ) : (
                        <div className="h-full flex items-center justify-center font-bold text-gray-500">
                            No orders placed yet.
                        </div>
                    )}
                </div>
            </div>
        </div>
    );
}

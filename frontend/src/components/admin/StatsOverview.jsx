import { useCallback, useEffect, useState } from "react";
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
    UsersIcon as Users,
    PackageIcon as Package,
    ShoppingCartIcon as ShoppingCart,
    CurrencyInrIcon as CurrencyInr,
    WarningCircleIcon as WarningCircle,
} from "@phosphor-icons/react";
import api from "../../api/axios";
import { useTheme } from "../../context/ThemeContext";
import LoadingState from "../LoadingState";
import ErrorState from "../ErrorState";
import { errorMessage } from "../../utils/format.js";

/** The original dashboard: stat cards plus orders/revenue charts. */
export default function StatsOverview() {
    const [stats, setStats] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const { darkMode } = useTheme();

    const load = useCallback(async () => {
        setLoading(true);
        setError(null);
        try {
            const res = await api.get("/admin/stats");
            setStats(res.data);
        } catch (err) {
            setError(errorMessage(err, "Could not load dashboard stats."));
        } finally {
            setLoading(false);
        }
    }, []);

    useEffect(() => {
        load();
    }, [load]);

    if (loading) return <LoadingState variant="page" />;
    if (error || !stats)
        return <ErrorState error={error || "Could not load dashboard stats."} onRetry={load} />;

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
            value: `₹${stats.totalRevenue ? stats.totalRevenue.toFixed(2) : "0.00"}`,
            icon: CurrencyInr,
            color: "bg-yellow-400",
        },
        {
            title: "OUT OF STOCK",
            value: stats.outOfStockProducts,
            icon: WarningCircle,
            color: stats.outOfStockProducts > 0 ? "bg-red-400" : "bg-gray-400",
        },
    ];

    return (
        <div>
            {/* Stat Cards */}
            <div className="grid grid-cols-1 md:grid-cols-3 lg:grid-cols-5 gap-6 mb-12">
                {statCards.map((card, idx) => (
                    <div
                        key={idx}
                        className={`p-6 border-4 border-black rounded-lg shadow-[8px_8px_0px_0px_rgba(0,0,0,1)] flex items-center justify-between ${card.color} text-black`}
                    >
                        <div>
                            <p className="font-black text-xs tracking-widest opacity-80 mb-1">
                                {card.title}
                            </p>
                            <p className="text-3xl font-black">{card.value}</p>
                        </div>
                        <card.icon
                            size={40}
                            className="opacity-80 shrink-0 ml-2"
                            aria-hidden="true"
                        />
                    </div>
                ))}
            </div>

            <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
                {/* 1. Orders Chart */}
                <div
                    className={`p-5 sm:p-8 border-4 border-black rounded-lg shadow-[8px_8px_0px_0px_rgba(0,0,0,1)] flex flex-col ${darkMode ? "bg-gray-800 text-white" : "bg-white text-black"}`}
                >
                    <h3 className="text-2xl font-black mb-6">
                        ORDERS OVER TIME
                    </h3>
                    <div className="h-80 w-full flex-grow">
                        {stats.ordersOverTime &&
                        stats.ordersOverTime.length > 0 ? (
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
                                        name="Orders"
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

                {/* 2. Revenue Chart */}
                <div
                    className={`p-5 sm:p-8 border-4 border-black rounded-lg shadow-[8px_8px_0px_0px_rgba(0,0,0,1)] flex flex-col ${darkMode ? "bg-gray-800 text-white" : "bg-white text-black"}`}
                >
                    <h3 className="text-2xl font-black mb-6">
                        REVENUE OVER TIME
                    </h3>
                    <div className="h-80 w-full flex-grow">
                        {stats.ordersOverTime &&
                        stats.ordersOverTime.length > 0 ? (
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
                                        stroke={darkMode ? "#fff" : "#000"}
                                        tick={{ fontWeight: "bold" }}
                                        width={80} // Extra width for formatting currency
                                        tickFormatter={(value) => `₹${value}`}
                                    />
                                    <Tooltip
                                        formatter={(value) => [
                                            `₹${Number(value).toFixed(2)}`,
                                            "Revenue",
                                        ]}
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
                                        dataKey="revenue" // Reads the "revenue" property from your backend data
                                        name="Revenue"
                                        stroke="#4ade80" // Green line for money
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
                                No revenue recorded yet.
                            </div>
                        )}
                    </div>
                </div>
            </div>
        </div>
    );
}

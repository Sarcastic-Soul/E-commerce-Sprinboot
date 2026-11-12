import { useQuery } from "@tanstack/react-query";
import { Link } from "react-router";
import { CaretRightIcon, ReceiptIcon } from "@phosphor-icons/react";
import api from "../api/axios";
import { useTheme } from "../context/ThemeContext";
import LoadingState from "../components/LoadingState";
import ErrorState from "../components/ErrorState";
import PageHeader from "../components/PageHeader";
import StatusBadge from "../components/StatusBadge";
import { productImage, PLACEHOLDER } from "../utils/image";
import { errorMessage, formatDate, formatMoney } from "../utils/format";
import { btn, focusRing, panel } from "../utils/ui";
import type { Order } from "../api/types";

export default function OrderHistoryPage() {
    const { darkMode } = useTheme();
    const {
        data: orders = [],
        isPending: loading,
        error: queryError,
        refetch: load,
    } = useQuery({
        queryKey: ["orders", "history"],
        queryFn: async () => (await api.get<Order[]>("/orders/history")).data || [],
    });
    const error = queryError ? errorMessage(queryError, "Could not load your orders.") : null;

    return (
        <div className="max-w-4xl mx-auto mt-8 pb-12">
            <PageHeader title="ORDER HISTORY" />

            {loading ? (
                <LoadingState variant="list" />
            ) : error ? (
                <ErrorState error={error} onRetry={load} />
            ) : orders.length === 0 ? (
                <div className={`${panel(darkMode)} text-center py-16 px-6`}>
                    <ReceiptIcon size={56} className="mx-auto mb-4 opacity-60" />
                    <h2 className="text-2xl font-black mb-2">No orders yet</h2>
                    <p className="font-bold opacity-70 mb-6">
                        Orders you place will show up here.
                    </p>
                    <Link to="/" className={btn(darkMode, "primary")}>
                        Start shopping
                    </Link>
                </div>
            ) : (
                <ul className="space-y-6">
                    {orders.map((order) => {
                        const itemCount = order.items.reduce((n, i) => n + i.quantity, 0);
                        return (
                            <li key={order.id}>
                                <Link
                                    to={`/orders/${order.id}`}
                                    className={`block p-5 sm:p-6 transition-transform hover:-translate-y-1 ${panel(darkMode)} ${focusRing}`}
                                    aria-label={`Order ${order.id}, ${formatMoney(order.totalAmount)}, view details`}
                                >
                                    <div className="flex flex-wrap justify-between gap-3 border-b-2 border-black pb-4 mb-4">
                                        <div>
                                            <p className="font-black text-lg">Order #{order.id}</p>
                                            <p className="text-sm font-bold opacity-70">
                                                {formatDate(order.createdAt)} &middot; {itemCount}{" "}
                                                {itemCount === 1 ? "item" : "items"}
                                            </p>
                                        </div>
                                        <div className="text-right">
                                            <p className="font-black text-xl">
                                                {formatMoney(order.totalAmount)}
                                            </p>
                                            <div className="mt-1">
                                                <StatusBadge status={order.status} />
                                            </div>
                                        </div>
                                    </div>
                                    <div className="flex items-center gap-3">
                                        <div className="flex -space-x-3 shrink-0">
                                            {order.items.slice(0, 4).map((item, idx) => (
                                                <img
                                                    key={`${item.productId}-${idx}`}
                                                    src={productImage(item.imageUrl, 120)}
                                                    alt=""
                                                    width={48}
                                                    height={48}
                                                    loading="lazy"
                                                    className="w-12 h-12 object-cover rounded border-2 border-black bg-white"
                                                    onError={(e) => {
                                                        e.currentTarget.src = PLACEHOLDER;
                                                    }}
                                                />
                                            ))}
                                        </div>
                                        <p className="flex-1 min-w-0 text-sm font-bold truncate">
                                            {order.items.map((i) => i.productName).join(", ")}
                                        </p>
                                        <CaretRightIcon size={20} className="shrink-0" aria-hidden="true" />
                                    </div>
                                </Link>
                            </li>
                        );
                    })}
                </ul>
            )}
        </div>
    );
}

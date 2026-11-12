import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Link, useParams } from "react-router";
import { isAxiosError } from "axios";
import { toast } from "sonner";
import api from "../api/axios";
import { useTheme } from "../context/ThemeContext";
import LoadingState from "../components/LoadingState";
import ErrorState from "../components/ErrorState";
import PageHeader from "../components/PageHeader";
import StatusBadge from "../components/StatusBadge";
import OrderTimeline from "../components/OrderTimeline";
import AddressLines from "../components/AddressLines";
import { productImage, PLACEHOLDER } from "../utils/image";
import { errorMessage, formatDateTime, formatMoney } from "../utils/format";
import { btn, focusRing, panel } from "../utils/ui";
import type { Order } from "../api/types";

export default function OrderDetailPage() {
    const { id } = useParams();
    const { darkMode } = useTheme();
    const queryClient = useQueryClient();
    const [confirming, setConfirming] = useState(false);

    const {
        data: order,
        isPending: loading,
        error: queryError,
        refetch: load,
    } = useQuery({
        queryKey: ["orders", id],
        queryFn: async () => (await api.get<Order>(`/orders/${id}`)).data,
    });
    const error = queryError
        ? isAxiosError(queryError) && queryError.response?.status === 404
            ? "We could not find this order."
            : errorMessage(queryError, "Could not load this order.")
        : null;

    const cancelMutation = useMutation({
        mutationFn: async () => (await api.post<Order>(`/orders/${id}/cancel`)).data,
        onSuccess: (data) => {
            queryClient.setQueryData(["orders", id], data);
            void queryClient.invalidateQueries({ queryKey: ["orders"] });
            setConfirming(false);
            toast.success("Order cancelled");
        },
        onError: (err) => {
            toast.error(errorMessage(err, "Could not cancel this order"));
        },
    });
    const cancelling = cancelMutation.isPending;
    const handleCancel = () => cancelMutation.mutate();

    if (loading) return <LoadingState variant="page" />;
    if (error) return <ErrorState error={error} onRetry={load} />;
    // Unreachable once loading and error are handled; narrows order for the compiler.
    if (!order) return null;

    const subtotal =
        order.subtotal ??
        order.items.reduce((s, i) => s + i.priceAtPurchase * i.quantity, 0);

    return (
        <div className="max-w-5xl mx-auto pb-12">
            <PageHeader title={`ORDER #${order.id}`} backTo={-1}>
                <StatusBadge status={order.status} />
            </PageHeader>

            <p className="font-bold opacity-70 -mt-4 mb-6">
                Placed {formatDateTime(order.createdAt)}
            </p>

            <div className="grid gap-8 lg:grid-cols-[minmax(0,320px)_1fr] items-start">
                <div className="space-y-8">
                    <section className={`${panel(darkMode)} p-5 sm:p-6`} aria-labelledby="progress-heading">
                        <h2 id="progress-heading" className="text-xl font-black mb-4">
                            STATUS
                        </h2>
                        <OrderTimeline order={order} />

                        {order.canCancel && (
                            <div className="mt-2 pt-4 border-t-4 border-black">
                                {confirming ? (
                                    <div role="alertdialog" aria-labelledby="cancel-q">
                                        <p id="cancel-q" className="font-black mb-3">
                                            Cancel this order? This cannot be undone.
                                        </p>
                                        <div className="flex flex-wrap gap-3">
                                            <button
                                                type="button"
                                                onClick={handleCancel}
                                                disabled={cancelling}
                                                className={btn(darkMode, "danger")}
                                            >
                                                {cancelling ? "Cancelling..." : "Yes, cancel"}
                                            </button>
                                            <button
                                                type="button"
                                                onClick={() => setConfirming(false)}
                                                disabled={cancelling}
                                                className={btn(darkMode, "secondary")}
                                            >
                                                Keep order
                                            </button>
                                        </div>
                                    </div>
                                ) : (
                                    <button
                                        type="button"
                                        onClick={() => setConfirming(true)}
                                        className={`${btn(darkMode, "danger")} w-full`}
                                    >
                                        Cancel order
                                    </button>
                                )}
                            </div>
                        )}
                    </section>

                    <section className={`${panel(darkMode)} p-5 sm:p-6`} aria-labelledby="ship-heading">
                        <h2 id="ship-heading" className="text-xl font-black mb-3">
                            SHIPPING TO
                        </h2>
                        {order.shippingAddress ? (
                            <AddressLines address={order.shippingAddress} />
                        ) : (
                            <p className="font-bold opacity-70">
                                No address saved for this order.
                            </p>
                        )}
                    </section>
                </div>

                <section className={`${panel(darkMode)} p-5 sm:p-6`} aria-labelledby="items-heading">
                    <h2 id="items-heading" className="text-xl font-black mb-4">
                        ITEMS
                    </h2>
                    <ul className="divide-y-2 divide-black mb-6">
                        {order.items.map((item, idx) => (
                            <li key={`${item.productId}-${idx}`} className="py-3 first:pt-0">
                                <Link
                                    to={`/product/${item.productId}`}
                                    className={`flex gap-4 items-center rounded-lg -m-1 p-1 hover:underline ${focusRing}`}
                                >
                                    <img
                                        src={productImage(item.imageUrl, 160)}
                                        alt=""
                                        width={64}
                                        height={64}
                                        loading="lazy"
                                        className="w-16 h-16 object-cover rounded border-2 border-black shrink-0"
                                        onError={(e) => {
                                            e.currentTarget.src = PLACEHOLDER;
                                        }}
                                    />
                                    <span className="flex-1 min-w-0">
                                        <span className="block font-black truncate">
                                            {item.productName}
                                        </span>
                                        <span className="block text-sm font-bold opacity-70">
                                            {item.quantity} x {formatMoney(item.priceAtPurchase)}
                                        </span>
                                    </span>
                                    <span className="font-black shrink-0">
                                        {formatMoney(item.priceAtPurchase * item.quantity)}
                                    </span>
                                </Link>
                            </li>
                        ))}
                    </ul>

                    <dl className="space-y-2 font-bold">
                        <div className="flex justify-between">
                            <dt>Subtotal</dt>
                            <dd>{formatMoney(subtotal)}</dd>
                        </div>
                        {Number(order.discount) > 0 && (
                            <div className={`flex justify-between ${darkMode ? "text-green-300" : "text-green-700"}`}>
                                <dt>
                                    Discount{order.couponCode ? ` (${order.couponCode})` : ""}
                                </dt>
                                <dd>-{formatMoney(order.discount)}</dd>
                            </div>
                        )}
                        <div
                            className={`flex justify-between items-center p-4 rounded-lg mt-3 ${darkMode ? "bg-gray-700" : "bg-black text-white"}`}
                        >
                            <dt className="text-lg">Total</dt>
                            <dd className="text-2xl font-black">{formatMoney(order.totalAmount)}</dd>
                        </div>
                    </dl>
                </section>
            </div>
        </div>
    );
}

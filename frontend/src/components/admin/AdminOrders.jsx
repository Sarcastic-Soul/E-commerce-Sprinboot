import { useCallback, useEffect, useId, useState } from "react";
import { Link } from "react-router-dom";
import { toast } from "sonner";
import api from "../../api/axios";
import { useTheme } from "../../context/ThemeContext";
import LoadingState from "../LoadingState";
import ErrorState from "../ErrorState";
import StatusBadge from "../StatusBadge";
import Pager from "./Pager";
import { ADMIN_TRANSITIONS, ORDER_STATUSES, STATUS_LABELS } from "../../utils/orderStatus.js";
import { errorMessage, formatDateTime, formatMoney } from "../../utils/format.js";
import { btn, focusRing, inputCls, labelCls, panel } from "../../utils/ui.js";

const PAGE_SIZE = 20;

export default function AdminOrders() {
    const { darkMode } = useTheme();
    const filterId = useId();
    const [status, setStatus] = useState("");
    const [page, setPage] = useState(0);
    const [data, setData] = useState({ content: [], totalPages: 0, totalElements: 0 });
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [busyId, setBusyId] = useState(null);

    const load = useCallback(async () => {
        setLoading(true);
        setError(null);
        try {
            const params = { page, size: PAGE_SIZE };
            if (status) params.status = status;
            const res = await api.get("/admin/orders", { params });
            setData({
                content: res.data.content || [],
                totalPages: res.data.page?.totalPages || 0,
                totalElements: res.data.page?.totalElements || 0,
            });
        } catch (err) {
            setError(errorMessage(err, "Could not load orders."));
        } finally {
            setLoading(false);
        }
    }, [page, status]);

    useEffect(() => {
        load();
    }, [load]);

    const changeStatus = async (order, to) => {
        if (
            to === "CANCELLED" &&
            !window.confirm(`Cancel order #${order.id}? Stock goes back on sale.`)
        )
            return;
        setBusyId(order.id);
        try {
            const res = await api.put(`/admin/orders/${order.id}/status`, { status: to });
            setData((d) => ({
                ...d,
                content: d.content.map((o) => (o.id === order.id ? res.data : o)),
            }));
            toast.success(`Order #${order.id} is now ${STATUS_LABELS[to].toLowerCase()}`);
        } catch (err) {
            toast.error(errorMessage(err, "Could not change the status"));
        } finally {
            setBusyId(null);
        }
    };

    return (
        <section className={`${panel(darkMode)} p-5 sm:p-6`} aria-labelledby="admin-orders-h">
            <div className="flex flex-wrap items-end justify-between gap-4 mb-6">
                <h2 id="admin-orders-h" className="text-2xl font-black">
                    ORDERS
                    {!loading && !error && (
                        <span className="ml-2 text-base opacity-70">({data.totalElements})</span>
                    )}
                </h2>
                <div className="w-full sm:w-56">
                    <label htmlFor={filterId} className={labelCls}>
                        Filter by status
                    </label>
                    <select
                        id={filterId}
                        value={status}
                        onChange={(e) => {
                            setStatus(e.target.value);
                            setPage(0);
                        }}
                        className={`${inputCls(darkMode)} cursor-pointer`}
                    >
                        <option value="">All statuses</option>
                        {ORDER_STATUSES.map((s) => (
                            <option key={s} value={s}>
                                {STATUS_LABELS[s]}
                            </option>
                        ))}
                    </select>
                </div>
            </div>

            {loading ? (
                <LoadingState variant="list" />
            ) : error ? (
                <ErrorState error={error} onRetry={load} />
            ) : data.content.length === 0 ? (
                <p className="py-10 text-center font-black text-lg opacity-70">
                    {status ? `No ${STATUS_LABELS[status].toLowerCase()} orders.` : "No orders yet."}
                </p>
            ) : (
                <div className="overflow-x-auto -mx-1 px-1">
                    <table className="w-full min-w-[720px] text-left border-collapse">
                        <thead>
                            <tr className="border-b-4 border-black text-xs tracking-widest">
                                <th scope="col" className="py-2 pr-3 font-black">ORDER</th>
                                <th scope="col" className="py-2 pr-3 font-black">DATE</th>
                                <th scope="col" className="py-2 pr-3 font-black">CUSTOMER</th>
                                <th scope="col" className="py-2 pr-3 font-black">ITEMS</th>
                                <th scope="col" className="py-2 pr-3 font-black text-right">TOTAL</th>
                                <th scope="col" className="py-2 pr-3 font-black">STATUS</th>
                                <th scope="col" className="py-2 font-black">ACTIONS</th>
                            </tr>
                        </thead>
                        <tbody>
                            {data.content.map((o) => {
                                const transitions = ADMIN_TRANSITIONS[o.status] || [];
                                return (
                                    <tr key={o.id} className={`border-b-2 ${darkMode ? "border-gray-600" : "border-gray-300"}`}>
                                        <td className="py-3 pr-3">
                                            <Link
                                                to={`/orders/${o.id}`}
                                                className={`font-black underline inline-flex items-center min-h-[44px] rounded ${focusRing}`}
                                            >
                                                #{o.id}
                                            </Link>
                                        </td>
                                        <td className="py-3 pr-3 text-sm font-bold whitespace-nowrap">
                                            {formatDateTime(o.createdAt)}
                                        </td>
                                        <td className="py-3 pr-3 font-bold">{o.username}</td>
                                        <td className="py-3 pr-3 font-bold">
                                            {o.items.reduce((n, i) => n + i.quantity, 0)}
                                        </td>
                                        <td className="py-3 pr-3 font-black text-right whitespace-nowrap">
                                            {formatMoney(o.totalAmount)}
                                        </td>
                                        <td className="py-3 pr-3">
                                            <StatusBadge status={o.status} />
                                        </td>
                                        <td className="py-3">
                                            {transitions.length === 0 ? (
                                                <span className="text-sm font-bold opacity-50">None</span>
                                            ) : (
                                                <div className="flex flex-wrap gap-2">
                                                    {transitions.map((t) => (
                                                        <button
                                                            key={t.to}
                                                            type="button"
                                                            onClick={() => changeStatus(o, t.to)}
                                                            disabled={busyId === o.id}
                                                            className={`${btn(darkMode, t.to === "CANCELLED" ? "danger" : "secondary")} text-sm px-3 whitespace-nowrap`}
                                                        >
                                                            {t.label}
                                                        </button>
                                                    ))}
                                                </div>
                                            )}
                                        </td>
                                    </tr>
                                );
                            })}
                        </tbody>
                    </table>
                </div>
            )}

            {!loading && !error && (
                <Pager page={page} totalPages={data.totalPages} onChange={setPage} />
            )}
        </section>
    );
}

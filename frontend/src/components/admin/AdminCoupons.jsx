import { useCallback, useEffect, useState } from "react";
import { toast } from "sonner";
import { PencilSimpleIcon, PlusIcon, ProhibitIcon, TicketIcon } from "@phosphor-icons/react";
import api from "../../api/axios";
import { useTheme } from "../../context/ThemeContext";
import LoadingState from "../LoadingState";
import ErrorState from "../ErrorState";
import CouponForm from "./CouponForm";
import { errorMessage, formatDate, formatMoney } from "../../utils/format.js";
import { btn, panel } from "../../utils/ui.js";

const describe = (c) =>
    c.type === "PERCENT" ? `${Number(c.value)}% off` : `${formatMoney(c.value)} off`;

export default function AdminCoupons() {
    const { darkMode } = useTheme();
    const [coupons, setCoupons] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [editing, setEditing] = useState(null); // coupon, "new", or null
    const [busyId, setBusyId] = useState(null);

    const load = useCallback(async () => {
        setLoading(true);
        setError(null);
        try {
            const { data } = await api.get("/admin/coupons");
            setCoupons(data || []);
        } catch (err) {
            setError(errorMessage(err, "Could not load coupons."));
        } finally {
            setLoading(false);
        }
    }, []);

    useEffect(() => {
        load();
    }, [load]);

    const handleSave = async (values) => {
        try {
            if (editing === "new") {
                await api.post("/admin/coupons", values);
                toast.success(`Coupon ${values.code} created`);
            } else {
                await api.put(`/admin/coupons/${editing.id}`, values);
                toast.success(`Coupon ${values.code} saved`);
            }
            setEditing(null);
            await load();
        } catch (err) {
            toast.error(errorMessage(err, "Could not save the coupon"));
        }
    };

    const handleDeactivate = async (c) => {
        if (!window.confirm(`Turn off coupon ${c.code}? Shoppers will no longer be able to use it.`))
            return;
        setBusyId(c.id);
        try {
            await api.delete(`/admin/coupons/${c.id}`);
            toast.success(`Coupon ${c.code} turned off`);
            setCoupons((list) => list.map((x) => (x.id === c.id ? { ...x, active: false } : x)));
        } catch (err) {
            toast.error(errorMessage(err, "Could not turn off the coupon"));
        } finally {
            setBusyId(null);
        }
    };

    const isExpired = (c) => c.expiresAt && new Date(c.expiresAt) < new Date();

    return (
        <section className={`${panel(darkMode)} p-5 sm:p-6`} aria-labelledby="coupons-h">
            <div className="flex flex-wrap items-center justify-between gap-3 mb-6">
                <h2 id="coupons-h" className="text-2xl font-black">
                    COUPONS
                </h2>
                {!editing && (
                    <button type="button" onClick={() => setEditing("new")} className={btn(darkMode, "primary")}>
                        <PlusIcon size={18} /> New coupon
                    </button>
                )}
            </div>

            {editing && (
                <div className="mb-6 p-4 rounded-lg border-4 border-black">
                    <h3 className="text-lg font-black mb-3">
                        {editing === "new" ? "New coupon" : `Edit ${editing.code}`}
                    </h3>
                    <CouponForm
                        key={editing === "new" ? "new" : editing.id}
                        initial={editing === "new" ? null : editing}
                        onSubmit={handleSave}
                        onCancel={() => setEditing(null)}
                    />
                </div>
            )}

            {loading ? (
                <LoadingState variant="list" />
            ) : error ? (
                <ErrorState error={error} onRetry={load} />
            ) : coupons.length === 0 ? (
                <div className="py-10 text-center">
                    <TicketIcon size={48} className="mx-auto mb-2 opacity-60" />
                    <p className="font-black text-lg">No coupons yet.</p>
                </div>
            ) : (
                <ul className="space-y-4">
                    {coupons.map((c) => (
                        <li
                            key={c.id}
                            className={`p-4 rounded-lg border-4 flex flex-wrap items-center gap-4 ${c.active ? "border-black" : darkMode ? "border-gray-600 opacity-70" : "border-gray-300 opacity-70"}`}
                        >
                            <div className="flex-1 min-w-[200px]">
                                <div className="flex flex-wrap items-center gap-2 mb-1">
                                    <span className="font-mono font-black text-lg px-2 border-2 border-black rounded bg-yellow-300 text-black">
                                        {c.code}
                                    </span>
                                    <span
                                        className={`px-2 py-0.5 text-xs font-black border-2 border-black rounded ${
                                            !c.active
                                                ? "bg-gray-300 text-gray-900"
                                                : isExpired(c)
                                                  ? "bg-red-200 text-red-900"
                                                  : "bg-green-200 text-green-900"
                                        }`}
                                    >
                                        {!c.active ? "OFF" : isExpired(c) ? "EXPIRED" : "ACTIVE"}
                                    </span>
                                </div>
                                <p className="font-bold">
                                    {describe(c)}
                                    {Number(c.minOrderAmount) > 0 && ` on orders of ${formatMoney(c.minOrderAmount)} or more`}
                                </p>
                                <p className="text-sm font-bold opacity-70">
                                    Used {c.usedCount ?? 0}
                                    {c.maxUses != null ? ` of ${c.maxUses}` : " times (no limit)"}
                                    {c.expiresAt ? ` · Expires ${formatDate(c.expiresAt)}` : " · No expiry"}
                                </p>
                            </div>
                            <div className="flex flex-wrap gap-2">
                                <button
                                    type="button"
                                    onClick={() => setEditing(c)}
                                    className={btn(darkMode, "secondary")}
                                    aria-label={`Edit coupon ${c.code}`}
                                >
                                    <PencilSimpleIcon size={18} /> Edit
                                </button>
                                {c.active && (
                                    <button
                                        type="button"
                                        onClick={() => handleDeactivate(c)}
                                        disabled={busyId === c.id}
                                        className={btn(darkMode, "danger")}
                                        aria-label={`Turn off coupon ${c.code}`}
                                    >
                                        <ProhibitIcon size={18} /> Turn off
                                    </button>
                                )}
                            </div>
                        </li>
                    ))}
                </ul>
            )}
        </section>
    );
}

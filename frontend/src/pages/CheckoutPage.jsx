import { useCallback, useEffect, useId, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { toast } from "sonner";
import {
    CircleNotchIcon,
    LockSimpleIcon,
    PlusIcon,
    TagIcon,
    XIcon,
} from "@phosphor-icons/react";
import api from "../api/axios";
import { useAuth } from "../context/AuthContext";
import { useTheme } from "../context/ThemeContext";
import { useCart } from "../context/CartContext.jsx";
import LoadingState from "../components/LoadingState";
import ErrorState from "../components/ErrorState";
import PageHeader from "../components/PageHeader";
import AddressForm from "../components/AddressForm";
import AddressLines from "../components/AddressLines";
import { productImage, PLACEHOLDER } from "../utils/image.js";
import { errorMessage, formatMoney } from "../utils/format.js";
import { fetchKeyId, loadRazorpayScript, openRazorpay } from "../utils/razorpay.js";
import { btn, focusRing, inputCls, labelCls, panel } from "../utils/ui.js";

export default function CheckoutPage() {
    const { user } = useAuth();
    const { darkMode } = useTheme();
    const { setCartItemCount } = useCart();
    const navigate = useNavigate();
    const couponId = useId();

    const [cartItems, setCartItems] = useState([]);
    const [addresses, setAddresses] = useState([]);
    const [selectedId, setSelectedId] = useState(null);
    const [showNewAddress, setShowNewAddress] = useState(false);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    const [couponInput, setCouponInput] = useState("");
    const [coupon, setCoupon] = useState(null); // validate response
    const [couponError, setCouponError] = useState("");
    const [checkingCoupon, setCheckingCoupon] = useState(false);

    const [paying, setPaying] = useState(false);

    const load = useCallback(async () => {
        setLoading(true);
        setError(null);
        try {
            const [cartRes, addrRes] = await Promise.all([
                api.get(`/cart/${user.username}`),
                api.get("/user/addresses"),
            ]);
            setCartItems(cartRes.data || []);
            const list = addrRes.data || [];
            setAddresses(list);
            const preferred = list.find((a) => a.isDefault) || list[0];
            setSelectedId(preferred ? preferred.id : null);
            setShowNewAddress(list.length === 0);
        } catch (err) {
            setError(errorMessage(err, "Could not load checkout. Please try again."));
        } finally {
            setLoading(false);
        }
    }, [user.username]);

    useEffect(() => {
        load();
    }, [load]);

    const subtotal = cartItems.reduce((sum, i) => sum + i.price * i.quantity, 0);
    const discount = coupon ? Number(coupon.discount) : 0;
    const total = coupon ? Number(coupon.total) : subtotal;

    const handleAddAddress = async (values) => {
        try {
            const { data } = await api.post("/user/addresses", values);
            setAddresses((list) => [...list, data]);
            setSelectedId(data.id);
            setShowNewAddress(false);
            toast.success("Address saved");
        } catch (err) {
            toast.error(errorMessage(err, "Could not save the address"));
        }
    };

    const applyCoupon = async (e) => {
        e.preventDefault();
        const code = couponInput.trim();
        if (!code) {
            setCouponError("Enter a coupon code");
            return;
        }
        setCheckingCoupon(true);
        setCouponError("");
        try {
            const { data } = await api.post("/coupons/validate", { code });
            setCoupon(data);
            toast.success(data.message || `Coupon ${data.code} applied`);
        } catch (err) {
            setCoupon(null);
            setCouponError(errorMessage(err, "This coupon cannot be used"));
        } finally {
            setCheckingCoupon(false);
        }
    };

    const removeCoupon = () => {
        setCoupon(null);
        setCouponInput("");
        setCouponError("");
    };

    const handlePay = async () => {
        if (paying) return;
        if (!selectedId) {
            toast.error("Choose a delivery address first");
            return;
        }
        setPaying(true);

        const [isLoaded, keyId] = await Promise.all([
            loadRazorpayScript(),
            fetchKeyId().catch(() => null),
        ]);
        if (!isLoaded || !keyId) {
            toast.error("Could not load Razorpay. Check your connection.");
            setPaying(false);
            return;
        }

        let order;
        try {
            const { data } = await api.post("/orders/place", {
                addressId: selectedId,
                couponCode: coupon?.code ?? null,
            });
            order = data;
        } catch (err) {
            toast.error(errorMessage(err, "Could not start checkout"));
            setPaying(false);
            return;
        }

        openRazorpay({
            order,
            keyId,
            username: user.username,
            darkMode,
            onSuccess: () => {
                setCartItemCount(0);
                toast.success("Payment received. Your order is placed.");
                navigate(`/orders/${order.id}`);
            },
            onFailure: (message) => {
                toast.error(message);
                setPaying(false);
            },
        });
    };

    if (loading) return <LoadingState variant="page" />;
    if (error) return <ErrorState error={error} onRetry={load} />;

    if (cartItems.length === 0) {
        return (
            <div className="max-w-5xl mx-auto">
                <PageHeader title="CHECKOUT" />
                <div className={`${panel(darkMode)} p-10 text-center`}>
                    <h2 className="text-2xl font-black mb-2">Your cart is empty</h2>
                    <p className="font-bold opacity-70 mb-6">
                        Add something to your cart to check out.
                    </p>
                    <Link to="/" className={btn(darkMode, "primary")}>
                        Browse products
                    </Link>
                </div>
            </div>
        );
    }

    return (
        <div className="max-w-5xl mx-auto pb-12">
            <PageHeader title="CHECKOUT" backTo={-1} />

            <div className="grid gap-8 lg:grid-cols-[1fr_minmax(0,380px)] items-start">
                <div className="space-y-8 min-w-0">
                    {/* Address picker */}
                    <section className={`${panel(darkMode)} p-5 sm:p-6`} aria-labelledby="addr-heading">
                        <h2 id="addr-heading" className="text-2xl font-black mb-4">
                            1. DELIVERY ADDRESS
                        </h2>

                        {addresses.length > 0 && (
                            <fieldset className="space-y-3 mb-4">
                                <legend className="sr-only">Choose a delivery address</legend>
                                {addresses.map((a) => {
                                    const checked = a.id === selectedId;
                                    return (
                                        <label
                                            key={a.id}
                                            className={`flex gap-3 p-4 rounded-lg border-4 cursor-pointer has-[:focus-visible]:outline-4 has-[:focus-visible]:outline-offset-2 has-[:focus-visible]:outline-rose-500
                                            ${checked
                                                ? darkMode
                                                    ? "border-yellow-400 bg-gray-700 shadow-[4px_4px_0px_0px_rgba(0,0,0,1)]"
                                                    : "border-black bg-yellow-100 shadow-[4px_4px_0px_0px_rgba(0,0,0,1)]"
                                                : darkMode
                                                  ? "border-gray-600"
                                                  : "border-gray-300"}`}
                                        >
                                            <input
                                                type="radio"
                                                name="address"
                                                value={a.id}
                                                checked={checked}
                                                onChange={() => setSelectedId(a.id)}
                                                className={`mt-1 w-5 h-5 shrink-0 focus:outline-none ${darkMode ? "accent-yellow-400" : "accent-black"}`}
                                            />
                                            <span className="min-w-0 flex-1">
                                                <AddressLines address={a} />
                                                {a.isDefault && (
                                                    <span className="inline-block mt-2 px-2 py-0.5 text-xs font-black border-2 border-black rounded bg-yellow-300 text-black">
                                                        DEFAULT
                                                    </span>
                                                )}
                                            </span>
                                        </label>
                                    );
                                })}
                            </fieldset>
                        )}

                        {showNewAddress ? (
                            <div className={addresses.length > 0 ? "pt-4 border-t-4 border-black" : ""}>
                                <h3 className="text-lg font-black mb-3">
                                    {addresses.length > 0 ? "New address" : "Add your delivery address"}
                                </h3>
                                <AddressForm
                                    onSubmit={handleAddAddress}
                                    onCancel={
                                        addresses.length > 0
                                            ? () => setShowNewAddress(false)
                                            : undefined
                                    }
                                    submitLabel="Save and use this address"
                                />
                            </div>
                        ) : (
                            <button
                                type="button"
                                onClick={() => setShowNewAddress(true)}
                                className={btn(darkMode, "secondary")}
                            >
                                <PlusIcon size={18} /> Add new address
                            </button>
                        )}
                    </section>

                    {/* Items */}
                    <section className={`${panel(darkMode)} p-5 sm:p-6`} aria-labelledby="items-heading">
                        <h2 id="items-heading" className="text-2xl font-black mb-4">
                            2. ITEMS ({cartItems.reduce((n, i) => n + i.quantity, 0)})
                        </h2>
                        <ul className="divide-y-2 divide-black">
                            {cartItems.map((item) => (
                                <li key={item.id} className="flex gap-4 py-3 first:pt-0 last:pb-0">
                                    <img
                                        src={productImage(item.imageUrl, 160)}
                                        alt=""
                                        width={64}
                                        height={64}
                                        loading="lazy"
                                        className="w-16 h-16 object-cover rounded border-2 border-black shrink-0"
                                        onError={(e) => {
                                            e.target.src = PLACEHOLDER;
                                        }}
                                    />
                                    <div className="flex-1 min-w-0">
                                        <p className="font-black truncate">{item.name}</p>
                                        <p className="text-sm font-bold opacity-70">
                                            {item.quantity} x {formatMoney(item.price)}
                                        </p>
                                    </div>
                                    <p className="font-black shrink-0">
                                        {formatMoney(item.price * item.quantity)}
                                    </p>
                                </li>
                            ))}
                        </ul>
                    </section>
                </div>

                {/* Summary */}
                <aside className={`${panel(darkMode)} p-5 sm:p-6 lg:sticky lg:top-6`} aria-labelledby="summary-heading">
                    <h2 id="summary-heading" className="text-2xl font-black mb-4">
                        3. PAY
                    </h2>

                    {coupon ? (
                        <div className="flex items-center justify-between gap-3 mb-4 p-3 rounded-lg border-4 border-black bg-green-200 text-green-900">
                            <span className="flex items-center gap-2 font-black min-w-0">
                                <TagIcon size={20} className="shrink-0" />
                                <span className="truncate">{coupon.code}</span>
                            </span>
                            <button
                                type="button"
                                onClick={removeCoupon}
                                aria-label={`Remove coupon ${coupon.code}`}
                                className={`w-11 h-11 flex items-center justify-center rounded-lg border-2 border-black bg-white text-black hover:bg-gray-100 shrink-0 ${focusRing}`}
                            >
                                <XIcon size={18} />
                            </button>
                        </div>
                    ) : (
                        <form onSubmit={applyCoupon} className="mb-4" noValidate>
                            <label htmlFor={couponId} className={labelCls}>
                                Coupon code
                            </label>
                            <div className="flex gap-2">
                                <input
                                    id={couponId}
                                    value={couponInput}
                                    onChange={(e) => {
                                        setCouponInput(e.target.value.toUpperCase());
                                        setCouponError("");
                                    }}
                                    autoComplete="off"
                                    placeholder="e.g. WELCOME10"
                                    aria-invalid={Boolean(couponError)}
                                    aria-describedby={couponError ? `${couponId}-err` : undefined}
                                    className={`${inputCls(darkMode)} uppercase placeholder:normal-case`}
                                />
                                <button
                                    type="submit"
                                    disabled={checkingCoupon}
                                    className={`${btn(darkMode, "ink")} shrink-0`}
                                >
                                    {checkingCoupon ? "Checking..." : "Apply"}
                                </button>
                            </div>
                            {couponError && (
                                <p
                                    id={`${couponId}-err`}
                                    role="alert"
                                    className={`mt-2 text-sm font-bold ${darkMode ? "text-red-300" : "text-red-700"}`}
                                >
                                    {couponError}
                                </p>
                            )}
                        </form>
                    )}

                    <dl className="space-y-2 font-bold mb-4">
                        <div className="flex justify-between">
                            <dt>Subtotal</dt>
                            <dd>{formatMoney(coupon ? coupon.subtotal : subtotal)}</dd>
                        </div>
                        {coupon && (
                            <div className={`flex justify-between ${darkMode ? "text-green-300" : "text-green-700"}`}>
                                <dt>Discount ({coupon.code})</dt>
                                <dd>-{formatMoney(discount)}</dd>
                            </div>
                        )}
                        <div className="flex justify-between">
                            <dt>Delivery</dt>
                            <dd>Free</dd>
                        </div>
                    </dl>

                    <div className={`p-4 mb-5 rounded-lg flex justify-between items-center ${darkMode ? "bg-gray-700" : "bg-black text-white"}`}>
                        <span className="text-lg">Total</span>
                        <span className="text-2xl font-black">{formatMoney(total)}</span>
                    </div>

                    <button
                        type="button"
                        onClick={handlePay}
                        disabled={paying || !selectedId}
                        className={`${btn(darkMode, "success")} w-full text-lg py-3 shadow-[8px_8px_0px_0px_black]`}
                    >
                        {paying ? (
                            <>
                                <CircleNotchIcon size={20} className="animate-spin" /> Processing...
                            </>
                        ) : (
                            <>
                                <LockSimpleIcon size={20} /> Pay {formatMoney(total)}
                            </>
                        )}
                    </button>
                    {!selectedId && (
                        <p className="mt-3 text-sm font-bold opacity-80">
                            Add a delivery address to continue.
                        </p>
                    )}
                </aside>
            </div>
        </div>
    );
}

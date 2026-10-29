import { useCallback, useEffect, useId, useState } from "react";
import { Link } from "react-router-dom";
import { CheckCircleIcon, PencilSimpleIcon } from "@phosphor-icons/react";
import api from "../../api/axios";
import { useTheme } from "../../context/ThemeContext";
import LoadingState from "../LoadingState";
import ErrorState from "../ErrorState";
import { productImage, PLACEHOLDER } from "../../utils/image.js";
import { errorMessage } from "../../utils/format.js";
import { btn, inputCls, labelCls, panel } from "../../utils/ui.js";

export default function LowStockList() {
    const { darkMode } = useTheme();
    const inputId = useId();
    const [input, setInput] = useState("5");
    const [threshold, setThreshold] = useState(5);
    const [products, setProducts] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    // Wait for typing to pause before hitting the API.
    useEffect(() => {
        const n = Number.parseInt(input, 10);
        if (Number.isNaN(n) || n < 0) return;
        const t = setTimeout(() => setThreshold(n), 400);
        return () => clearTimeout(t);
    }, [input]);

    const load = useCallback(async () => {
        setLoading(true);
        setError(null);
        try {
            const { data } = await api.get("/admin/products/low-stock", {
                params: { threshold },
            });
            setProducts(data || []);
        } catch (err) {
            setError(errorMessage(err, "Could not load low stock products."));
        } finally {
            setLoading(false);
        }
    }, [threshold]);

    useEffect(() => {
        load();
    }, [load]);

    return (
        <section className={`${panel(darkMode)} p-5 sm:p-6`} aria-labelledby="lowstock-h">
            <div className="flex flex-wrap items-end justify-between gap-4 mb-6">
                <h2 id="lowstock-h" className="text-2xl font-black">
                    LOW STOCK
                </h2>
                <div className="w-full sm:w-48">
                    <label htmlFor={inputId} className={labelCls}>
                        Show stock at or below
                    </label>
                    <input
                        id={inputId}
                        type="number"
                        min="0"
                        inputMode="numeric"
                        value={input}
                        onChange={(e) => setInput(e.target.value)}
                        className={inputCls(darkMode)}
                    />
                </div>
            </div>

            {loading ? (
                <LoadingState variant="list" />
            ) : error ? (
                <ErrorState error={error} onRetry={load} />
            ) : products.length === 0 ? (
                <div className="py-10 text-center">
                    <CheckCircleIcon size={48} className="mx-auto mb-2 text-green-500" />
                    <p className="font-black text-lg">
                        No products at or below {threshold} in stock.
                    </p>
                </div>
            ) : (
                <ul className="divide-y-2 divide-black">
                    {products.map((p) => (
                        <li key={p.id} className="flex items-center gap-4 py-3">
                            <img
                                src={productImage(p.imageUrl, 120)}
                                alt=""
                                width={48}
                                height={48}
                                loading="lazy"
                                className="w-12 h-12 object-cover rounded border-2 border-black shrink-0"
                                onError={(e) => {
                                    e.target.src = PLACEHOLDER;
                                }}
                            />
                            <div className="flex-1 min-w-0">
                                <Link to={`/product/${p.id}`} className="font-black underline block truncate">
                                    {p.name}
                                </Link>
                                <p className="text-sm font-bold opacity-70">{p.brand}</p>
                            </div>
                            <span
                                className={`px-3 py-1 rounded-full border-2 border-black font-black text-sm shrink-0 ${p.quantity === 0 ? "bg-red-400 text-black" : "bg-yellow-300 text-black"}`}
                            >
                                {p.quantity} left
                            </span>
                            <Link
                                to={`/update-product/${p.id}`}
                                className={`${btn(darkMode, "secondary")} shrink-0`}
                                aria-label={`Edit ${p.name}`}
                            >
                                <PencilSimpleIcon size={18} />
                                <span className="hidden sm:inline">Edit</span>
                            </Link>
                        </li>
                    ))}
                </ul>
            )}
        </section>
    );
}

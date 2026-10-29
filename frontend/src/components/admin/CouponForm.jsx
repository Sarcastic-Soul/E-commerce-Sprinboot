import { useId, useState } from "react";
import { useTheme } from "../../context/ThemeContext";
import { btn, inputCls, labelCls } from "../../utils/ui.js";

// "2026-11-01T10:30" for <input type="datetime-local">, in local time.
function toLocalInput(iso) {
    if (!iso) return "";
    const d = new Date(iso);
    if (Number.isNaN(d.getTime())) return "";
    const pad = (n) => String(n).padStart(2, "0");
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

function validate(v) {
    const e = {};
    if (!/^[A-Z0-9_-]{3,30}$/.test(v.code.trim()))
        e.code = "3 to 30 letters, numbers, - or _";
    const value = Number(v.value);
    if (!v.value || Number.isNaN(value) || value <= 0) e.value = "Enter a value above 0";
    else if (v.type === "PERCENT" && (value < 1 || value > 90))
        e.value = "Percent must be between 1 and 90";
    if (v.minOrderAmount !== "" && Number(v.minOrderAmount) < 0)
        e.minOrderAmount = "Cannot be negative";
    if (v.maxUses !== "" && (!Number.isInteger(Number(v.maxUses)) || Number(v.maxUses) < 1))
        e.maxUses = "Whole number of 1 or more, or leave empty";
    return e;
}

export default function CouponForm({ initial, onSubmit, onCancel }) {
    const { darkMode } = useTheme();
    const idp = useId();
    const [values, setValues] = useState(() => ({
        code: initial?.code ?? "",
        type: initial?.type ?? "PERCENT",
        value: initial?.value != null ? String(initial.value) : "",
        minOrderAmount: initial?.minOrderAmount != null ? String(initial.minOrderAmount) : "0",
        maxUses: initial?.maxUses != null ? String(initial.maxUses) : "",
        expiresAt: toLocalInput(initial?.expiresAt),
        active: initial?.active ?? true,
    }));
    const [errors, setErrors] = useState({});
    const [saving, setSaving] = useState(false);

    const set = (name, value) => {
        setValues((v) => ({ ...v, [name]: value }));
        if (errors[name]) setErrors((e) => ({ ...e, [name]: undefined }));
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        const found = validate(values);
        setErrors(found);
        if (Object.keys(found).length) return;

        setSaving(true);
        try {
            await onSubmit({
                code: values.code.trim().toUpperCase(),
                type: values.type,
                value: Number(values.value),
                minOrderAmount: values.minOrderAmount === "" ? 0 : Number(values.minOrderAmount),
                maxUses: values.maxUses === "" ? null : Number(values.maxUses),
                expiresAt: values.expiresAt ? new Date(values.expiresAt).toISOString() : null,
                active: values.active,
            });
        } finally {
            setSaving(false);
        }
    };

    const field = (name, label, props = {}) => {
        const id = `${idp}-${name}`;
        return (
            <div>
                <label htmlFor={id} className={labelCls}>
                    {label}
                </label>
                <input
                    id={id}
                    value={values[name]}
                    onChange={(e) => set(name, e.target.value)}
                    aria-invalid={Boolean(errors[name])}
                    aria-describedby={errors[name] ? `${id}-err` : undefined}
                    className={inputCls(darkMode)}
                    {...props}
                />
                {errors[name] && (
                    <p id={`${id}-err`} className={`mt-1 text-sm font-bold ${darkMode ? "text-red-300" : "text-red-700"}`}>
                        {errors[name]}
                    </p>
                )}
            </div>
        );
    };

    return (
        <form onSubmit={handleSubmit} noValidate className="space-y-4">
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                {field("code", "Code", {
                    onChange: (e) => set("code", e.target.value.toUpperCase()),
                    autoComplete: "off",
                    className: `${inputCls(darkMode)} uppercase`,
                })}
                <div>
                    <label htmlFor={`${idp}-type`} className={labelCls}>
                        Type
                    </label>
                    <select
                        id={`${idp}-type`}
                        value={values.type}
                        onChange={(e) => set("type", e.target.value)}
                        className={`${inputCls(darkMode)} cursor-pointer`}
                    >
                        <option value="PERCENT">Percent off</option>
                        <option value="FLAT">Flat amount off (₹)</option>
                    </select>
                </div>
                {field("value", values.type === "PERCENT" ? "Percent off (1-90)" : "Amount off (₹)", {
                    type: "number",
                    min: "0",
                    step: "0.01",
                    inputMode: "decimal",
                })}
                {field("minOrderAmount", "Minimum order (₹)", {
                    type: "number",
                    min: "0",
                    step: "0.01",
                    inputMode: "decimal",
                })}
                {field("maxUses", "Max uses (empty = no limit)", {
                    type: "number",
                    min: "1",
                    step: "1",
                    inputMode: "numeric",
                })}
                {field("expiresAt", "Expires (empty = never)", { type: "datetime-local" })}
            </div>
            <label className="inline-flex items-center gap-3 min-h-[44px] font-black cursor-pointer">
                <input
                    type="checkbox"
                    checked={values.active}
                    onChange={(e) => set("active", e.target.checked)}
                    className="w-5 h-5 accent-black"
                />
                Active
            </label>
            <div className="flex flex-wrap gap-3">
                <button type="submit" disabled={saving} className={btn(darkMode, "primary")}>
                    {saving ? "Saving..." : initial ? "Save coupon" : "Create coupon"}
                </button>
                <button type="button" onClick={onCancel} className={btn(darkMode, "secondary")}>
                    Cancel
                </button>
            </div>
        </form>
    );
}

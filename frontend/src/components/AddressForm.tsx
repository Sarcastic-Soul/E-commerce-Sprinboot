import { useId, useState, type ChangeEvent, type FormEvent } from "react";
import type { AddressRequest } from "../api/types";
import { useTheme } from "../context/ThemeContext";
import { btn, inputCls, labelCls } from "../utils/ui";

type AddressField =
    | "fullName"
    | "phone"
    | "line1"
    | "line2"
    | "city"
    | "state"
    | "postalCode"
    | "country";

type AddressValues = Record<AddressField, string>;
type AddressErrors = Partial<Record<AddressField, string>>;

/** What onSubmit receives: trimmed values, with an empty line2 sent as null. */
export type AddressFormPayload = Omit<AddressRequest, "line2" | "isDefault"> & {
    line2: string | null;
};

interface FieldConfig {
    name: AddressField;
    label: string;
    autoComplete: string;
    type?: string;
    wide?: boolean;
}

const EMPTY: AddressValues = {
    fullName: "",
    phone: "",
    line1: "",
    line2: "",
    city: "",
    state: "",
    postalCode: "",
    country: "India",
};

const FIELDS: FieldConfig[] = [
    { name: "fullName", label: "Full name", autoComplete: "name", wide: true },
    { name: "phone", label: "Phone", autoComplete: "tel", type: "tel" },
    { name: "postalCode", label: "PIN / postal code", autoComplete: "postal-code" },
    { name: "line1", label: "Address line 1", autoComplete: "address-line1", wide: true },
    { name: "line2", label: "Address line 2 (optional)", autoComplete: "address-line2", wide: true },
    { name: "city", label: "City", autoComplete: "address-level2" },
    { name: "state", label: "State", autoComplete: "address-level1" },
    { name: "country", label: "Country", autoComplete: "country-name" },
];

// Mirrors the backend rules in the API contract so most mistakes are caught
// before a round trip.
function validate(values: AddressValues): AddressErrors {
    const errors: AddressErrors = {};
    for (const f of ["fullName", "line1", "city", "state"] as const) {
        if (!values[f].trim()) errors[f] = "Required";
    }
    if (!/^\+?\d{7,15}$/.test(values.phone.trim()))
        errors.phone = "Use 7 to 15 digits, with an optional leading +";
    const pin = values.postalCode.trim();
    if (pin.length < 3 || pin.length > 10)
        errors.postalCode = "Use 3 to 10 characters";
    return errors;
}

export interface AddressFormProps {
    /** Existing address to edit; any missing or null field starts empty. */
    initial?: Partial<Record<AddressField, string | null>> | null;
    onSubmit: (payload: AddressFormPayload) => Promise<unknown> | void;
    onCancel?: () => void;
    submitLabel?: string;
}

export default function AddressForm({
    initial,
    onSubmit,
    onCancel,
    submitLabel = "Save address",
}: AddressFormProps) {
    const { darkMode } = useTheme();
    const idPrefix = useId();
    const [values, setValues] = useState<AddressValues>(() => {
        const start = { ...EMPTY };
        if (initial) {
            for (const key of Object.keys(EMPTY) as AddressField[]) start[key] = initial[key] ?? "";
            if (!start.country) start.country = "India";
        }
        return start;
    });
    const [errors, setErrors] = useState<AddressErrors>({});
    const [saving, setSaving] = useState(false);

    const handleChange = (e: ChangeEvent<HTMLInputElement>) => {
        const { value } = e.target;
        // Inputs are only rendered from FIELDS, so name is always an AddressField.
        const name = e.target.name as AddressField;
        setValues((v) => ({ ...v, [name]: value }));
        if (errors[name]) setErrors((er) => ({ ...er, [name]: undefined }));
    };

    const handleSubmit = async (e: FormEvent<HTMLFormElement>) => {
        e.preventDefault();
        e.stopPropagation();
        const found = validate(values);
        setErrors(found);
        if (Object.keys(found).length > 0) return;

        const payload: AddressFormPayload = {
            fullName: values.fullName.trim(),
            phone: values.phone.trim(),
            line1: values.line1.trim(),
            line2: values.line2.trim() || null,
            city: values.city.trim(),
            state: values.state.trim(),
            postalCode: values.postalCode.trim(),
            country: values.country.trim() || "India",
        };

        setSaving(true);
        try {
            await onSubmit(payload);
        } finally {
            setSaving(false);
        }
    };

    return (
        <form onSubmit={handleSubmit} noValidate className="space-y-4">
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                {FIELDS.map((f) => {
                    const id = `${idPrefix}-${f.name}`;
                    const errId = `${id}-error`;
                    return (
                        <div key={f.name} className={f.wide ? "sm:col-span-2" : ""}>
                            <label htmlFor={id} className={labelCls}>
                                {f.label}
                            </label>
                            <input
                                id={id}
                                name={f.name}
                                type={f.type || "text"}
                                autoComplete={f.autoComplete}
                                value={values[f.name]}
                                onChange={handleChange}
                                aria-invalid={Boolean(errors[f.name])}
                                aria-describedby={errors[f.name] ? errId : undefined}
                                className={inputCls(darkMode)}
                            />
                            {errors[f.name] && (
                                <p
                                    id={errId}
                                    className={`mt-1 text-sm font-bold ${darkMode ? "text-red-300" : "text-red-700"}`}
                                >
                                    {errors[f.name]}
                                </p>
                            )}
                        </div>
                    );
                })}
            </div>
            <div className="flex flex-wrap gap-3">
                <button type="submit" disabled={saving} className={btn(darkMode, "primary")}>
                    {saving ? "Saving..." : submitLabel}
                </button>
                {onCancel && (
                    <button
                        type="button"
                        onClick={onCancel}
                        className={btn(darkMode, "secondary")}
                    >
                        Cancel
                    </button>
                )}
            </div>
        </form>
    );
}

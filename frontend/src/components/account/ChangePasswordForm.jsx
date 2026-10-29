import { useId, useState } from "react";
import { toast } from "sonner";
import { EyeIcon, EyeSlashIcon } from "@phosphor-icons/react";
import api from "../../api/axios";
import { useTheme } from "../../context/ThemeContext";
import { errorMessage } from "../../utils/format.js";
import { btn, focusRing, inputCls, labelCls, panel } from "../../utils/ui.js";

const EMPTY = { currentPassword: "", newPassword: "", confirmPassword: "" };

export default function ChangePasswordForm() {
    const { darkMode } = useTheme();
    const idp = useId();
    const [values, setValues] = useState(EMPTY);
    const [errors, setErrors] = useState({});
    const [show, setShow] = useState(false);
    const [saving, setSaving] = useState(false);

    const fields = [
        { name: "currentPassword", label: "Current password", autoComplete: "current-password" },
        { name: "newPassword", label: "New password (at least 8 characters)", autoComplete: "new-password" },
        { name: "confirmPassword", label: "Confirm new password", autoComplete: "new-password" },
    ];

    const handleChange = (e) => {
        const { name, value } = e.target;
        setValues((v) => ({ ...v, [name]: value }));
        if (errors[name]) setErrors((er) => ({ ...er, [name]: undefined }));
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        const found = {};
        if (!values.currentPassword) found.currentPassword = "Required";
        if (values.newPassword.length < 8)
            found.newPassword = "Use at least 8 characters";
        if (values.confirmPassword !== values.newPassword)
            found.confirmPassword = "Passwords do not match";
        setErrors(found);
        if (Object.keys(found).length) return;

        setSaving(true);
        try {
            await api.put("/user/password", {
                currentPassword: values.currentPassword,
                newPassword: values.newPassword,
            });
            toast.success("Password changed");
            setValues(EMPTY);
        } catch (err) {
            const msg = errorMessage(err, "Could not change password");
            if (/current password/i.test(msg)) setErrors({ currentPassword: msg });
            toast.error(msg);
        } finally {
            setSaving(false);
        }
    };

    return (
        <section className={`${panel(darkMode)} p-5 sm:p-6`} aria-labelledby={`${idp}-h`}>
            <h2 id={`${idp}-h`} className="text-2xl font-black mb-4">
                CHANGE PASSWORD
            </h2>
            <form onSubmit={handleSubmit} noValidate className="space-y-4">
                {fields.map((f) => {
                    const id = `${idp}-${f.name}`;
                    return (
                        <div key={f.name}>
                            <label htmlFor={id} className={labelCls}>
                                {f.label}
                            </label>
                            <input
                                id={id}
                                name={f.name}
                                type={show ? "text" : "password"}
                                autoComplete={f.autoComplete}
                                value={values[f.name]}
                                onChange={handleChange}
                                aria-invalid={Boolean(errors[f.name])}
                                aria-describedby={errors[f.name] ? `${id}-err` : undefined}
                                className={inputCls(darkMode)}
                            />
                            {errors[f.name] && (
                                <p
                                    id={`${id}-err`}
                                    className={`mt-1 text-sm font-bold ${darkMode ? "text-red-300" : "text-red-700"}`}
                                >
                                    {errors[f.name]}
                                </p>
                            )}
                        </div>
                    );
                })}
                <div className="flex flex-wrap items-center gap-3">
                    <button type="submit" disabled={saving} className={btn(darkMode, "primary")}>
                        {saving ? "Saving..." : "Change password"}
                    </button>
                    <button
                        type="button"
                        onClick={() => setShow((s) => !s)}
                        aria-pressed={show}
                        className={`inline-flex items-center gap-2 min-h-[44px] px-3 font-bold underline rounded-lg ${focusRing}`}
                    >
                        {show ? <EyeSlashIcon size={20} /> : <EyeIcon size={20} />}
                        {show ? "Hide passwords" : "Show passwords"}
                    </button>
                </div>
            </form>
        </section>
    );
}

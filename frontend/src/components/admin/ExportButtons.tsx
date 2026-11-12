import { useState } from "react";
import { toast } from "sonner";
import { DownloadSimpleIcon } from "@phosphor-icons/react";
import api from "../../api/axios";
import { useTheme } from "../../context/ThemeContext";
import { saveBlob } from "../../utils/format";
import { btn } from "../../utils/ui";

interface ExportDef {
    key: string;
    label: string;
    url: string;
    file: string;
}

const EXPORTS: ExportDef[] = [
    { key: "orders", label: "Orders CSV", url: "/admin/orders/export", file: "orders.csv" },
    { key: "products", label: "Products CSV", url: "/admin/products/export", file: "products.csv" },
];

export default function ExportButtons() {
    const { darkMode } = useTheme();
    const [busy, setBusy] = useState<string | null>(null);

    // Through axios (not a plain <a href>) so the auth cookie refresh flow applies.
    const download = async (exp: ExportDef) => {
        setBusy(exp.key);
        try {
            const res = await api.get<Blob>(exp.url, { responseType: "blob" });
            saveBlob(res.data, exp.file);
            toast.success(`Downloaded ${exp.file}`);
        } catch {
            toast.error(`Could not download ${exp.file}`);
        } finally {
            setBusy(null);
        }
    };

    return (
        <div className="flex flex-wrap gap-3">
            {EXPORTS.map((exp) => (
                <button
                    key={exp.key}
                    type="button"
                    onClick={() => download(exp)}
                    disabled={busy === exp.key}
                    className={btn(darkMode, "secondary")}
                >
                    <DownloadSimpleIcon size={18} />
                    {busy === exp.key ? "Downloading..." : exp.label}
                </button>
            ))}
        </div>
    );
}

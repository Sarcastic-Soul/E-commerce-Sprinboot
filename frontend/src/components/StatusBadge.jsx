import { STATUS_COLORS, STATUS_LABELS } from "../utils/orderStatus.js";

export default function StatusBadge({ status }) {
    return (
        <span
            className={`inline-block px-3 py-1 font-black text-xs rounded-full border-2 border-black whitespace-nowrap ${
                STATUS_COLORS[status] || "bg-gray-200 text-gray-900"
            }`}
        >
            {STATUS_LABELS[status] || status}
        </span>
    );
}

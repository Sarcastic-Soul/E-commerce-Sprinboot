import {
    CheckIcon,
    ClockIcon,
    CreditCardIcon,
    HourglassIcon,
    PackageIcon,
    ProhibitIcon,
    TruckIcon,
    XCircleIcon,
} from "@phosphor-icons/react";
import { useTheme } from "../context/ThemeContext";
import { formatDateTime } from "../utils/format.js";

const STEPS = [
    { status: "PENDING", label: "Order placed", at: "createdAt", Icon: ClockIcon },
    { status: "PAID", label: "Paid", at: "paidAt", Icon: CreditCardIcon },
    { status: "SHIPPED", label: "Shipped", at: "shippedAt", Icon: TruckIcon },
    { status: "DELIVERED", label: "Delivered", at: "deliveredAt", Icon: PackageIcon },
];

const EXITS = {
    CANCELLED: { label: "Cancelled", at: "cancelledAt", Icon: ProhibitIcon, cls: "bg-gray-300 text-gray-900" },
    REJECTED: { label: "Payment failed", at: null, Icon: XCircleIcon, cls: "bg-red-300 text-red-950" },
    EXPIRED: { label: "Expired (not paid in 30 minutes)", at: null, Icon: HourglassIcon, cls: "bg-gray-200 text-gray-800" },
};

/**
 * PENDING -> PAID -> SHIPPED -> DELIVERED, with CANCELLED / REJECTED / EXPIRED
 * drawn as a distinct final step after the last step the order reached.
 */
export default function OrderTimeline({ order }) {
    const { darkMode } = useTheme();
    const status = order.status === "COMPLETED" ? "PAID" : order.status;
    const exit = EXITS[status];
    const mainIndex = STEPS.findIndex((s) => s.status === status);

    // For an exit status, the reached steps are the ones with a timestamp.
    const reached = (i) =>
        exit ? i === 0 || Boolean(order[STEPS[i].at]) : i <= mainIndex;

    const steps = exit
        ? STEPS.filter((_, i) => reached(i))
        : STEPS;

    return (
        <ol className="relative space-y-0" aria-label="Order progress">
            {steps.map((step, i) => {
                const done = exit ? true : reached(STEPS.indexOf(step));
                const current =
                    !exit && step.status === status && status !== "DELIVERED";
                const isLast = i === steps.length - 1 && !exit;
                return (
                    <li key={step.status} className="flex gap-4">
                        <div className="flex flex-col items-center">
                            <span
                                className={`w-11 h-11 flex items-center justify-center rounded-full border-4 border-black shrink-0
                                ${done
                                    ? current
                                        ? "bg-yellow-400 text-black"
                                        : "bg-green-400 text-black"
                                    : darkMode
                                      ? "bg-gray-700 text-gray-400"
                                      : "bg-white text-gray-400"}`}
                                aria-hidden="true"
                            >
                                {done && !current ? <CheckIcon size={20} /> : <step.Icon size={20} />}
                            </span>
                            {!isLast && (
                                <span
                                    className={`w-1 flex-1 min-h-6 ${done ? "bg-black" : darkMode ? "bg-gray-600" : "bg-gray-300"}`}
                                    aria-hidden="true"
                                />
                            )}
                        </div>
                        <div className="pb-6 pt-2 min-w-0">
                            <p className={`font-black ${done ? "" : "opacity-50"}`}>
                                {step.label}
                                {current && <span className="sr-only"> (current status)</span>}
                                {!done && <span className="sr-only"> (not yet)</span>}
                            </p>
                            {order[step.at] && (
                                <p className="text-sm font-bold opacity-70">
                                    {formatDateTime(order[step.at])}
                                </p>
                            )}
                        </div>
                    </li>
                );
            })}
            {exit && (
                <li className="flex gap-4">
                    <span
                        className={`w-11 h-11 flex items-center justify-center rounded-full border-4 border-black shrink-0 ${exit.cls}`}
                        aria-hidden="true"
                    >
                        <exit.Icon size={20} />
                    </span>
                    <div className="pt-1 min-w-0">
                        <p className={`inline-block font-black px-2 py-0.5 rounded border-2 border-black ${exit.cls}`}>
                            {exit.label}
                            <span className="sr-only"> (current status)</span>
                        </p>
                        {exit.at && order[exit.at] && (
                            <p className="text-sm font-bold opacity-70 mt-1">
                                {formatDateTime(order[exit.at])}
                            </p>
                        )}
                    </div>
                </li>
            )}
        </ol>
    );
}

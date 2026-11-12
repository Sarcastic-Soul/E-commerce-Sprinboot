import type { OrderStatus } from "../api/types";

export const ORDER_STATUSES = [
    "PENDING",
    "PAID",
    "SHIPPED",
    "DELIVERED",
    "CANCELLED",
    "REJECTED",
    "EXPIRED",
] as const satisfies readonly OrderStatus[];

/** Keyed by string: older orders may still carry the legacy COMPLETED value. */
export const STATUS_LABELS: Record<string, string> = {
    PENDING: "Awaiting payment",
    PAID: "Paid",
    COMPLETED: "Paid", // legacy value, migrated to PAID by the backend
    SHIPPED: "Shipped",
    DELIVERED: "Delivered",
    CANCELLED: "Cancelled",
    REJECTED: "Payment failed",
    EXPIRED: "Expired",
};

export const STATUS_COLORS: Record<string, string> = {
    PENDING: "bg-yellow-200 text-yellow-900",
    PAID: "bg-green-200 text-green-900",
    COMPLETED: "bg-green-200 text-green-900",
    SHIPPED: "bg-cyan-200 text-cyan-900",
    DELIVERED: "bg-green-500 text-black",
    CANCELLED: "bg-gray-300 text-gray-900",
    REJECTED: "bg-red-200 text-red-900",
    EXPIRED: "bg-gray-200 text-gray-800",
};

/** Admin status changes the backend allows (PUT /admin/orders/{id}/status). */
export interface AdminTransition {
    to: OrderStatus;
    label: string;
}

export const ADMIN_TRANSITIONS: Partial<Record<OrderStatus, AdminTransition[]>> = {
    PAID: [
        { to: "SHIPPED", label: "Mark shipped" },
        { to: "CANCELLED", label: "Cancel" },
    ],
    SHIPPED: [{ to: "DELIVERED", label: "Mark delivered" }],
};

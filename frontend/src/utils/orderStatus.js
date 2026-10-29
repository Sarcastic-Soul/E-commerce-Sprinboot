export const ORDER_STATUSES = [
    "PENDING",
    "PAID",
    "SHIPPED",
    "DELIVERED",
    "CANCELLED",
    "REJECTED",
    "EXPIRED",
];

export const STATUS_LABELS = {
    PENDING: "Awaiting payment",
    PAID: "Paid",
    COMPLETED: "Paid", // legacy value, migrated to PAID by the backend
    SHIPPED: "Shipped",
    DELIVERED: "Delivered",
    CANCELLED: "Cancelled",
    REJECTED: "Payment failed",
    EXPIRED: "Expired",
};

export const STATUS_COLORS = {
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
export const ADMIN_TRANSITIONS = {
    PAID: [
        { to: "SHIPPED", label: "Mark shipped" },
        { to: "CANCELLED", label: "Cancel" },
    ],
    SHIPPED: [{ to: "DELIVERED", label: "Mark delivered" }],
};

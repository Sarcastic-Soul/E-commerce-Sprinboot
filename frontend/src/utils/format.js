export const formatMoney = (value) => `₹${Number(value ?? 0).toFixed(2)}`;

export const formatDate = (iso) =>
    iso
        ? new Date(iso).toLocaleDateString(undefined, {
              day: "numeric",
              month: "short",
              year: "numeric",
          })
        : "";

export const formatDateTime = (iso) =>
    iso
        ? new Date(iso).toLocaleString(undefined, {
              day: "numeric",
              month: "short",
              year: "numeric",
              hour: "numeric",
              minute: "2-digit",
          })
        : "";

/** Pulls the backend's ApiErrorResponse message out of an axios error. */
export const errorMessage = (err, fallback = "Something went wrong") =>
    err?.response?.data?.message || fallback;

/** Downloads a blob response (keeps cookies + CSRF by going through axios). */
export function saveBlob(data, filename) {
    const url = URL.createObjectURL(data);
    const a = document.createElement("a");
    a.href = url;
    a.download = filename;
    document.body.appendChild(a);
    a.click();
    a.remove();
    setTimeout(() => URL.revokeObjectURL(url), 1000);
}

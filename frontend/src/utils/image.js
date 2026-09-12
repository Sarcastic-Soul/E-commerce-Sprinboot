export const PLACEHOLDER = "/placeholder.svg";

/**
 * Cloudinary serves whatever transform is in the URL path. Without one we ship the
 * original upload — often several MB — into a card that renders it at ~400px wide.
 * Non-Cloudinary URLs are returned untouched.
 */
export function productImage(url, width = 400) {
    if (!url) return PLACEHOLDER;
    const src = String(url);
    if (!src.includes("/upload/")) return src;
    if (/\/upload\/[^/]*(f_|q_|w_|c_)/.test(src)) return src; // already transformed
    return src.replace("/upload/", `/upload/f_auto,q_auto,c_limit,w_${width}/`);
}

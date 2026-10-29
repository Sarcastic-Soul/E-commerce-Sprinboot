/**
 * Shared class strings for the brutalist look (thick black borders, hard offset
 * shadows). Pulled from ProductCard / Cart / AdminDashboard so new screens reuse
 * the same system instead of re-typing it slightly differently each time.
 */

export const focusRing =
    "focus:outline-none focus-visible:outline-4 focus-visible:outline-offset-2 focus-visible:outline-rose-500";

export const panel = (darkMode) =>
    `rounded-lg border-4 border-black shadow-[8px_8px_0px_0px_rgba(0,0,0,1)] ${
        darkMode ? "bg-gray-800 text-white" : "bg-white text-black"
    }`;

export const subtle = (darkMode) => (darkMode ? "bg-gray-700" : "bg-gray-100");

export const inputCls = (darkMode) =>
    `w-full min-h-[44px] px-3 py-2 border-4 border-black rounded-lg font-bold ${
        darkMode
            ? "bg-gray-900 text-white placeholder-gray-400"
            : "bg-white text-black placeholder-gray-500"
    } ${focusRing}`;

// Square icon buttons in the black navbar: white tiles with a yellow offset
// shadow, since a black shadow would vanish against the bar.
export const navIcon = `relative h-11 w-11 shrink-0 inline-flex items-center justify-center rounded-lg border-2 border-black bg-white text-black hover:bg-yellow-300 shadow-[3px_3px_0px_0px_#facc15] transition-transform active:translate-x-[2px] active:translate-y-[2px] active:shadow-none ${focusRing}`;

export const labelCls = "block font-black text-sm mb-2";

const pressable = `inline-flex items-center justify-center gap-2 min-h-[44px] px-4 py-2 rounded-lg font-black border-4 border-black cursor-pointer
    shadow-[4px_4px_0px_0px_rgba(0,0,0,1)] transition-transform
    active:translate-x-[2px] active:translate-y-[2px] active:shadow-none
    disabled:opacity-50 disabled:cursor-not-allowed disabled:active:translate-x-0 disabled:active:translate-y-0 ${focusRing}`;

const variants = {
    primary: (d) =>
        d
            ? "bg-yellow-400 hover:bg-yellow-300 text-black"
            : "bg-yellow-400 hover:bg-yellow-500 text-black",
    secondary: (d) =>
        d
            ? "bg-gray-700 hover:bg-gray-600 text-white"
            : "bg-white hover:bg-gray-100 text-black",
    success: (d) =>
        d
            ? "bg-green-600 hover:bg-green-700 text-white"
            : "bg-green-500 hover:bg-green-600 text-white",
    danger: () => "bg-red-500 hover:bg-red-600 text-white",
    ink: (d) =>
        d
            ? "bg-gray-900 hover:bg-gray-700 text-white"
            : "bg-black hover:bg-gray-800 text-white",
};

export const btn = (darkMode, variant = "primary") =>
    `${pressable} ${variants[variant](darkMode)}`;

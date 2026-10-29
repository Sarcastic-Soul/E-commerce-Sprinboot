// src/components/LoadingState.jsx
import { useTheme } from "../context/ThemeContext";

/**
 * Skeletons rather than a spinner: the placeholder occupies the same footprint as
 * the content that replaces it, so the page does not jump on swap and the user can
 * see what is loading.
 *
 * `variant="grid"` mirrors the ProductCard grid; `variant="page"` is a generic block
 * for detail and dashboard views that are not card grids; `variant="list"` is a
 * short stack of rows for sections inside a page.
 */
export default function LoadingState({ variant = "grid", count = 8 }) {
    const { darkMode } = useTheme();

    const surface = darkMode
        ? "bg-gray-800 border-gray-700"
        : "bg-white border-black";
    const bar = darkMode ? "bg-gray-700" : "bg-gray-200";
    const block = darkMode ? "bg-gray-700" : "bg-gray-300";

    if (variant === "list") {
        return (
            <div
                className="animate-pulse space-y-3"
                role="status"
                aria-label="Loading"
            >
                {Array.from({ length: Math.min(count, 5) }).map((_, i) => (
                    <div
                        key={i}
                        className={`h-16 rounded-lg border-4 ${surface}`}
                    />
                ))}
                <span className="sr-only">Loading...</span>
            </div>
        );
    }

    if (variant === "page") {
        return (
            <div
                className="animate-pulse space-y-6"
                role="status"
                aria-label="Loading"
            >
                <div className={`h-10 w-1/3 rounded ${bar}`} />
                <div
                    className={`rounded-lg border-4 border-black p-6 shadow-[8px_8px_0px_0px_rgba(0,0,0,1)] ${surface}`}
                >
                    <div className={`h-64 w-full rounded ${block} mb-6`} />
                    <div className={`h-6 w-2/3 rounded ${bar} mb-3`} />
                    <div className={`h-6 w-1/2 rounded ${bar} mb-3`} />
                    <div className={`h-6 w-1/4 rounded ${bar}`} />
                </div>
                <span className="sr-only">Loading...</span>
            </div>
        );
    }

    return (
        <div
            className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6"
            role="status"
            aria-label="Loading products"
        >
            {Array.from({ length: count }).map((_, i) => (
                <div
                    key={i}
                    className={`rounded-lg overflow-hidden border-4 border-black shadow-[6px_6px_0px_0px_rgba(0,0,0,1)] animate-pulse ${surface}`}
                >
                    <div className={`w-full h-64 ${block}`} />
                    <div className="p-4">
                        <div className={`h-6 w-3/4 rounded ${bar} mb-3`} />
                        <div className={`h-4 w-1/3 rounded ${bar} mb-3`} />
                        <div className={`h-6 w-1/2 rounded ${bar} mb-4`} />
                        <div className="flex space-x-2">
                            <div className={`h-10 flex-1 rounded ${bar}`} />
                            <div className={`h-10 w-14 rounded ${bar}`} />
                        </div>
                    </div>
                </div>
            ))}
            <span className="sr-only">Loading products...</span>
        </div>
    );
}

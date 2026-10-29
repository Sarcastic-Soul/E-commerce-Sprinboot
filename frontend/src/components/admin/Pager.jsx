import { CaretLeftIcon, CaretRightIcon } from "@phosphor-icons/react";
import { useTheme } from "../../context/ThemeContext";
import { btn } from "../../utils/ui.js";

/** Prev / "Page x of y" / Next, matching the home page pagination. */
export default function Pager({ page, totalPages, onChange }) {
    const { darkMode } = useTheme();
    if (totalPages <= 1) return null;
    return (
        <nav className="flex justify-center items-center gap-4 mt-6" aria-label="Pagination">
            <button
                type="button"
                onClick={() => onChange(page - 1)}
                disabled={page === 0}
                aria-label="Previous page"
                className={btn(darkMode, "primary")}
            >
                <CaretLeftIcon size={20} />
            </button>
            <span className="font-black">
                Page {page + 1} of {totalPages}
            </span>
            <button
                type="button"
                onClick={() => onChange(page + 1)}
                disabled={page >= totalPages - 1}
                aria-label="Next page"
                className={btn(darkMode, "primary")}
            >
                <CaretRightIcon size={20} />
            </button>
        </nav>
    );
}

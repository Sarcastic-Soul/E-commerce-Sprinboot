import { ArrowLeftIcon } from "@phosphor-icons/react";
import { useNavigate } from "react-router-dom";
import { useTheme } from "../context/ThemeContext";
import { focusRing } from "../utils/ui.js";

/** Back button + big underlined title, the header used across the app's pages. */
export default function PageHeader({ title, backTo = "/", children }) {
    const { darkMode } = useTheme();
    const navigate = useNavigate();

    return (
        <div className="flex flex-wrap items-center gap-4 mb-8">
            <button
                type="button"
                onClick={() => (backTo === -1 ? navigate(-1) : navigate(backTo))}
                aria-label="Go back"
                className={`w-11 h-11 flex items-center justify-center rounded-full border-2 border-black transition-transform active:translate-x-[2px] active:translate-y-[2px] shadow-[4px_4px_0px_0px_rgba(0,0,0,1)] active:shadow-none ${focusRing}
                ${darkMode ? "bg-gray-800 text-white" : "bg-white text-black"}`}
            >
                <ArrowLeftIcon size={24} />
            </button>
            <h1 className="text-3xl sm:text-4xl font-black border-b-4 border-black pb-2 inline-block break-words">
                {title}
            </h1>
            {children && <div className="ml-auto flex flex-wrap gap-3">{children}</div>}
        </div>
    );
}

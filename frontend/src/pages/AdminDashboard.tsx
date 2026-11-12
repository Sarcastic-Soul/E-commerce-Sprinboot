import { useRef } from "react";
import type { KeyboardEvent } from "react";
import { useSearchParams } from "react-router";
import { PulseIcon } from "@phosphor-icons/react";
import { useTheme } from "../context/ThemeContext";
import PageHeader from "../components/PageHeader";
import StatsOverview from "../components/admin/StatsOverview";
import AdminOrders from "../components/admin/AdminOrders";
import LowStockList from "../components/admin/LowStockList";
import AdminCoupons from "../components/admin/AdminCoupons";
import ExportButtons from "../components/admin/ExportButtons";
import { focusRing } from "../utils/ui";

const TABS = [
    { key: "overview", label: "Overview", Panel: StatsOverview },
    { key: "orders", label: "Orders", Panel: AdminOrders },
    { key: "stock", label: "Low stock", Panel: LowStockList },
    { key: "coupons", label: "Coupons", Panel: AdminCoupons },
];

export default function AdminDashboard() {
    const { darkMode } = useTheme();
    const [params, setParams] = useSearchParams();
    const tabRefs = useRef<(HTMLButtonElement | null)[]>([]);

    // Tab lives in the URL so a refresh or a back button from an order page
    // lands on the same section.
    const activeIndex = Math.max(
        0,
        TABS.findIndex((t) => t.key === params.get("tab")),
    );
    const activeTab = TABS[activeIndex];
    // activeIndex is always inside TABS; this only narrows the type.
    if (!activeTab) return null;
    const { key: activeKey, Panel } = activeTab;

    const select = (i: number, focus = false) => {
        const tab = TABS[i];
        if (!tab) return;
        setParams(i === 0 ? {} : { tab: tab.key }, { replace: true });
        if (focus) tabRefs.current[i]?.focus();
    };

    const onKeyDown = (e: KeyboardEvent<HTMLDivElement>) => {
        const last = TABS.length - 1;
        let next: number | null = null;
        if (e.key === "ArrowRight") next = activeIndex === last ? 0 : activeIndex + 1;
        else if (e.key === "ArrowLeft") next = activeIndex === 0 ? last : activeIndex - 1;
        else if (e.key === "Home") next = 0;
        else if (e.key === "End") next = last;
        if (next === null) return;
        e.preventDefault();
        select(next, true);
    };

    return (
        <div className="max-w-7xl mx-auto mt-8 pb-12">
            <PageHeader
                title={
                    <span className="inline-flex items-center gap-3">
                        <PulseIcon size={40} aria-hidden="true" /> ADMIN DASHBOARD
                    </span>
                }
            >
                <ExportButtons />
            </PageHeader>

            {/* The tabs inside take focus (roving tabindex); the tablist itself should not. */}
            {/* oxlint-disable-next-line jsx-a11y/interactive-supports-focus */}
            <div
                role="tablist"
                aria-label="Dashboard sections"
                onKeyDown={onKeyDown}
                className="flex flex-wrap gap-3 mb-8"
            >
                {TABS.map((t, i) => {
                    const selected = i === activeIndex;
                    return (
                        <button
                            key={t.key}
                            ref={(el) => {
                                tabRefs.current[i] = el;
                            }}
                            id={`tab-${t.key}`}
                            role="tab"
                            type="button"
                            aria-selected={selected}
                            aria-controls={`panel-${t.key}`}
                            tabIndex={selected ? 0 : -1}
                            onClick={() => select(i)}
                            className={`min-h-[44px] px-4 py-2 rounded-lg font-black border-4 border-black transition-transform ${focusRing}
                            ${selected
                                ? `${darkMode ? "bg-yellow-400 text-black" : "bg-black text-white"} translate-x-[2px] translate-y-[2px]`
                                : `${darkMode ? "bg-gray-800 text-white hover:bg-gray-700" : "bg-white text-black hover:bg-yellow-100"} shadow-[4px_4px_0px_0px_rgba(0,0,0,1)]`}`}
                        >
                            {t.label}
                        </button>
                    );
                })}
            </div>

            <div
                role="tabpanel"
                id={`panel-${activeKey}`}
                aria-labelledby={`tab-${activeKey}`}
            >
                <Panel />
            </div>
        </div>
    );
}

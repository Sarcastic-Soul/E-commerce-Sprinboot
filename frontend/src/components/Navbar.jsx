import {
    ShoppingCartIcon as ShoppingCart,
    MoonIcon as Moon,
    SunIcon as Sun,
    PlusIcon as Plus,
    SignOutIcon as LogOut,
    FunnelIcon as Filter,
    MagnifyingGlassIcon as Search,
    XIcon as X,
    ClockCounterClockwiseIcon as History,
    SquaresFourIcon as LayoutDashboard,
    HeartIcon as Heart,
    UserCircleIcon,
} from "@phosphor-icons/react";
import { Link, useNavigate } from "react-router-dom";
import { useState, useEffect, useRef, useCallback } from "react"; // Added useRef
import { useAuth } from "../context/AuthContext";
import api from "../api/axios";
import { useTheme } from "../context/ThemeContext";
import { useCart } from "../context/CartContext.jsx";
import NotificationBell from "./NotificationBell";
import { navIcon, focusRing } from "../utils/ui";
import { PRODUCT_CATEGORIES } from "../constants/categories.js";

export function Navbar({
    setShowCart,
    onSearchResults,
    resetSignal = 0,
    sort = "newest",
}) {
    const [searchTerm, setSearchTerm] = useState("");
    const [debouncedTerm, setDebouncedTerm] = useState("");

    // Filter States
    const [showFilters, setShowFilters] = useState(false);
    const [category, setCategory] = useState("");
    const [minPrice, setMinPrice] = useState("");
    const [maxPrice, setMaxPrice] = useState("");
    const [available, setAvailable] = useState("");

    const { user, logout } = useAuth();
    const navigate = useNavigate();
    const { darkMode, toggleDarkMode } = useTheme();
    const { cartItemCount } = useCart();

    // Track initial mount so we don't accidentally override the pagination on load
    const isInitialMount = useRef(true);

    useEffect(() => {
        const handler = setTimeout(() => setDebouncedTerm(searchTerm), 500);
        return () => clearTimeout(handler);
    }, [searchTerm]);

    // Unified API Call combining Search + Filters
    useEffect(() => {
        const fetchData = async () => {
            const hasFilters =
                debouncedTerm.trim() ||
                category ||
                minPrice ||
                maxPrice ||
                available !== "";

            // If it's the very first page load and no filters are active, do nothing!
            // Let App.jsx handle the default paginated fetch.
            if (isInitialMount.current) {
                isInitialMount.current = false;
                if (!hasFilters) return;
            }

            // If the user cleared all filters, tell App.jsx to reset back to pagination
            if (!hasFilters) {
                onSearchResults({ isReset: true });
                return;
            }

            try {
                const params = new URLSearchParams();
                if (debouncedTerm.trim())
                    params.append("keyword", debouncedTerm.trim());
                if (category) params.append("category", category);
                if (minPrice) params.append("minPrice", minPrice);
                if (maxPrice) params.append("maxPrice", maxPrice);
                if (available !== "") params.append("available", available);
                params.append("sort", sort);

                const { data } = await api.get(
                    `/products/filter?${params.toString()}`,
                );
                onSearchResults(data);
            } catch (error) {
                console.error("Search/Filter error:", error);
            }
        };
        fetchData();
    }, [
        debouncedTerm,
        category,
        minPrice,
        maxPrice,
        available,
        sort,
        onSearchResults,
    ]);

    const handleResetFilters = useCallback(() => {
        setSearchTerm(""); // Added search reset
        setDebouncedTerm("");
        setCategory("");
        setMinPrice("");
        setMaxPrice("");
        setAvailable("");
    }, []);

    // The empty-state "Clear filters" button lives in ProductGrid, which cannot reach
    // this component's filter state directly — App bumps a counter and we clear here,
    // so the inputs and the query never disagree about what is filtered.
    useEffect(() => {
        if (resetSignal === 0) return;
        handleResetFilters();
    }, [resetSignal, handleResetFilters]);

    const handleLogout = async () => {
        await logout();
        navigate("/");
    };

    return (
        <div className="flex flex-col">
            {/* Main Navbar */}
            <nav
                className="bg-black text-white py-4 px-6 border-b-4 border-yellow-400 z-20 relative"
            >
                <div className="container mx-auto flex flex-wrap gap-4 justify-between items-center">
                    <h1 className="text-3xl font-black tracking-tighter">
                        <Link to="/">BRUT SHOP</Link>
                    </h1>

                    <div className="flex items-center gap-2">
                        <div className="relative">
                            <label htmlFor="nav-search" className="sr-only">
                                Search products
                            </label>
                            <Search
                                className={`absolute left-3 top-1/2 -translate-y-1/2 ${darkMode ? "text-white" : "text-black"}`}
                                size={18}
                            />
                            <input
                                id="nav-search"
                                type="search"
                                placeholder="Search products..."
                                value={searchTerm}
                                onChange={(e) => setSearchTerm(e.target.value)}
                                className={`pl-10 pr-4 py-2 rounded-lg font-bold border-4 border-black focus:outline-none focus-visible:outline-4 focus-visible:outline-offset-2 focus-visible:outline-rose-500 w-52 sm:w-64 min-h-[44px]
                                ${darkMode ? "bg-gray-900 text-white placeholder-gray-400" : "bg-white text-black placeholder-gray-500"}`}
                            />
                        </div>

                        <button
                            onClick={() => setShowFilters(!showFilters)}
                            className={`${navIcon} ${showFilters ? "!bg-yellow-400" : ""}`}
                            title="Toggle Filters"
                            aria-label="Filters"
                            aria-expanded={showFilters}
                        >
                            <Filter size={20} />
                        </button>
                    </div>

                    <div className="flex flex-wrap items-center gap-2 sm:gap-4">
                        {user?.role === "ADMIN" && (
                            <>
                                <Link
                                    to="/admin/dashboard"
                                    className={`flex items-center gap-2 min-h-[44px] px-3 py-2 rounded-lg bg-white text-black font-bold hover:bg-yellow-300 border-2 border-black shadow-[3px_3px_0px_0px_#facc15] ${focusRing}`}
                                >
                                    <LayoutDashboard size={20} /> Dashboard
                                </Link>
                                <Link
                                    to="/add-product"
                                    className={`flex items-center gap-2 min-h-[44px] px-3 py-2 rounded-lg bg-yellow-400 text-black font-bold hover:bg-yellow-300 border-2 border-black ${focusRing}`}
                                >
                                    <Plus size={20} /> Add Product
                                </Link>
                            </>
                        )}

                        <button
                            onClick={toggleDarkMode}
                            aria-label={darkMode ? "Switch to light mode" : "Switch to dark mode"}
                            className={navIcon}
                        >
                            {darkMode ? <Sun size={20} /> : <Moon size={20} />}
                        </button>

                        {user && (
                            <>
                                <NotificationBell />
                                <Link
                                    to="/wishlist"
                                    className={navIcon}
                                    title="My Wishlist"
                                    aria-label="My wishlist"
                                >
                                    <Heart size={20} />
                                </Link>

                                <Link
                                    to="/orders"
                                    className={navIcon}
                                    title="Order History"
                                    aria-label="Order history"
                                >
                                    <History size={20} />
                                </Link>
                            </>
                        )}

                        <button
                            onClick={() =>
                                user ? setShowCart(true) : navigate("/login")
                            }
                            aria-label={`Shopping cart, ${cartItemCount} items`}
                            className={`${navIcon} !bg-yellow-400 hover:!bg-yellow-300`}
                            title="Shopping Cart"
                        >
                            <ShoppingCart size={20} />
                            {cartItemCount > 0 && (
                                <span className="absolute -top-2 -right-2 bg-rose-500 text-white text-xs font-black min-w-6 h-6 px-1 flex items-center justify-center rounded-full border-2 border-black">
                                    {cartItemCount}
                                </span>
                            )}
                        </button>

                        {!user ? (
                            <Link
                                to="/login"
                                className={`inline-flex items-center min-h-[44px] px-4 py-2 bg-yellow-400 text-black font-black rounded-lg border-2 border-black hover:bg-yellow-300 ${focusRing}`}
                            >
                                Login
                            </Link>
                        ) : (
                            <div className="flex items-center gap-2">
                                <Link
                                    to="/account"
                                    title="My account"
                                    aria-label="My account"
                                    className={`flex items-center gap-2 min-h-[44px] p-2 rounded-lg font-bold text-yellow-400 hover:bg-gray-800 transition-colors ${focusRing}`}
                                >
                                    <UserCircleIcon size={22} />
                                    <span className="hidden md:inline">
                                        Hi, {user.username}
                                    </span>
                                </Link>
                                <button
                                    onClick={handleLogout}
                                    title="Logout"
                                    aria-label="Log out"
                                    className={`flex items-center gap-1 min-h-[44px] px-3 py-2 rounded-lg font-bold border-2 border-white text-white hover:bg-white hover:text-black transition-colors ${focusRing}`}
                                >
                                    <LogOut size={18} />{" "}
                                    <span className="hidden md:inline">
                                        Logout
                                    </span>
                                </button>
                            </div>
                        )}
                    </div>
                </div>
            </nav>

            {/* Collapsible Filter Drawer */}
            <div
                className={`w-full overflow-hidden transition-all duration-300 ease-in-out border-black ${showFilters ? "max-h-[500px] border-b-4" : "max-h-0 border-b-0"} ${darkMode ? "bg-gray-700" : "bg-yellow-100"}`}
            >
                <div className="container mx-auto p-6 flex flex-col items-center gap-6">
                    <div className="flex flex-wrap justify-center items-end gap-6 w-full max-w-6xl">
                        {/* Category */}
                        <div className="flex flex-col w-full sm:w-48">
                            <label htmlFor="filter-category" className="block font-black text-sm mb-2 text-center">
                                CATEGORY
                            </label>
                            <select
                                id="filter-category"
                                value={category}
                                onChange={(e) => setCategory(e.target.value)}
                                className={`h-12 px-3 border-4 border-black font-bold outline-none focus-visible:outline-4 focus-visible:outline-offset-2 focus-visible:outline-rose-500 cursor-pointer ${darkMode ? "bg-gray-900 text-white" : "bg-white text-black"}`}
                            >
                                <option value="">ALL</option>
                                {PRODUCT_CATEGORIES.map((c) => (
                                    <option key={c.value} value={c.value}>
                                        {c.label.toUpperCase()}
                                    </option>
                                ))}
                            </select>
                        </div>

                        {/* Price Range */}
                        <div className="flex flex-col w-full sm:w-56">
                            <label className="block font-black text-sm mb-2 text-center">
                                PRICE RANGE
                            </label>
                            <div className="flex gap-2">
                                <input
                                    type="number"
                                    min="0"
                                    placeholder="Min ₹"
                                    aria-label="Minimum price"
                                    value={minPrice}
                                    onChange={(e) =>
                                        setMinPrice(e.target.value)
                                    }
                                    onKeyDown={(e) => {
                                        if (e.key === "-") e.preventDefault();
                                    }}
                                    className={`h-12 w-1/2 px-3 border-4 border-black font-bold outline-none focus-visible:outline-4 focus-visible:outline-offset-2 focus-visible:outline-rose-500 ${darkMode ? "bg-gray-900 text-white" : "bg-white text-black"}`}
                                />
                                <input
                                    type="number"
                                    min="0"
                                    placeholder="Max ₹"
                                    aria-label="Maximum price"
                                    value={maxPrice}
                                    onChange={(e) =>
                                        setMaxPrice(e.target.value)
                                    }
                                    onKeyDown={(e) => {
                                        if (e.key === "-") e.preventDefault();
                                    }}
                                    className={`h-12 w-1/2 px-3 border-4 border-black font-bold outline-none focus-visible:outline-4 focus-visible:outline-offset-2 focus-visible:outline-rose-500 ${darkMode ? "bg-gray-900 text-white" : "bg-white text-black"}`}
                                />
                            </div>
                        </div>

                        {/* Availability */}
                        <div className="flex flex-col w-full sm:w-40">
                            <label htmlFor="filter-available" className="block font-black text-sm mb-2 text-center">
                                AVAILABILITY
                            </label>
                            <select
                                id="filter-available"
                                value={available}
                                onChange={(e) => setAvailable(e.target.value)}
                                className={`h-12 px-3 border-4 border-black font-bold outline-none focus-visible:outline-4 focus-visible:outline-offset-2 focus-visible:outline-rose-500 cursor-pointer ${darkMode ? "bg-gray-900 text-white" : "bg-white text-black"}`}
                            >
                                <option value="">ALL</option>
                                <option value="true">IN STOCK</option>
                                <option value="false">OUT OF STOCK</option>
                            </select>
                        </div>

                        {/* Reset Button */}
                        <button
                            onClick={handleResetFilters}
                            title="Reset Filters"
                            aria-label="Reset filters"
                            className="h-12 w-12 flex items-center justify-center bg-red-500 text-white rounded-lg hover:bg-red-600 transition-transform active:scale-95 shadow-md flex-shrink-0"
                        >
                            <X size={28} />
                        </button>
                    </div>
                </div>
            </div>
        </div>
    );
}

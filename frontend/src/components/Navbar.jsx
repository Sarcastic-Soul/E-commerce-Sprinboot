import {
    ShoppingCart,
    Moon,
    Sun,
    Plus,
    LogOut,
    Filter,
    Search,
    X,
    History,
    LayoutDashboard,
    Heart,
} from "lucide-react";
import { Link } from "react-router-dom";
import { useState, useEffect, useRef } from "react"; // Added useRef
import { useAuth } from "../context/AuthContext";
import api from "../api/axios";
import { useTheme } from "../context/ThemeContext";
import { useCart } from "../context/CartContext.jsx";
import NotificationBell from "./NotificationBell";

export function Navbar({ setShowCart, onSearchResults }) {
    const [searchTerm, setSearchTerm] = useState("");
    const [debouncedTerm, setDebouncedTerm] = useState("");

    // Filter States
    const [showFilters, setShowFilters] = useState(false);
    const [category, setCategory] = useState("");
    const [minPrice, setMinPrice] = useState("");
    const [maxPrice, setMaxPrice] = useState("");
    const [sort, setSort] = useState("asc");
    const [available, setAvailable] = useState("");

    const { user, logout } = useAuth();
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
                available !== "" ||
                sort !== "asc";

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

    const handleResetFilters = () => {
        setSearchTerm(""); // Added search reset
        setDebouncedTerm("");
        setCategory("");
        setMinPrice("");
        setMaxPrice("");
        setSort("asc");
        setAvailable("");
    };

    return (
        <div className="flex flex-col">
            {/* Main Navbar */}
            <nav
                className={`${darkMode ? "bg-gray-800 text-white" : "bg-black text-white"} py-4 px-6 shadow-lg z-20 relative`}
            >
                <div className="container mx-auto flex flex-wrap gap-4 justify-between items-center">
                    <h1 className="text-3xl font-black tracking-tighter">
                        <Link to="/">BRUT SHOP</Link>
                    </h1>

                    <div className="flex items-center gap-2">
                        <div className="relative">
                            <Search
                                className={`absolute left-3 top-1/2 -translate-y-1/2 ${darkMode ? "text-white" : "text-black"}`}
                                size={18}
                            />
                            <input
                                type="text"
                                placeholder="Search products..."
                                value={searchTerm}
                                onChange={(e) => setSearchTerm(e.target.value)}
                                className={`pl-10 pr-4 py-2 rounded-lg font-bold border-4 border-black focus:outline-none w-64
                                ${darkMode ? "bg-gray-900 text-white placeholder-gray-400" : "bg-white text-black placeholder-gray-500"}`}
                            />
                        </div>

                        <button
                            onClick={() => setShowFilters(!showFilters)}
                            className={`p-2 rounded-lg border-4 border-black font-bold flex items-center gap-2 transition-transform active:translate-y-1 ${darkMode ? "bg-gray-700" : "bg-yellow-400 text-black"}`}
                            title="Toggle Filters"
                        >
                            <Filter size={20} />
                        </button>
                    </div>

                    <div className="flex items-center gap-4">
                        {user?.role === "ADMIN" && (
                            <>
                                <Link
                                    to="/admin/dashboard"
                                    className="flex items-center gap-2 p-2 rounded-lg bg-cyan-400 text-black font-semibold hover:bg-cyan-300 transition-all duration-300 border-2 border-black"
                                >
                                    <LayoutDashboard size={20} /> Dashboard
                                </Link>
                                <Link
                                    to="/add-product"
                                    className="flex items-center gap-2 p-2 rounded-lg bg-yellow-400 text-black font-semibold hover:bg-yellow-300 transition-all duration-300 border-2 border-black"
                                >
                                    <Plus size={20} /> Add Product
                                </Link>
                            </>
                        )}

                        <button
                            onClick={toggleDarkMode}
                            className={`p-2 rounded-lg ${darkMode ? "bg-gray-700" : "bg-gray-900"} hover:bg-opacity-80 transition-all duration-300`}
                        >
                            {darkMode ? <Sun size={20} /> : <Moon size={20} />}
                        </button>

                        {user && (
                            <>
                                <NotificationBell />
                                <Link
                                    to="/wishlist"
                                    className="p-2 rounded-lg border-2 border-red-500 bg-red-500 hover:bg-red-600 transition-all duration-300"
                                    title="My Wishlist"
                                >
                                    <Heart size={20} />
                                </Link>

                                <Link
                                    to="/orders"
                                    className="p-2 rounded-lg bg-indigo-500 text-white hover:bg-indigo-600 transition-all duration-300"
                                    title="Order History"
                                >
                                    <History size={20} />
                                </Link>
                            </>
                        )}

                        <button
                            onClick={() => setShowCart(true)}
                            className="relative p-2 rounded-lg bg-rose-600 hover:bg-rose-700 transition-all duration-300"
                            title="Shopping Cart"
                        >
                            <ShoppingCart size={20} />
                            {cartItemCount > 0 && (
                                <span className="absolute -top-2 -right-2 bg-yellow-400 text-black text-xs font-bold px-2 py-1 rounded-full border-2 border-black">
                                    {cartItemCount}
                                </span>
                            )}
                        </button>

                        {!user ? (
                            <Link
                                to="/login"
                                className="px-4 py-2 bg-blue-600 text-white font-semibold rounded-lg border-2 border-blue-700 hover:bg-blue-700 transition"
                            >
                                Login
                            </Link>
                        ) : (
                            <div className="flex items-center gap-3">
                                <span className="font-bold text-yellow-400 hidden md:block">
                                    Hi, {user.username}
                                </span>
                                <button
                                    onClick={logout}
                                    title="Logout"
                                    className="flex items-center gap-1 px-3 py-2 rounded-lg font-semibold border-2 border-red-600 bg-red-500 hover:bg-red-600 transition"
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
                            <label className="block font-black text-sm mb-2 text-center">
                                CATEGORY
                            </label>
                            <select
                                value={category}
                                onChange={(e) => setCategory(e.target.value)}
                                className={`h-12 px-3 border-4 border-black font-bold outline-none cursor-pointer ${darkMode ? "bg-gray-900 text-white" : "bg-white text-black"}`}
                            >
                                <option value="">ALL</option>
                                <option value="ELECTRONICS">ELECTRONICS</option>
                                <option value="FASHION">FASHION</option>
                                <option value="HOME_KITCHEN">
                                    HOME & KITCHEN
                                </option>
                                <option value="TOYS_GAMES">TOYS & GAMES</option>
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
                                    placeholder="Min $"
                                    value={minPrice}
                                    onChange={(e) =>
                                        setMinPrice(e.target.value)
                                    }
                                    onKeyDown={(e) => {
                                        if (e.key === "-") e.preventDefault();
                                    }}
                                    className={`h-12 w-1/2 px-3 border-4 border-black font-bold outline-none ${darkMode ? "bg-gray-900 text-white" : "bg-white text-black"}`}
                                />
                                <input
                                    type="number"
                                    min="0"
                                    placeholder="Max $"
                                    value={maxPrice}
                                    onChange={(e) =>
                                        setMaxPrice(e.target.value)
                                    }
                                    onKeyDown={(e) => {
                                        if (e.key === "-") e.preventDefault();
                                    }}
                                    className={`h-12 w-1/2 px-3 border-4 border-black font-bold outline-none ${darkMode ? "bg-gray-900 text-white" : "bg-white text-black"}`}
                                />
                            </div>
                        </div>

                        {/* Availability */}
                        <div className="flex flex-col w-full sm:w-40">
                            <label className="block font-black text-sm mb-2 text-center">
                                AVAILABILITY
                            </label>
                            <select
                                value={available}
                                onChange={(e) => setAvailable(e.target.value)}
                                className={`h-12 px-3 border-4 border-black font-bold outline-none cursor-pointer ${darkMode ? "bg-gray-900 text-white" : "bg-white text-black"}`}
                            >
                                <option value="">ALL</option>
                                <option value="true">IN STOCK</option>
                                <option value="false">OUT OF STOCK</option>
                            </select>
                        </div>

                        {/* Sort */}
                        <div className="flex flex-col w-full sm:w-48">
                            <label className="block font-black text-sm mb-2 text-center">
                                SORT BY
                            </label>
                            <select
                                value={sort}
                                onChange={(e) => setSort(e.target.value)}
                                className={`h-12 px-3 border-4 border-black font-bold outline-none cursor-pointer ${darkMode ? "bg-gray-900 text-white" : "bg-white text-black"}`}
                            >
                                <option value="asc">PRICE: LOW TO HIGH</option>
                                <option value="desc">PRICE: HIGH TO LOW</option>
                            </select>
                        </div>

                        {/* Reset Button */}
                        <button
                            onClick={handleResetFilters}
                            title="Reset Filters"
                            className="h-12 w-12 flex items-center justify-center bg-red-500 text-white rounded-lg hover:bg-red-600 transition-transform active:scale-95 shadow-md flex-shrink-0"
                        >
                            <X size={28} strokeWidth={3} />
                        </button>
                    </div>
                </div>
            </div>
        </div>
    );
}

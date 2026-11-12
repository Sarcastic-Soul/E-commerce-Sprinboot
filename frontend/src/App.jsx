// src/App.jsx
import { BrowserRouter as Router, Routes, Route } from "react-router";
import { useState, useCallback, lazy, Suspense } from "react";
import { keepPreviousData, useQuery } from "@tanstack/react-query";
import {
    CaretLeftIcon as ChevronLeft,
    CaretRightIcon as ChevronRight,
    IconContext,
} from "@phosphor-icons/react";
import { Navbar } from "./components/Navbar";
import ProductGrid from "./components/ProductGrid";
import ProductDetailPage from "./pages/ProductDetailPage";
import LoadingState from "./components/LoadingState";
import ErrorState from "./components/ErrorState";
import Cart from "./components/Cart";
import LoginPage from "./pages/LoginPage";
import SignupPage from "./pages/SignupPage";
import NotFound from "./pages/NotFound";
import ProtectedRoute from "./routes/ProtectedRoute";
import ErrorBoundary from "./components/ErrorBoundary";

import { AuthProvider } from "./context/AuthContext";
import { ThemeProvider, useTheme } from "./context/ThemeContext";
import ToastProvider from "./components/ToastProvider";
import api from "./api/axios";
import { CartProvider } from "./context/CartContext.jsx";
import OrderHistoryPage from "./pages/OrderHistoryPage.jsx";
import WishlistPage from "./pages/WishlistPage.jsx";

// Admin-only routes. recharts (~360 kB) is pulled in by AdminDashboard, so keeping
// these out of the initial bundle halves what every visitor downloads.
const AdminDashboard = lazy(() => import("./pages/AdminDashboard.jsx"));
const AddProduct = lazy(() => import("./pages/AddProductPage"));
const UpdateProduct = lazy(() => import("./pages/UpdateProductPage"));
const CheckoutPage = lazy(() => import("./pages/CheckoutPage.jsx"));
const OrderDetailPage = lazy(() => import("./pages/OrderDetailPage.jsx"));
const AccountPage = lazy(() => import("./pages/AccountPage.jsx"));

const SORT_OPTIONS = [
    { value: "newest", label: "Newest" },
    { value: "price_asc", label: "Price: low to high" },
    { value: "price_desc", label: "Price: high to low" },
    { value: "rating", label: "Top rated" },
    { value: "name", label: "Name (A-Z)" },
];

// One weight across the app; "bold" matches the thick-border brutalist style.
const ICON_DEFAULTS = { weight: "bold" };

const lazyPage = (node) => (
    <Suspense fallback={<LoadingState variant="page" />}>{node}</Suspense>
);

function InnerApp() {
    const [showCart, setShowCart] = useState(false);

    const [currentPage, setCurrentPage] = useState(0);
    const [isFiltering, setIsFiltering] = useState(false);
    // Results pushed up by the Navbar search/filters; only used while filtering.
    const [searchResult, setSearchResult] = useState({ items: [], totalPages: 1 });
    const [filterResetSignal, setFilterResetSignal] = useState(0);
    const [sort, setSort] = useState("newest");

    const { darkMode } = useTheme();

    // Default paginated catalog. Paused while the Navbar filters own the grid;
    // keepPreviousData stops the grid flashing empty between pages.
    const catalog = useQuery({
        queryKey: ["products", "list", currentPage, sort],
        queryFn: async () => {
            const { data } = await api.get("/products", {
                params: { page: currentPage, size: 12, sort },
            });
            // Spring serializes Page VIA_DTO (see ECommerceApplication): items in
            // `content`, totals nested under `page`.
            return {
                items: data.content || data,
                totalPages: data.page?.totalPages || 1,
            };
        },
        enabled: !isFiltering,
        placeholderData: keepPreviousData,
    });

    const products = isFiltering ? searchResult.items : (catalog.data?.items ?? []);
    const totalPages = isFiltering ? searchResult.totalPages : (catalog.data?.totalPages ?? 0);
    const loading = !isFiltering && catalog.isPending;
    const error =
        !isFiltering && catalog.isError
            ? "Failed to fetch products. Please try again later."
            : null;

    const handleSortChange = (e) => {
        setSort(e.target.value);
        setCurrentPage(0);
    };

    // Handle data coming from the Navbar (Search & Filters)
    const clearFilters = useCallback(
        () => setFilterResetSignal((n) => n + 1),
        [],
    );

    const handleSearchResults = useCallback((data) => {
        if (data?.isReset) {
            setIsFiltering(false);
            setCurrentPage(0);
            return;
        }

        setIsFiltering(true);

        if (data?.content) {
            setSearchResult({
                items: data.content,
                totalPages: data.page?.totalPages || 1,
            });
        } else {
            setSearchResult({ items: Array.isArray(data) ? data : [], totalPages: 1 });
        }
    }, []);

    return (
        <div
            className={`min-h-screen transition-colors duration-300 ${
                darkMode
                    ? "bg-gray-900 text-white"
                    : "bg-yellow-50 text-gray-900"
            }`}
        >
            <Navbar
                setShowCart={setShowCart}
                onSearchResults={handleSearchResults}
                resetSignal={filterResetSignal}
                sort={sort}
            />

            <main className="container mx-auto px-4 py-8">
                <Routes>
                    <Route
                        path="/"
                        element={
                            <div className="flex flex-col pb-8">
                                <div className="flex flex-wrap items-center justify-end gap-3 mb-6">
                                    <label
                                        htmlFor="home-sort"
                                        className="font-black text-sm"
                                    >
                                        SORT BY
                                    </label>
                                    <select
                                        id="home-sort"
                                        value={sort}
                                        onChange={handleSortChange}
                                        className={`min-h-[44px] px-3 border-4 border-black rounded-lg font-bold cursor-pointer shadow-[4px_4px_0px_0px_rgba(0,0,0,1)] focus:outline-none focus-visible:outline-4 focus-visible:outline-offset-2 focus-visible:outline-rose-500 ${darkMode ? "bg-gray-800 text-white" : "bg-white text-black"}`}
                                    >
                                        {SORT_OPTIONS.map((o) => (
                                            <option key={o.value} value={o.value}>
                                                {o.label}
                                            </option>
                                        ))}
                                    </select>
                                </div>
                                {loading ? (
                                    <LoadingState />
                                ) : error ? (
                                    <ErrorState
                                        error={error}
                                        onRetry={() => catalog.refetch()}
                                    />
                                ) : (
                                    <>
                                        <ProductGrid
                                            products={products}
                                            onClearFilters={
                                                isFiltering
                                                    ? clearFilters
                                                    : undefined
                                            }
                                        />

                                        {/* Neo-brutalist Pagination Controls */}
                                        {totalPages > 1 && !isFiltering && (
                                            <div className="flex justify-center items-center gap-4 mt-12 mb-4">
                                                <button
                                                    onClick={() =>
                                                        setCurrentPage((p) =>
                                                            Math.max(0, p - 1),
                                                        )
                                                    }
                                                    disabled={currentPage === 0}
                                                    aria-label="Previous page"
                                                    className={`p-2 min-w-[44px] min-h-[44px] flex items-center justify-center border-4 border-black rounded-lg shadow-[4px_4px_0px_0px_rgba(0,0,0,1)] transition-transform active:translate-x-[2px] active:translate-y-[2px] active:shadow-none disabled:opacity-50 disabled:cursor-not-allowed
                                                    bg-yellow-400 hover:bg-yellow-500 text-black`}
                                                >
                                                    <ChevronLeft size={24} />
                                                </button>

                                                <span className="font-black text-xl border-4 border-black px-4 py-1 rounded-lg bg-white text-black shadow-[4px_4px_0px_0px_rgba(0,0,0,1)]">
                                                    Page {currentPage + 1} of{" "}
                                                    {totalPages}
                                                </span>

                                                <button
                                                    onClick={() =>
                                                        setCurrentPage((p) =>
                                                            Math.min(
                                                                totalPages - 1,
                                                                p + 1,
                                                            ),
                                                        )
                                                    }
                                                    disabled={
                                                        currentPage ===
                                                        totalPages - 1
                                                    }
                                                    aria-label="Next page"
                                                    className={`p-2 min-w-[44px] min-h-[44px] flex items-center justify-center border-4 border-black rounded-lg shadow-[4px_4px_0px_0px_rgba(0,0,0,1)] transition-transform active:translate-x-[2px] active:translate-y-[2px] active:shadow-none disabled:opacity-50 disabled:cursor-not-allowed
                                                    bg-yellow-400 hover:bg-yellow-500 text-black`}
                                                >
                                                    <ChevronRight size={24} />
                                                </button>
                                            </div>
                                        )}
                                    </>
                                )}
                            </div>
                        }
                    />
                    <Route
                        path="/product/:id"
                        element={<ProductDetailPage />}
                    />
                    <Route
                        path="/add-product"
                        element={
                            <ProtectedRoute role="ADMIN">
                                <Suspense fallback={<LoadingState variant="page" />}>
                                    <AddProduct />
                                </Suspense>
                            </ProtectedRoute>
                        }
                    />
                    <Route
                        path="/update-product/:id"
                        element={
                            <ProtectedRoute role="ADMIN">
                                <Suspense fallback={<LoadingState variant="page" />}>
                                    <UpdateProduct />
                                </Suspense>
                            </ProtectedRoute>
                        }
                    />
                    <Route
                        path="/orders"
                        element={
                            <ProtectedRoute>
                                <OrderHistoryPage />
                            </ProtectedRoute>
                        }
                    />
                    <Route
                        path="/orders/:id"
                        element={
                            <ProtectedRoute>
                                {lazyPage(<OrderDetailPage />)}
                            </ProtectedRoute>
                        }
                    />
                    <Route
                        path="/checkout"
                        element={
                            <ProtectedRoute>
                                {lazyPage(<CheckoutPage />)}
                            </ProtectedRoute>
                        }
                    />
                    <Route
                        path="/account"
                        element={
                            <ProtectedRoute>
                                {lazyPage(<AccountPage />)}
                            </ProtectedRoute>
                        }
                    />
                    <Route
                        path="/admin/dashboard"
                        element={
                            <ProtectedRoute role="ADMIN">
                                <Suspense fallback={<LoadingState variant="page" />}>
                                    <AdminDashboard />
                                </Suspense>
                            </ProtectedRoute>
                        }
                    />
                    <Route
                        path="/wishlist"
                        element={
                            <ProtectedRoute>
                                <WishlistPage />
                            </ProtectedRoute>
                        }
                    />
                    <Route path="/login" element={<LoginPage />} />
                    <Route path="/signup" element={<SignupPage />} />
                    <Route path="*" element={<NotFound />} />
                </Routes>
            </main>

            {showCart && (
                <div className="fixed inset-0 z-50">
                    <div
                        className="fixed inset-0 backdrop-blur-sm"
                        onClick={() => setShowCart(false)}
                    />
                    <Cart closeCart={() => setShowCart(false)} />
                </div>
            )}
        </div>
    );
}

export default function App() {
    return (
        <AuthProvider>
            <ThemeProvider>
                <CartProvider>
                    <IconContext.Provider value={ICON_DEFAULTS}>
                        <Router>
                            <ToastProvider />
                            <ErrorBoundary>
                                <InnerApp />
                            </ErrorBoundary>
                        </Router>
                    </IconContext.Provider>
                </CartProvider>
            </ThemeProvider>
        </AuthProvider>
    );
}

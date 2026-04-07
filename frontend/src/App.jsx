// src/App.jsx
import { BrowserRouter as Router, Routes, Route } from "react-router-dom";
import { useEffect, useState, useCallback } from "react";
import { ChevronLeft, ChevronRight } from "lucide-react"; // Added Pagination Icons
import { Navbar } from "./components/Navbar";
import ProductGrid from "./components/ProductGrid";
import ProductDetailPage from "./pages/ProductDetailPage";
import LoadingState from "./components/LoadingState";
import ErrorState from "./components/ErrorState";
import Cart from "./components/Cart";
import AddProduct from "./pages/AddProductPage";
import UpdateProduct from "./pages/UpdateProductPage";
import LoginPage from "./pages/LoginPage";
import SignupPage from "./pages/SignupPage";
import NotFound from "./pages/NotFound";
import ProtectedRoute from "./routes/ProtectedRoute";

import { AuthProvider } from "./context/AuthContext";
import { ThemeProvider, useTheme } from "./context/ThemeContext";
import ToastProvider from "./components/ToastProvider";
import api from "./api/axios";
import { CartProvider } from "./context/CartContext.jsx";
import OrderHistoryPage from "./pages/OrderHistoryPage.jsx";
import AdminDashboard from "./pages/AdminDashboard.jsx";
import WishlistPage from "./pages/WishlistPage.jsx";

function InnerApp() {
    const [products, setProducts] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [showCart, setShowCart] = useState(false);

    // NEW Pagination States
    const [currentPage, setCurrentPage] = useState(0);
    const [totalPages, setTotalPages] = useState(0);
    const [isFiltering, setIsFiltering] = useState(false);

    const { darkMode } = useTheme();

    const fetchProducts = useCallback(async (page) => {
        try {
            setLoading(true);
            // Request the specific page
            const response = await api.get(`/products?page=${page}&size=12`);

            // Extract the 'content' array from the Spring Boot Page object
            setProducts(response.data.content || response.data);
            setTotalPages(response.data.page?.totalPages || 1);
            setError(null);
        } catch (err) {
            setError("Failed to fetch products. Please try again later.");
            console.error(err);
        } finally {
            setLoading(false);
        }
    }, []);

    // Only fetch paginated defaults if the user is NOT using the Navbar filters
    useEffect(() => {
        if (!isFiltering) {
            fetchProducts(currentPage);
        }
    }, [currentPage, isFiltering, fetchProducts]);

    // Handle data coming from the Navbar (Search & Filters)
    const handleSearchResults = useCallback((data) => {
        if (data?.isReset) {
            setIsFiltering(false);
            setCurrentPage(0);
            return;
        }

        setIsFiltering(true);

        if (data?.content) {
            setProducts(data.content);
            setTotalPages(data.page?.totalPages || 1);
        } else {
            setProducts(Array.isArray(data) ? data : []);
            setTotalPages(1);
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
            />

            <main className="container mx-auto px-4 py-8">
                <Routes>
                    <Route
                        path="/"
                        element={
                            loading ? (
                                <LoadingState />
                            ) : error ? (
                                <ErrorState error={error} />
                            ) : (
                                <div className="flex flex-col pb-8">
                                    <ProductGrid products={products} />

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
                                                className={`p-2 border-4 border-black rounded-lg shadow-[4px_4px_0px_0px_rgba(0,0,0,1)] transition-transform active:translate-x-[2px] active:translate-y-[2px] active:shadow-none disabled:opacity-50 disabled:cursor-not-allowed
                                                ${darkMode ? "bg-blue-600 hover:bg-blue-700 text-white" : "bg-yellow-400 hover:bg-yellow-500 text-black"}`}
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
                                                className={`p-2 border-4 border-black rounded-lg shadow-[4px_4px_0px_0px_rgba(0,0,0,1)] transition-transform active:translate-x-[2px] active:translate-y-[2px] active:shadow-none disabled:opacity-50 disabled:cursor-not-allowed
                                                ${darkMode ? "bg-blue-600 hover:bg-blue-700 text-white" : "bg-yellow-400 hover:bg-yellow-500 text-black"}`}
                                            >
                                                <ChevronRight size={24} />
                                            </button>
                                        </div>
                                    )}
                                </div>
                            )
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
                                <AddProduct />
                            </ProtectedRoute>
                        }
                    />
                    <Route
                        path="/update-product/:id"
                        element={
                            <ProtectedRoute role="ADMIN">
                                <UpdateProduct />
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
                        path="/admin/dashboard"
                        element={
                            <ProtectedRoute role="ADMIN">
                                <AdminDashboard />
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
                    <Router>
                        <ToastProvider />
                        <InnerApp />
                    </Router>
                </CartProvider>
            </ThemeProvider>
        </AuthProvider>
    );
}

// src/App.jsx
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import { useEffect, useState } from 'react';
import {Navbar} from './components/Navbar';
import ProductGrid from './components/ProductGrid';
import ProductDetailPage from './pages/ProductDetailPage';
import LoadingState from './components/LoadingState';
import ErrorState from './components/ErrorState';
import Cart from './components/Cart';
import AddProduct from './pages/AddProductPage';
import UpdateProduct from './pages/UpdateProductPage';
import LoginPage from './pages/LoginPage';
import SignupPage from './pages/SignupPage';
import NotFound from './pages/NotFound';
import ProtectedRoute from './routes/ProtectedRoute';

import { AuthProvider } from './context/AuthContext';
import { ThemeProvider, useTheme } from './context/ThemeContext';
import ToastProvider from './components/ToastProvider';
import api from './api/axios';
import {CartProvider} from "./context/CartContext.jsx";

function InnerApp() {
    const [products, setProducts] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [showCart, setShowCart] = useState(false);

    const { darkMode } = useTheme();

    useEffect(() => {
        const fetchProducts = async () => {
            try {
                setLoading(true);
                const response = await api.get('/products');
                setProducts(response.data);
                setError(null);
            } catch (err) {
                setError('Failed to fetch products. Please try again later.');
                console.error(err);
            } finally {
                setLoading(false);
            }
        };
        fetchProducts();
    }, []);

    return (
        <div
            className={`min-h-screen transition-colors duration-300 ${
                darkMode ? 'bg-gray-900 text-white' : 'bg-yellow-50 text-gray-900'
            }`}
        >
            <Navbar
                setShowCart={setShowCart}
                onSearchResults={setProducts}
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
                                <ProductGrid products={products}/>
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
                    <Cart
                        closeCart={() => setShowCart(false)}
                    />
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
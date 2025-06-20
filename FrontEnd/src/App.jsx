// src/App.jsx
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import { useEffect, useState } from 'react';
import Navbar from './components/Navbar';
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

function InnerApp() {
    const [products, setProducts] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [cart, setCart] = useState([]);
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

    const addToCart = (product) => {
        const existingItem = cart.find((item) => item.id === product.id);
        if (existingItem) {
            setCart(
                cart.map((item) =>
                    item.id === product.id ? { ...item, quantity: item.quantity + 1 } : item
                )
            );
        } else {
            setCart([...cart, { ...product, quantity: 1 }]);
        }
    };

    const removeFromCart = (productId) => {
        setCart(cart.filter((item) => item.id !== productId));
    };

    const updateQuantity = (productId, change) => {
        setCart(
            cart.map((item) => {
                if (item.id === productId) {
                    const newQuantity = item.quantity + change;
                    return newQuantity > 0 ? { ...item, quantity: newQuantity } : item;
                }
                return item;
            })
        );
    };

    const cartTotal = cart.reduce(
        (total, item) => total + item.price * item.quantity,
        0
    );

    return (
        <div
            className={`min-h-screen transition-colors duration-300 ${
                darkMode ? 'bg-gray-900 text-white' : 'bg-yellow-50 text-gray-900'
            }`}
        >
            <Navbar
                cartItemCount={cart.reduce((count, item) => count + item.quantity, 0)}
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
                                <ProductGrid products={products} addToCart={addToCart} />
                            )
                        }
                    />
                    <Route
                        path="/product/:id"
                        element={<ProductDetailPage addToCart={addToCart} />}
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
                        cart={cart}
                        closeCart={() => setShowCart(false)}
                        removeFromCart={removeFromCart}
                        updateQuantity={updateQuantity}
                        cartTotal={cartTotal}
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
                <Router>
                    <ToastProvider />
                    <InnerApp />
                </Router>
            </ThemeProvider>
        </AuthProvider>
    );
}
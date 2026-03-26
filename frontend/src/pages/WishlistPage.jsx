// src/pages/WishlistPage.jsx
import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { Heart, ArrowLeft } from "lucide-react";
import api from "../api/axios";
import { useTheme } from "../context/ThemeContext";
import ProductCard from "../components/ProductCard";
import LoadingState from "../components/LoadingState";
import ErrorState from "../components/ErrorState";

export default function WishlistPage() {
    const [wishlist, setWishlist] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const { darkMode } = useTheme();
    const navigate = useNavigate();

    useEffect(() => {
        const fetchWishlist = async () => {
            try {
                setLoading(true);
                const response = await api.get("/user/wishlist");
                // The backend returns an array of Wishlist objects, which contain the Product
                setWishlist(response.data);
                setError(null);
            } catch (err) {
                console.error("Failed to fetch wishlist:", err);
                setError(
                    "Could not load your wishlist. Please try again later.",
                );
            } finally {
                setLoading(false);
            }
        };

        fetchWishlist();
    }, []);

    // Function to remove an item from the list instantly without reloading
    const handleRemoveFromWishlist = (productId) => {
        setWishlist((current) =>
            current.filter((item) => item.product.id !== productId),
        );
    };

    if (loading) return <LoadingState />;
    if (error) return <ErrorState error={error} />;

    return (
        <div className="max-w-7xl mx-auto">
            <div className="flex items-center gap-4 mb-8">
                <button
                    onClick={() => navigate("/")}
                    className={`p-2 rounded-full border-2 border-black transition-transform active:translate-x-[2px] active:translate-y-[2px] shadow-[4px_4px_0px_0px_rgba(0,0,0,1)] active:shadow-none
                    ${darkMode ? "bg-gray-800 text-white" : "bg-white text-black"}`}
                >
                    <ArrowLeft size={24} />
                </button>
                <h1 className="text-4xl font-black flex items-center gap-3">
                    <Heart
                        className="text-red-500"
                        fill="currentColor"
                        size={36}
                    />
                    My Wishlist
                </h1>
            </div>

            {wishlist.length === 0 ? (
                <div
                    className={`text-center py-20 border-4 border-black rounded-lg shadow-[8px_8px_0px_0px_rgba(0,0,0,1)] ${darkMode ? "bg-gray-800" : "bg-white"}`}
                >
                    <Heart size={64} className="mx-auto text-gray-400 mb-4" />
                    <h2 className="text-2xl font-bold mb-2">
                        Your wishlist is empty
                    </h2>
                    <p className="opacity-70 mb-6">
                        Looks like you haven't saved any items yet.
                    </p>
                    <button
                        onClick={() => navigate("/")}
                        className={`px-6 py-3 font-bold border-4 border-black rounded-lg transition-transform active:translate-x-[2px] active:translate-y-[2px] shadow-[4px_4px_0px_0px_rgba(0,0,0,1)] active:shadow-none
                        ${darkMode ? "bg-blue-600 hover:bg-blue-700" : "bg-yellow-400 hover:bg-yellow-500"}`}
                    >
                        Start Shopping
                    </button>
                </div>
            ) : (
                <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
                    {wishlist.map((item) => (
                        <div
                            key={item.id}
                            onClick={() =>
                                handleRemoveFromWishlist(item.product.id)
                            }
                        >
                            {/* We pass item.product because our API returns a Wishlist entity
                               We also wrap it to intercept the heart click and remove it from UI immediately
                             */}
                            <ProductCard
                                product={item.product}
                                onClick={() =>
                                    navigate(`/product/${item.product.id}`)
                                }
                            />
                        </div>
                    ))}
                </div>
            )}
        </div>
    );
}

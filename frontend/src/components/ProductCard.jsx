import { ShoppingCart, Heart } from "lucide-react";
import { useState, useEffect } from "react";
import { useAuth } from "../context/AuthContext";
import { useNavigate } from "react-router-dom";
import { useTheme } from "../context/ThemeContext";
import { toast } from "sonner";
import api from "../api/axios.js";
import { useCart } from "../context/CartContext.jsx";

export default function ProductCard({
    product,
    onClick,
    initialIsWishlisted = null,
    onWishlistChange,
}) {
    const { user } = useAuth();
    const { darkMode } = useTheme();
    const navigate = useNavigate();
    const { setCartItemCount } = useCart();

    const [isWishlisted, setIsWishlisted] = useState(
        initialIsWishlisted || false,
    );

    const imageUrl = product.imageUrl
        ? product.imageUrl.toString()
        : "https://picsum.photos/400";

    useEffect(() => {
        if (initialIsWishlisted !== null) {
            setIsWishlisted(initialIsWishlisted);
        }
    }, [initialIsWishlisted]);

    const handleWishlistToggle = async (e) => {
        e.stopPropagation();
        if (!user) {
            toast.error("Please login to add to wishlist");
            return navigate("/login");
        }
        try {
            const response = await api.post(`/user/wishlist/${product.id}`);
            const newState = !isWishlisted;
            setIsWishlisted(newState);
            const msg =
                response.data?.message ||
                (typeof response.data === "string"
                    ? response.data
                    : "Wishlist updated");
            toast.success(msg);

            if (onWishlistChange) {
                onWishlistChange(product.id, newState);
            }
        } catch (error) {
            toast.error("Failed to update wishlist");
        }
    };

    const handleAddToCart = async (product) => {
        try {
            if (!user?.username) {
                toast.error("User not found");
                return;
            }

            const response = await api.post(
                `/cart/${user.username}/add`,
                null,
                {
                    params: {
                        productId: product.id,
                        quantity: 1,
                    },
                },
            );

            const updatedCart = response.data;
            const totalCount = updatedCart.reduce(
                (acc, item) => acc + item.quantity,
                0,
            );
            setCartItemCount(totalCount);
            toast.success("Item added to cart!");
            return updatedCart;
        } catch (error) {
            console.error("Failed to add to cart:", error);
            toast.error("Failed to add to cart.");
        }
    };

    return (
        <div
            className={`group relative rounded-lg overflow-hidden transition-all duration-300 transform hover:-translate-y-1
        ${darkMode ? "bg-gray-800 shadow-blue-500/20" : "bg-white shadow-black/30"}
        shadow-lg border-4 ${darkMode ? "border-gray-700" : "border-black"}`}
        >
            <div className="relative aspect-w-1 aspect-h-1 overflow-hidden">
                <img
                    src={imageUrl}
                    alt={product.name}
                    loading="lazy"
                    className="w-full h-64 object-cover transition-transform duration-500 group-hover:scale-105"
                    onError={(e) => {
                        e.target.src = "https://picsum.photos/400";
                    }}
                />

                {/* Floating Wishlist Button */}
                <button
                    onClick={handleWishlistToggle}
                    className={`absolute top-3 right-3 p-2 rounded-full border-2 border-black shadow-[2px_2px_0px_0px_rgba(0,0,0,1)] transition-transform active:translate-x-[2px] active:translate-y-[2px] active:shadow-none z-10 cursor-pointer
                    ${isWishlisted ? "bg-red-500 text-white" : darkMode ? "bg-gray-800 text-white hover:bg-gray-700" : "bg-white text-black hover:bg-red-50"}`}
                >
                    <Heart
                        size={20}
                        fill={isWishlisted ? "currentColor" : "none"}
                    />
                </button>
            </div>

            <div className="p-4 relative">
                <div className="flex justify-between items-start mb-2">
                    <h3 className="text-xl font-black truncate pr-2">
                        {product.name}
                    </h3>
                    <span
                        className={`text-xs font-bold px-2 py-1 rounded shrink-0 ${product.available ? "bg-green-500 text-white" : "bg-red-500 text-white"}`}
                    >
                        {product.available ? "In Stock" : "Out of Stock"}
                    </span>
                </div>

                <div className="mb-4">
                    <p className="text-sm opacity-70">{product.category}</p>
                    <p className="text-xl font-bold mt-1">
                        ₹{product.price.toFixed(2)}
                    </p>
                </div>

                <div className="flex space-x-2">
                    <button
                        onClick={(e) => {
                            e.stopPropagation();
                            onClick();
                        }}
                        className={`flex-1 py-2 px-4 rounded font-bold text-sm cursor-pointer
              ${darkMode ? "bg-blue-600 hover:bg-blue-700" : "bg-yellow-400 hover:bg-yellow-500"}
              transition-colors duration-300 transform border-2 border-black shadow-[4px_4px_0px_0px_rgba(0,0,0,1)]
              active:translate-x-[2px] active:translate-y-[2px] active:shadow-[2px_2px_0px_0px_rgba(0,0,0,1)]`}
                    >
                        View Details
                    </button>

                    <button
                        onClick={(e) => {
                            e.stopPropagation();
                            if (!user) {
                                toast.error("Login required");
                                return navigate("/login");
                            }
                            if (product.available) handleAddToCart(product);
                        }}
                        disabled={!product.available}
                        className={`py-2 px-4 rounded font-bold text-sm cursor-pointer
        ${
            product.available
                ? darkMode
                    ? "bg-rose-600 hover:bg-rose-700"
                    : "bg-rose-500 hover:bg-rose-600 text-white"
                : "bg-gray-400 cursor-not-allowed"
        }
        transition-colors duration-300 transform border-2 border-black shadow-[4px_4px_0px_0px_rgba(0,0,0,1)]
        ${product.available ? "active:translate-x-[2px] active:translate-y-[2px] active:shadow-[2px_2px_0px_0px_rgba(0,0,0,1)]" : ""}`}
                    >
                        <ShoppingCart size={16} />
                    </button>
                </div>
            </div>
        </div>
    );
}

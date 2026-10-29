import {
    ShoppingCartIcon as ShoppingCart,
    HeartIcon as Heart,
    CircleNotchIcon as Loader2,
} from "@phosphor-icons/react";
import StarRating from "./StarRating";
import { focusRing } from "../utils/ui.js";
import { useState } from "react";
import { useAuth } from "../context/AuthContext";
import { useNavigate } from "react-router-dom";
import { useTheme } from "../context/ThemeContext";
import { toast } from "sonner";
import api from "../api/axios.js";
import { useCart } from "../context/CartContext.jsx";
import { productImage, PLACEHOLDER } from "../utils/image.js";
import { categoryLabel } from "../constants/categories.js";

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

    // Without a pending flag a double-click fires two POSTs and two toasts.
    const [adding, setAdding] = useState(false);

    const imageUrl = productImage(product.imageUrl, 400);
    const lowStock =
        product.available && product.quantity > 0 && product.quantity <= 5;

    // Follow the parent's value when it changes (e.g. the wishlist finished
    // loading). Done during render, not in an effect, to avoid a second render.
    const [prevInitial, setPrevInitial] = useState(initialIsWishlisted);
    if (initialIsWishlisted !== prevInitial) {
        setPrevInitial(initialIsWishlisted);
        if (initialIsWishlisted !== null) setIsWishlisted(initialIsWishlisted);
    }

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
        } catch {
            toast.error("Failed to update wishlist");
        }
    };

    const handleAddToCart = async (product) => {
        if (adding) return;
        try {
            if (!user?.username) {
                toast.error("User not found");
                return;
            }

            setAdding(true);

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
        } finally {
            setAdding(false);
        }
    };

    return (
        <div
            className={`group relative rounded-lg overflow-hidden transition-all duration-300 transform hover:-translate-y-1
        ${darkMode ? "bg-gray-800" : "bg-white"}
        shadow-[6px_6px_0px_0px_rgba(0,0,0,1)] border-4 border-black`}
        >
            <div className="relative aspect-w-1 aspect-h-1 overflow-hidden">
                <img
                    src={imageUrl}
                    alt={product.name}
                    loading="lazy"
                    width={400}
                    height={256}
                    className="w-full h-64 object-cover"
                    onError={(e) => {
                        e.target.src = PLACEHOLDER;
                    }}
                />

                {/* Floating Wishlist Button */}
                {lowStock && (
                    <span className="absolute top-3 left-3 px-2 py-1 text-xs font-black rounded border-2 border-black bg-yellow-300 text-black shadow-[2px_2px_0px_0px_rgba(0,0,0,1)]">
                        Only {product.quantity} left
                    </span>
                )}

                <button
                    onClick={handleWishlistToggle}
                    aria-label={isWishlisted ? `Remove ${product.name} from wishlist` : `Add ${product.name} to wishlist`}
                    aria-pressed={isWishlisted}
                    className={`absolute top-3 right-3 w-11 h-11 flex items-center justify-center ${focusRing} rounded-full border-2 border-black shadow-[2px_2px_0px_0px_rgba(0,0,0,1)] transition-transform active:translate-x-[2px] active:translate-y-[2px] active:shadow-none z-10 cursor-pointer
                    ${isWishlisted ? "bg-red-500 text-white" : darkMode ? "bg-gray-800 text-white hover:bg-gray-700" : "bg-white text-black hover:bg-red-50"}`}
                >
                    <Heart size={20} weight={isWishlisted ? "fill" : "bold"} />
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
                    <p className="text-sm opacity-70">{categoryLabel(product.category)}</p>
                    {product.reviewCount > 0 && (
                        <p className="flex items-center gap-1 mt-1 text-sm font-bold">
                            <StarRating value={product.averageRating} size={14} />
                            <span>{Number(product.averageRating).toFixed(1)}</span>
                            <span className="opacity-70">({product.reviewCount})</span>
                        </p>
                    )}
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
                        className={`flex-1 min-h-[44px] py-2 px-4 rounded font-bold text-sm cursor-pointer ${focusRing}
              bg-yellow-400 hover:bg-yellow-500 text-black
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
                        disabled={!product.available || adding}
                        aria-label={product.available ? `Add ${product.name} to cart` : `${product.name} is out of stock`}
                        className={`min-h-[44px] min-w-[44px] flex items-center justify-center py-2 px-4 rounded font-bold text-sm cursor-pointer ${focusRing}
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
                        {adding ? (
                            <Loader2 size={16} className="animate-spin" />
                        ) : (
                            <ShoppingCart size={16} />
                        )}
                    </button>
                </div>
            </div>
        </div>
    );
}

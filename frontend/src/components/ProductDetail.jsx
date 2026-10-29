import { useNavigate } from "react-router-dom";
import {
    PencilSimpleIcon as Edit,
    ShoppingCartIcon as ShoppingCart,
    ArchiveIcon,
    HeartIcon as Heart,
    CircleNotchIcon as Loader2,
} from "@phosphor-icons/react";
import { useState, useEffect } from "react";
import api from "../api/axios";
import { useAuth } from "../context/AuthContext";
import { useTheme } from "../context/ThemeContext";
import { toast } from "sonner";
import { useCart } from "../context/CartContext.jsx";
import ProductGallery from "./ProductGallery";
import StarRating from "./StarRating";
import { focusRing } from "../utils/ui.js";
import { categoryLabel } from "../constants/categories.js";

export default function ProductDetail({ product }) {
    const { darkMode } = useTheme();
    const { user } = useAuth();
    const isAdmin = user?.role === "ADMIN";
    const navigate = useNavigate();

    const [isWishlisted, setIsWishlisted] = useState(false);

    // Without a pending flag a double-click fires two POSTs and two toasts.
    const [adding, setAdding] = useState(false);

    const createdAtDate = new Date(product.createdAt);
    const formattedCreatedAtDate = `${String(createdAtDate.getDate()).padStart(2, "0")}-${String(createdAtDate.getMonth() + 1).padStart(2, "0")}-${createdAtDate.getFullYear()}`;
    const { setCartItemCount } = useCart();

    useEffect(() => {
        const checkWishlistStatus = async () => {
            if (user) {
                try {
                    const response = await api.get("/user/wishlist");
                    const isInWishlist = response.data.some(
                        (item) => item.product.id === product.id,
                    );
                    setIsWishlisted(isInWishlist);
                } catch (error) {
                    console.error("Failed to fetch wishlist status:", error);
                }
            }
        };
        checkWishlistStatus();
    }, [user, product.id]);

    const handleWishlistToggle = async () => {
        if (!user) {
            toast.error("Please login to add to wishlist");
            return navigate("/login");
        }
        try {
            const response = await api.post(`/user/wishlist/${product.id}`);
            setIsWishlisted(!isWishlisted);
            const msg =
                response.data?.message ||
                (typeof response.data === "string"
                    ? response.data
                    : "Wishlist updated");
            toast.success(msg);
        } catch (error) {
            console.error("Wishlist error:", error);
            toast.error("Failed to update wishlist");
        }
    };

    const handleArchive = async () => {
        if (
            !window.confirm(
                "Archive this product?\n\nThis hides the product from the store; past orders keep it.",
            )
        )
            return;

        try {
            await api.delete(`/product/${product.id}`);
            toast.success("Product archived. It is now hidden from the store.");
            navigate("/");
        } catch (error) {
            console.error("Archive error:", error);
            toast.error("Could not archive the product");
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
        <div>
            <div
                className={`grid md:grid-cols-2 gap-8 rounded-lg overflow-hidden border-4 border-black p-6 shadow-[8px_8px_0px_0px_rgba(0,0,0,1)]
                ${darkMode ? "bg-gray-800" : "bg-white"}`}
            >
                <ProductGallery
                    key={product.id}
                    name={product.name}
                    imageUrl={product.imageUrl}
                    images={product.images}
                />

                <div>
                    <div className="mb-6">
                        {/* Title and Wishlist Button Header */}
                        <div className="flex justify-between items-start gap-4 mb-2">
                            <h2 className="text-3xl font-black">
                                {product.name}
                            </h2>
                            <button
                                onClick={handleWishlistToggle}
                                aria-label={isWishlisted ? "Remove from wishlist" : "Add to wishlist"}
                                aria-pressed={isWishlisted}
                                className={`p-3 shrink-0 ${focusRing} rounded-full border-4 border-black transition-all cursor-pointer
                                shadow-[4px_4px_0px_0px_rgba(0,0,0,1)] active:translate-x-[2px] active:translate-y-[2px] active:shadow-none
                                ${isWishlisted ? "bg-red-500 text-white" : darkMode ? "bg-gray-700 text-white hover:bg-gray-600" : "bg-white text-black hover:bg-gray-100"}
                                `}
                            >
                                <Heart
                                    weight={isWishlisted ? "fill" : "bold"}
                                    size={28}
                                />
                            </button>
                        </div>

                        <a
                            href="#reviews"
                            className={`inline-flex items-center gap-2 mb-3 min-h-[44px] font-bold hover:underline rounded ${focusRing}`}
                        >
                            {product.reviewCount > 0 ? (
                                <>
                                    <StarRating value={product.averageRating} size={20} />
                                    <span className="font-black">
                                        {Number(product.averageRating).toFixed(1)}
                                    </span>
                                    <span className="opacity-70">
                                        ({product.reviewCount}{" "}
                                        {product.reviewCount === 1 ? "review" : "reviews"})
                                    </span>
                                </>
                            ) : (
                                <span className="opacity-70">No reviews yet</span>
                            )}
                        </a>

                        <div
                            className={`block w-fit px-3 py-1 rounded-lg text-sm font-bold mb-4
                            ${darkMode ? "bg-gray-700" : "bg-yellow-300"}`}
                        >
                            {categoryLabel(product.category)}
                        </div>
                        <p className="text-3xl font-black mb-4">
                            ₹{product.price.toFixed(2)}
                        </p>

                        <div
                            className={`p-4 mb-4 rounded-lg ${darkMode ? "bg-gray-700" : "bg-gray-100"}`}
                        >
                            <p>{product.description}</p>
                        </div>
                    </div>

                    <div className="grid grid-cols-2 gap-4 mb-6">
                        <div
                            className={`p-4 rounded-lg ${darkMode ? "bg-gray-700" : "bg-gray-100"}`}
                        >
                            <p className="text-sm opacity-70">Product ID</p>
                            <p className="font-bold">{product.id}</p>
                        </div>
                        <div
                            className={`p-4 rounded-lg ${darkMode ? "bg-gray-700" : "bg-gray-100"}`}
                        >
                            <p className="text-sm opacity-70">Creation Date</p>
                            <p className="font-bold">
                                {formattedCreatedAtDate}
                            </p>
                        </div>
                        <div
                            className={`p-4 rounded-lg ${darkMode ? "bg-gray-700" : "bg-gray-100"}`}
                        >
                            <p className="text-sm opacity-70">
                                Available Quantity
                            </p>
                            <p className="font-bold">
                                {product.quantity}
                                {product.available &&
                                    product.quantity > 0 &&
                                    product.quantity <= 5 && (
                                        <span className="ml-2 inline-block px-2 py-0.5 text-xs font-black border-2 border-black rounded bg-yellow-300 text-black">
                                            Only {product.quantity} left
                                        </span>
                                    )}
                            </p>
                        </div>
                        <div
                            className={`p-4 rounded-lg ${darkMode ? "bg-gray-700" : "bg-gray-100"}`}
                        >
                            <p className="text-sm opacity-70">Status</p>
                            <p
                                className={`font-bold ${product.available ? "text-green-500" : "text-red-500"}`}
                            >
                                {product.available
                                    ? "In Stock"
                                    : "Out of Stock"}
                            </p>
                        </div>
                    </div>

                    <button
                        onClick={() => {
                            if (!user) {
                                toast.error("Login required");
                                return navigate("/login");
                            }
                            if (product.available) handleAddToCart(product);
                        }}
                        disabled={!product.available || adding}
                        className={`w-full py-3 px-6 rounded-lg font-bold text-lg flex items-center justify-center gap-2 border-4 border-black transition-colors duration-300 cursor-pointer
                            shadow-[8px_8px_0px_0px_rgba(0,0,0,1)]
                            ${
                                product.available
                                    ? darkMode
                                        ? "bg-rose-600 hover:bg-rose-700"
                                        : "bg-rose-500 hover:bg-rose-600 text-white"
                                    : "bg-gray-400 cursor-not-allowed"
                            }
                            ${product.available ? "active:translate-x-[4px] active:translate-y-[4px] active:shadow-[4px_4px_0px_0px_rgba(0,0,0,1)]" : ""}`}
                    >
                        {adding ? (
                            <Loader2 size={20} className="animate-spin" />
                        ) : (
                            <ShoppingCart size={20} />
                        )}
                        {!product.available
                            ? "Out of Stock"
                            : adding
                              ? "Adding..."
                              : "Add to Cart"}
                    </button>
                </div>
            </div>

            {isAdmin && (
                <div className="flex flex-wrap gap-4 mt-6">
                    <button
                        onClick={() =>
                            navigate(`/update-product/${product.id}`)
                        }
                        className={`py-3 px-6 rounded-lg font-bold flex items-center gap-2 border-4 border-black transition-colors
                ${darkMode ? "bg-gray-700 hover:bg-gray-600 text-white" : "bg-white hover:bg-gray-100 text-black"}
                shadow-[8px_8px_0px_0px_rgba(0,0,0,1)] active:translate-x-[4px] active:translate-y-[4px] active:shadow-[4px_4px_0px_0px_rgba(0,0,0,1)]`}
                    >
                        <Edit size={20} />
                        Edit Product
                    </button>

                    <button
                        onClick={handleArchive}
                        className={`py-3 px-6 rounded-lg font-bold flex items-center gap-2 border-4 border-black transition-colors
                ${darkMode ? "bg-gray-600 hover:bg-gray-700" : "bg-gray-500 hover:bg-gray-600 text-white"}
                shadow-[8px_8px_0px_0px_rgba(0,0,0,1)] active:translate-x-[4px] active:translate-y-[4px] active:shadow-[4px_4px_0px_0px_rgba(0,0,0,1)]`}
                    >
                        <ArchiveIcon size={20} />
                        Archive product
                    </button>
                </div>
            )}
        </div>
    );
}

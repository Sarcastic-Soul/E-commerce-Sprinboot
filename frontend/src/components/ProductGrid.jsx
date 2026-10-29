import { useState, useEffect } from "react";
import { MagnifyingGlassIcon } from "@phosphor-icons/react";
import ProductCard from "./ProductCard";
import api from "../api/axios";
import { useAuth } from "../context/AuthContext";
import { useNavigate } from "react-router-dom";
import { useTheme } from "../context/ThemeContext";

export default function ProductGrid({ products, onClearFilters }) {
    // console.log(products);
    const { user } = useAuth();
    const navigate = useNavigate();
    const { darkMode } = useTheme();

    const [wishlistIds, setWishlistIds] = useState([]);

    useEffect(() => {
        const fetchWishlist = async () => {
            if (user) {
                try {
                    const response = await api.get("/user/wishlist");
                    const ids = response.data.map((item) =>
                        Number(item.product.id),
                    );
                    setWishlistIds(ids);
                } catch (error) {
                    console.error("Failed to fetch wishlist for grid:", error);
                }
            } else {
                setWishlistIds([]);
            }
        };
        fetchWishlist();
    }, [user]);

    // A blank page reads as "broken" rather than "no matches".
    if (products.length === 0) {
        return (
            <div className="flex justify-center py-16">
                <div
                    className={`flex flex-col items-center text-center gap-4 px-10 py-12 rounded-lg border-4 border-black shadow-[8px_8px_0px_0px_rgba(0,0,0,1)]
                    ${darkMode ? "bg-gray-800 text-white" : "bg-white text-black"}`}
                >
                    <MagnifyingGlassIcon size={56} aria-hidden="true" />
                    <h3 className="text-2xl font-black">NO PRODUCTS FOUND</h3>
                    <p className="font-bold opacity-70 max-w-xs">
                        Nothing matches the current search and filters.
                    </p>
                    {onClearFilters && (
                        <button
                            onClick={onClearFilters}
                            className={`mt-2 py-2 px-6 rounded-lg font-black border-4 border-black
                            shadow-[4px_4px_0px_0px_rgba(0,0,0,1)] transition-transform
                            active:translate-x-[2px] active:translate-y-[2px] active:shadow-none
                            bg-yellow-400 hover:bg-yellow-500 text-black`}
                        >
                            Clear filters
                        </button>
                    )}
                </div>
            </div>
        );
    }

    return (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
            {products.map((product) => (
                <ProductCard
                    key={product.id}
                    product={product}
                    // Pass true if this product's ID is in the wishlist array
                    initialIsWishlisted={wishlistIds.includes(
                        Number(product.id),
                    )}
                    onClick={() => navigate(`/product/${product.id}`)}
                />
            ))}
        </div>
    );
}

import { useState, useEffect } from "react";
import ProductCard from "./ProductCard";
import api from "../api/axios";
import { useAuth } from "../context/AuthContext";
import { useNavigate } from "react-router-dom";

export default function ProductGrid({ products }) {
    const { user } = useAuth();
    const navigate = useNavigate();

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

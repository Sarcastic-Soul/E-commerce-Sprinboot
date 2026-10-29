import { useParams } from "react-router-dom";
import { useCallback, useEffect, useState } from "react";
import ProductDetail from "../components/ProductDetail";
import ProductReviews from "../components/reviews/ProductReviews";
import LoadingState from "../components/LoadingState";
import ErrorState from "../components/ErrorState";
import api from "../api/axios";

export default function ProductDetailPage() {
    const { id } = useParams();
    const [product, setProduct] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    const fetchProduct = useCallback(
        async (silent = false) => {
            try {
                if (!silent) setLoading(true);
                const { data } = await api.get(`/product/${id}`);
                setProduct(data);
                setError(null);
            } catch (err) {
                console.error(err);
                if (!silent)
                    setError(
                        err.response?.status === 404
                            ? "This product is not available."
                            : "Failed to load product.",
                    );
            } finally {
                if (!silent) setLoading(false);
            }
        },
        [id],
    );

    useEffect(() => {
        fetchProduct();
    }, [fetchProduct]);

    // After a review is written or removed, refresh the average + count quietly.
    const refreshRating = useCallback(() => fetchProduct(true), [fetchProduct]);

    if (loading) return <LoadingState variant="page" />;
    if (error) return <ErrorState error={error} onRetry={() => fetchProduct()} />;

    return (
        <div className="pb-12">
            <ProductDetail product={product} />
            <ProductReviews
                key={product.id}
                productId={product.id}
                averageRating={product.averageRating}
                reviewCount={product.reviewCount}
                onChanged={refreshRating}
            />
        </div>
    );
}

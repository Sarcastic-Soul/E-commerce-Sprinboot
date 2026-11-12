import { useParams } from "react-router";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import ProductDetail from "../components/ProductDetail";
import ProductReviews from "../components/reviews/ProductReviews";
import LoadingState from "../components/LoadingState";
import ErrorState from "../components/ErrorState";
import api from "../api/axios";

export default function ProductDetailPage() {
    const { id } = useParams();
    const queryClient = useQueryClient();
    const {
        data: product,
        isPending: loading,
        error: queryError,
        refetch,
    } = useQuery({
        queryKey: ["products", id],
        queryFn: async () => (await api.get(`/product/${id}`)).data,
    });

    // After a review is written or removed, refresh the average + count quietly.
    const refreshRating = () => queryClient.invalidateQueries({ queryKey: ["products", id] });

    if (loading) return <LoadingState variant="page" />;
    // A failed quiet refresh keeps the product on screen; only a failed first load shows the error.
    if (queryError && !product) {
        return (
            <ErrorState
                error={
                    queryError.response?.status === 404
                        ? "This product is not available."
                        : "Failed to load product."
                }
                onRetry={refetch}
            />
        );
    }

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

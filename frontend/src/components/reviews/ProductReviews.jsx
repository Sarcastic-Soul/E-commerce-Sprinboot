import { useCallback, useEffect, useState } from "react";
import { toast } from "sonner";
import { ChatTextIcon, PencilSimpleIcon, TrashIcon } from "@phosphor-icons/react";
import api from "../../api/axios";
import { useAuth } from "../../context/AuthContext";
import { useTheme } from "../../context/ThemeContext";
import LoadingState from "../LoadingState";
import ErrorState from "../ErrorState";
import StarRating from "../StarRating";
import ReviewForm from "./ReviewForm";
import { errorMessage, formatDate } from "../../utils/format.js";
import { btn, panel } from "../../utils/ui.js";

const PAGE_SIZE = 10;

function ReviewItem({ review, mine }) {
    return (
        <article className="py-4">
            <div className="flex flex-wrap items-center gap-x-3 gap-y-1 mb-1">
                <StarRating value={review.rating} size={16} />
                <span className="font-black">
                    {review.username}
                    {mine && " (you)"}
                </span>
                <span className="text-sm font-bold opacity-70">
                    {formatDate(review.updatedAt || review.createdAt)}
                </span>
            </div>
            {review.comment && (
                <p className="whitespace-pre-line break-words">{review.comment}</p>
            )}
        </article>
    );
}

/**
 * Reviews list (paged, "load more") plus the signed-in buyer's own review form.
 * `onChanged` lets the page refresh the product's average and count.
 */
export default function ProductReviews({ productId, averageRating = 0, reviewCount = 0, onChanged }) {
    const { user } = useAuth();
    const { darkMode } = useTheme();

    const [reviews, setReviews] = useState([]);
    const [page, setPage] = useState(0);
    const [totalPages, setTotalPages] = useState(0);
    const [loading, setLoading] = useState(true);
    const [loadingMore, setLoadingMore] = useState(false);
    const [error, setError] = useState(null);

    const [mine, setMine] = useState({ canReview: false, review: null });
    const [editing, setEditing] = useState(false);
    const [confirmDelete, setConfirmDelete] = useState(false);
    const [deleting, setDeleting] = useState(false);

    const fetchPage = useCallback(
        (p) =>
            api.get(`/product/${productId}/reviews`, {
                params: { page: p, size: PAGE_SIZE },
            }),
        [productId],
    );

    const loadFirst = useCallback(async () => {
        setLoading(true);
        setError(null);
        try {
            const { data } = await fetchPage(0);
            setReviews(data.content || []);
            setTotalPages(data.page?.totalPages || 0);
            setPage(0);
        } catch (err) {
            setError(errorMessage(err, "Could not load reviews."));
        } finally {
            setLoading(false);
        }
    }, [fetchPage]);

    const loadMine = useCallback(async () => {
        if (!user) {
            setMine({ canReview: false, review: null });
            return;
        }
        try {
            const { data } = await api.get(`/product/${productId}/reviews/me`);
            setMine({ canReview: Boolean(data.canReview), review: data.review || null });
        } catch {
            setMine({ canReview: false, review: null });
        }
    }, [productId, user]);

    useEffect(() => {
        loadFirst();
    }, [loadFirst]);

    useEffect(() => {
        loadMine();
    }, [loadMine]);

    const loadMore = async () => {
        setLoadingMore(true);
        try {
            const next = page + 1;
            const { data } = await fetchPage(next);
            setReviews((list) => {
                const seen = new Set(list.map((r) => r.id));
                return [...list, ...(data.content || []).filter((r) => !seen.has(r.id))];
            });
            setTotalPages(data.page?.totalPages || 0);
            setPage(next);
        } catch (err) {
            toast.error(errorMessage(err, "Could not load more reviews"));
        } finally {
            setLoadingMore(false);
        }
    };

    const refreshAll = async () => {
        await Promise.all([loadFirst(), loadMine()]);
        onChanged?.();
    };

    const handleSubmit = async (values) => {
        try {
            await api.post(`/product/${productId}/reviews`, values);
            toast.success(mine.review ? "Review updated" : "Thanks for your review");
            setEditing(false);
            await refreshAll();
        } catch (err) {
            toast.error(errorMessage(err, "Could not save your review"));
        }
    };

    const handleDelete = async () => {
        setDeleting(true);
        try {
            await api.delete(`/product/${productId}/reviews/me`);
            toast.success("Review deleted");
            setConfirmDelete(false);
            await refreshAll();
        } catch (err) {
            toast.error(errorMessage(err, "Could not delete your review"));
        } finally {
            setDeleting(false);
        }
    };

    const ownReview = mine.review;
    const others = ownReview ? reviews.filter((r) => r.id !== ownReview.id) : reviews;

    return (
        <section id="reviews" className={`${panel(darkMode)} p-5 sm:p-6 mt-8 scroll-mt-6`} aria-labelledby="reviews-h">
            <div className="flex flex-wrap items-center gap-4 mb-6">
                <h2 id="reviews-h" className="text-2xl font-black">
                    REVIEWS
                </h2>
                {reviewCount > 0 ? (
                    <div className="flex items-center gap-2">
                        <StarRating value={averageRating} size={22} />
                        <span className="font-black text-lg">{Number(averageRating).toFixed(1)}</span>
                        <span className="font-bold opacity-70">
                            ({reviewCount} {reviewCount === 1 ? "review" : "reviews"})
                        </span>
                    </div>
                ) : (
                    <span className="font-bold opacity-70">No ratings yet</span>
                )}
            </div>

            {/* The signed-in shopper's own review */}
            {user && (ownReview || mine.canReview) && (
                <div className={`mb-6 p-4 rounded-lg border-4 border-black ${darkMode ? "bg-gray-700" : "bg-yellow-100"}`}>
                    {ownReview && !editing ? (
                        <>
                            <h3 className="font-black text-lg">Your review</h3>
                            <ReviewItem review={ownReview} mine />
                            {confirmDelete ? (
                                <div className="flex flex-wrap items-center gap-3" role="alertdialog" aria-label="Confirm delete">
                                    <p className="font-black w-full">Delete your review?</p>
                                    <button
                                        type="button"
                                        onClick={handleDelete}
                                        disabled={deleting}
                                        className={btn(darkMode, "danger")}
                                    >
                                        {deleting ? "Deleting..." : "Yes, delete"}
                                    </button>
                                    <button
                                        type="button"
                                        onClick={() => setConfirmDelete(false)}
                                        className={btn(darkMode, "secondary")}
                                    >
                                        Keep it
                                    </button>
                                </div>
                            ) : (
                                <div className="flex flex-wrap gap-3">
                                    {mine.canReview && (
                                        <button
                                            type="button"
                                            onClick={() => setEditing(true)}
                                            className={btn(darkMode, "secondary")}
                                        >
                                            <PencilSimpleIcon size={18} /> Edit review
                                        </button>
                                    )}
                                    <button
                                        type="button"
                                        onClick={() => setConfirmDelete(true)}
                                        className={btn(darkMode, "danger")}
                                    >
                                        <TrashIcon size={18} /> Delete review
                                    </button>
                                </div>
                            )}
                        </>
                    ) : (
                        <>
                            <h3 className="font-black text-lg mb-3">
                                {ownReview ? "Edit your review" : "Write a review"}
                            </h3>
                            <ReviewForm
                                initial={ownReview}
                                onSubmit={handleSubmit}
                                onCancel={ownReview ? () => setEditing(false) : undefined}
                            />
                        </>
                    )}
                </div>
            )}

            {loading ? (
                <LoadingState variant="list" count={3} />
            ) : error ? (
                <ErrorState error={error} onRetry={loadFirst} />
            ) : others.length === 0 && !ownReview ? (
                <div className="py-8 text-center">
                    <ChatTextIcon size={48} className="mx-auto mb-2 opacity-60" />
                    <p className="font-black text-lg">No reviews yet</p>
                    <p className="font-bold opacity-70">
                        {mine.canReview
                            ? "You bought this. Be the first to review it."
                            : "Reviews come from people who bought this product."}
                    </p>
                </div>
            ) : (
                <>
                    <div className="divide-y-2 divide-black">
                        {others.map((r) => (
                            <ReviewItem key={r.id} review={r} />
                        ))}
                    </div>
                    {page < totalPages - 1 && (
                        <div className="mt-4 flex justify-center">
                            <button
                                type="button"
                                onClick={loadMore}
                                disabled={loadingMore}
                                className={btn(darkMode, "primary")}
                            >
                                {loadingMore ? "Loading..." : "Load more reviews"}
                            </button>
                        </div>
                    )}
                </>
            )}
        </section>
    );
}

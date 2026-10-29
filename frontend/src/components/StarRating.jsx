import { StarIcon, StarHalfIcon } from "@phosphor-icons/react";

/** Read-only 0-5 star display. Rounds to the nearest half star. */
export default function StarRating({ value = 0, size = 18, className = "" }) {
    const rounded = Math.round(Number(value || 0) * 2) / 2;
    return (
        <span
            className={`inline-flex items-center text-yellow-500 ${className}`}
            role="img"
            aria-label={`Rated ${Number(value || 0).toFixed(1)} out of 5`}
        >
            {[1, 2, 3, 4, 5].map((n) =>
                rounded >= n ? (
                    <StarIcon key={n} size={size} weight="fill" />
                ) : rounded >= n - 0.5 ? (
                    <StarHalfIcon key={n} size={size} weight="fill" />
                ) : (
                    <StarIcon key={n} size={size} />
                ),
            )}
        </span>
    );
}

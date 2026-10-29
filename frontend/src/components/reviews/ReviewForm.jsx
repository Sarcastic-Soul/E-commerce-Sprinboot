import { useId, useState } from "react";
import { StarIcon } from "@phosphor-icons/react";
import { useTheme } from "../../context/ThemeContext";
import { btn, inputCls, labelCls } from "../../utils/ui.js";

const MAX = 1000;
const WORDS = ["", "Poor", "Fair", "Good", "Very good", "Excellent"];

export default function ReviewForm({ initial, onSubmit, onCancel }) {
    const { darkMode } = useTheme();
    const idp = useId();
    const [rating, setRating] = useState(initial?.rating ?? 0);
    const [hover, setHover] = useState(0);
    const [comment, setComment] = useState(initial?.comment ?? "");
    const [error, setError] = useState("");
    const [saving, setSaving] = useState(false);

    const shown = hover || rating;

    const handleSubmit = async (e) => {
        e.preventDefault();
        if (rating < 1) {
            setError("Pick a rating from 1 to 5 stars");
            return;
        }
        setSaving(true);
        try {
            await onSubmit({ rating, comment: comment.trim() });
        } finally {
            setSaving(false);
        }
    };

    return (
        <form onSubmit={handleSubmit} noValidate className="space-y-4">
            <fieldset>
                <legend className={labelCls}>Your rating</legend>
                <div
                    className="flex items-center gap-1"
                    onMouseLeave={() => setHover(0)}
                >
                    {[1, 2, 3, 4, 5].map((n) => (
                        <label
                            key={n}
                            onMouseEnter={() => setHover(n)}
                            className="w-11 h-11 flex items-center justify-center cursor-pointer rounded-lg text-yellow-500 has-[:focus-visible]:outline-4 has-[:focus-visible]:outline-rose-500"
                        >
                            <input
                                type="radio"
                                name={`${idp}-rating`}
                                value={n}
                                checked={rating === n}
                                onChange={() => {
                                    setRating(n);
                                    setError("");
                                }}
                                className="sr-only"
                            />
                            <StarIcon size={32} weight={n <= shown ? "fill" : "bold"} aria-hidden="true" />
                            <span className="sr-only">
                                {n} {n === 1 ? "star" : "stars"}, {WORDS[n]}
                            </span>
                        </label>
                    ))}
                    <span className="ml-2 font-black" aria-hidden="true">
                        {WORDS[shown]}
                    </span>
                </div>
                {error && (
                    <p role="alert" className={`mt-1 text-sm font-bold ${darkMode ? "text-red-300" : "text-red-700"}`}>
                        {error}
                    </p>
                )}
            </fieldset>

            <div>
                <label htmlFor={`${idp}-comment`} className={labelCls}>
                    Comment (optional)
                </label>
                <textarea
                    id={`${idp}-comment`}
                    value={comment}
                    onChange={(e) => setComment(e.target.value.slice(0, MAX))}
                    rows={4}
                    maxLength={MAX}
                    aria-describedby={`${idp}-count`}
                    className={inputCls(darkMode)}
                />
                <p id={`${idp}-count`} className="text-sm font-bold opacity-70 text-right">
                    {comment.length} / {MAX}
                </p>
            </div>

            <div className="flex flex-wrap gap-3">
                <button type="submit" disabled={saving} className={btn(darkMode, "primary")}>
                    {saving ? "Saving..." : initial ? "Save review" : "Post review"}
                </button>
                {onCancel && (
                    <button type="button" onClick={onCancel} className={btn(darkMode, "secondary")}>
                        Cancel
                    </button>
                )}
            </div>
        </form>
    );
}

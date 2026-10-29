import { useRef, useState } from "react";
import { productImage, PLACEHOLDER } from "../utils/image.js";
import { focusRing } from "../utils/ui.js";

/**
 * Main image + thumbnail strip. Thumbnails are buttons (Tab/Enter work) and the
 * strip also takes Left/Right/Home/End to move between images.
 */
export default function ProductGallery({ name, imageUrl, images = [] }) {
    const all = [imageUrl, ...(images || [])].filter(
        (url, i, arr) => url && arr.indexOf(url) === i,
    );
    const [index, setIndex] = useState(0);
    const thumbRefs = useRef([]);
    const current = Math.min(index, Math.max(all.length - 1, 0));

    const go = (i) => {
        setIndex(i);
        thumbRefs.current[i]?.focus();
    };

    const onKeyDown = (e) => {
        const last = all.length - 1;
        if (e.key === "ArrowRight") go(current === last ? 0 : current + 1);
        else if (e.key === "ArrowLeft") go(current === 0 ? last : current - 1);
        else if (e.key === "Home") go(0);
        else if (e.key === "End") go(last);
        else return;
        e.preventDefault();
    };

    return (
        <div className="flex flex-col gap-4 min-w-0">
            <div className="overflow-hidden rounded-lg border-4 border-black aspect-square bg-white">
                <img
                    key={all[current]}
                    src={productImage(all[current], 800)}
                    alt={all.length > 1 ? `${name}, image ${current + 1} of ${all.length}` : name}
                    width={800}
                    height={800}
                    className="w-full h-full object-cover"
                    onError={(e) => {
                        e.target.src = PLACEHOLDER;
                    }}
                />
            </div>

            {all.length > 1 && (
                <div
                    role="group"
                    aria-label={`${name} images. Use the arrow keys to switch.`}
                    onKeyDown={onKeyDown}
                    className="flex gap-3 overflow-x-auto p-1 -m-1"
                >
                    {all.map((url, i) => {
                        const selected = i === current;
                        return (
                            <button
                                key={url}
                                ref={(el) => (thumbRefs.current[i] = el)}
                                type="button"
                                onClick={() => setIndex(i)}
                                aria-label={`Show image ${i + 1} of ${all.length}`}
                                aria-pressed={selected}
                                tabIndex={selected ? 0 : -1}
                                className={`shrink-0 w-16 h-16 sm:w-20 sm:h-20 rounded-lg overflow-hidden border-4 cursor-pointer ${focusRing}
                                ${selected ? "border-black shadow-[4px_4px_0px_0px_rgba(0,0,0,1)]" : "border-transparent opacity-70 hover:opacity-100"}`}
                            >
                                <img
                                    src={productImage(url, 160)}
                                    alt=""
                                    width={80}
                                    height={80}
                                    loading="lazy"
                                    className="w-full h-full object-cover"
                                    onError={(e) => {
                                        e.target.src = PLACEHOLDER;
                                    }}
                                />
                            </button>
                        );
                    })}
                </div>
            )}
        </div>
    );
}

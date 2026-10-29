import { useEffect, useId, useState } from "react";
import { toast } from "sonner";
import { CaretLeftIcon, CaretRightIcon, CircleNotchIcon, XIcon } from "@phosphor-icons/react";
import api from "../api/axios";
import { productImage, PLACEHOLDER } from "../utils/image.js";
import { focusRing } from "../utils/ui.js";

const MAX_GALLERY_IMAGES = 6;
const MAX_BYTES = 5 * 1024 * 1024;

/**
 * Extra product images (the main image stays separate). Each file is uploaded
 * straight away through POST /upload-image, so `images` is always a list of URLs
 * ready to send in the ProductDTO. Order is kept; arrows move an image.
 */
export default function GalleryUploader({ images, onChange, onBusyChange, disabled = false }) {
    const inputId = useId();
    const [uploading, setUploading] = useState(0);

    // Lets the form hold its submit button until every upload has finished.
    useEffect(() => {
        onBusyChange?.(uploading > 0);
    }, [uploading, onBusyChange]);
    const slotsLeft = MAX_GALLERY_IMAGES - images.length - uploading;

    const handleFiles = async (e) => {
        const files = Array.from(e.target.files || []);
        e.target.value = ""; // allow picking the same file again
        if (files.length === 0) return;

        if (files.length > slotsLeft) {
            toast.error(`You can add ${Math.max(slotsLeft, 0)} more image(s). Up to ${MAX_GALLERY_IMAGES} in total.`);
        }
        const accepted = files.slice(0, Math.max(slotsLeft, 0)).filter((f) => {
            if (f.size > MAX_BYTES) {
                toast.error(`${f.name} is over 5 MB`);
                return false;
            }
            return true;
        });
        if (accepted.length === 0) return;

        setUploading((n) => n + accepted.length);
        let added = [];
        // One at a time so the gallery keeps the order the files were picked in.
        for (const file of accepted) {
            try {
                const formData = new FormData();
                formData.append("image", file);
                const res = await api.post("/upload-image", formData, {
                    headers: { "Content-Type": "multipart/form-data" },
                });
                added = [...added, res.data];
            } catch {
                toast.error(`Could not upload ${file.name}`);
            } finally {
                setUploading((n) => n - 1);
            }
        }
        if (added.length) onChange((current) => [...current, ...added].slice(0, MAX_GALLERY_IMAGES));
    };

    const move = (i, delta) => {
        onChange((current) => {
            const next = [...current];
            const j = i + delta;
            if (j < 0 || j >= next.length) return current;
            [next[i], next[j]] = [next[j], next[i]];
            return next;
        });
    };

    const remove = (i) => onChange((current) => current.filter((_, idx) => idx !== i));

    const smallBtn = `w-11 h-11 flex items-center justify-center rounded border-2 border-black bg-white text-black hover:bg-gray-100 disabled:opacity-40 ${focusRing}`;

    return (
        <div>
            <label htmlFor={inputId} className="block text-black font-semibold mb-2">
                Gallery images (optional, up to {MAX_GALLERY_IMAGES})
            </label>
            <input
                id={inputId}
                type="file"
                accept="image/*"
                multiple
                onChange={handleFiles}
                disabled={disabled || slotsLeft <= 0}
                className={`block w-full text-sm text-gray-700 file:mr-4 file:min-h-[44px] file:py-2 file:px-4 file:rounded-xl file:border-2 file:border-black file:text-sm file:font-semibold file:bg-[#d6d6d6] hover:file:bg-gray-200 disabled:opacity-50 ${focusRing}`}
            />
            <p className="mt-1 text-sm text-gray-700">
                {images.length} of {MAX_GALLERY_IMAGES} added. Each image must be under 5 MB.
            </p>

            {(images.length > 0 || uploading > 0) && (
                <ul className="mt-3 grid grid-cols-2 sm:grid-cols-3 gap-3">
                    {images.map((url, i) => (
                        <li key={url} className="border-2 border-black rounded-lg p-2 bg-white">
                            <img
                                src={productImage(url, 240)}
                                alt={`Gallery image ${i + 1}`}
                                width={120}
                                height={96}
                                className="w-full h-24 object-cover rounded border-2 border-black"
                                onError={(e) => {
                                    e.target.src = PLACEHOLDER;
                                }}
                            />
                            <div className="mt-2 flex justify-between gap-1">
                                <button
                                    type="button"
                                    onClick={() => move(i, -1)}
                                    disabled={i === 0}
                                    aria-label={`Move image ${i + 1} earlier`}
                                    className={smallBtn}
                                >
                                    <CaretLeftIcon size={16} />
                                </button>
                                <button
                                    type="button"
                                    onClick={() => remove(i)}
                                    aria-label={`Remove image ${i + 1}`}
                                    className={`w-11 h-11 flex items-center justify-center rounded border-2 border-black bg-red-500 text-white hover:bg-red-600 ${focusRing}`}
                                >
                                    <XIcon size={16} />
                                </button>
                                <button
                                    type="button"
                                    onClick={() => move(i, 1)}
                                    disabled={i === images.length - 1}
                                    aria-label={`Move image ${i + 1} later`}
                                    className={smallBtn}
                                >
                                    <CaretRightIcon size={16} />
                                </button>
                            </div>
                        </li>
                    ))}
                    {Array.from({ length: uploading }).map((_, i) => (
                        <li
                            key={`uploading-${i}`}
                            className="border-2 border-dashed border-black rounded-lg h-[152px] flex flex-col items-center justify-center gap-2 text-black"
                            role="status"
                        >
                            <CircleNotchIcon size={24} className="animate-spin" />
                            <span className="text-sm font-bold">Uploading...</span>
                        </li>
                    ))}
                </ul>
            )}
        </div>
    );
}

import { useState, useEffect } from 'react';
import type { ChangeEvent, FormEvent } from 'react';
import { isAxiosError } from 'axios';
import { useParams, useNavigate } from 'react-router';
import { toast } from 'sonner';
import api from '../api/axios';
import type { Product, ProductCategory, ProductPayload } from '../api/types';
import { queryClient } from '../api/queryClient';
import { PRODUCT_CATEGORIES } from '../constants/categories';
import GalleryUploader from '../components/GalleryUploader';
import LoadingState from '../components/LoadingState';
import ErrorState from '../components/ErrorState';
import { productImage } from '../utils/image';

// Price and quantity stay strings while the user types; they become numbers on submit.
interface ProductForm {
    name: string;
    description: string;
    brand: string;
    price: string;
    category: ProductCategory | '';
    available: boolean;
    quantity: string;
    imageUrl: string;
}

export default function UpdateProduct() {
    const { id } = useParams();
    const navigate = useNavigate();

    const [product, setProduct] = useState<ProductForm>({
        name: '', description: '', brand: '', price: '', category: '',
        available: false, quantity: '', imageUrl: ''
    });

    const [image, setImage] = useState<File | null>(null);
    const [preview, setPreview] = useState<string | null>(null);
    const [loading, setLoading] = useState(true);
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [error, setError] = useState('');
    const [images, setImages] = useState<string[]>([]);
    const [galleryBusy, setGalleryBusy] = useState(false);

    useEffect(() => {
        api.get<Product>(`/product/${id}`)
            .then((res) => {
                const p = res.data;
                setProduct({
                    name: p.name,
                    description: p.description ?? '',
                    brand: p.brand ?? '',
                    price: String(p.price),
                    category: p.category,
                    available: p.available,
                    quantity: String(p.quantity),
                    imageUrl: p.imageUrl ?? '',
                });
                setImages(Array.isArray(p.images) ? p.images : []);
                if (p.imageUrl) setPreview(productImage(p.imageUrl, 800));
                setLoading(false);
            })
            .catch(() => {
                setError('Product not found');
                setLoading(false);
            });
    }, [id]);

    const handleImageChange = (e: ChangeEvent<HTMLInputElement>) => {
        const file = e.target.files?.[0];
        if (!file) return;

        const maxSize = 4 * 1024 * 1024;
        const sizeInMB = (file.size / (1024 * 1024)).toFixed(2);

        if (file.size > maxSize) {
            toast.error(`Image size is ${sizeInMB} MB. Max allowed: 4 MB`);
            return;
        }

        setImage(file);
        setPreview(URL.createObjectURL(file));
    };

    const handleSubmit = async (e: FormEvent<HTMLFormElement>) => {
        e.preventDefault();
        if (isSubmitting) return;
        if (galleryBusy) {
            toast.error('Wait for the gallery images to finish uploading');
            return;
        }
        if (product.category === '') return; // the select is required, so this only narrows the type
        setIsSubmitting(true);

        try {
            const updatedProduct: ProductPayload = {
                name: product.name,
                description: product.description,
                brand: product.brand,
                price: Number(product.price),
                category: product.category,
                available: product.available,
                quantity: product.quantity === '' ? undefined : Number(product.quantity),
                imageUrl: product.imageUrl || undefined,
                images,
            };

            if (image) {
                const imgForm = new FormData();
                imgForm.append("image", image);

                const res = await api.post<string>("/upload-image", imgForm, {
                    headers: { 'Content-Type': 'multipart/form-data' }
                });

                updatedProduct.imageUrl = res.data; // returned URL
            }

            await api.put(`/product/${id}`, updatedProduct);
            void queryClient.invalidateQueries({ queryKey: ["products"] });
            void queryClient.invalidateQueries({ queryKey: ["admin"] });
            toast.success('Product updated!');
            void navigate(`/product/${id}`);
        } catch (err) {
            console.error('Error updating:', err);
            const status = isAxiosError(err) ? err.response?.status : undefined;

            if (status === 403) toast.error('Not authorized.');
            else if (status === 404) toast.error('Product not found.');
            else toast.error('Update failed.');
        } finally {
            setIsSubmitting(false);
        }
    };

    const handleChange = (
        e: ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>,
    ) => {
        const { name, value, type } = e.target;
        const checked = e.target instanceof HTMLInputElement && e.target.checked;
        setProduct((prev) => ({ ...prev, [name]: type === "checkbox" ? checked : value }));
    };

    if (loading) return <LoadingState variant="page" />;
    if (error) return <ErrorState error={error} />;

    return (
        <div className="min-h-screen flex items-center justify-center p-4">
            <form onSubmit={handleSubmit} className="bg-white border-4 border-black rounded-3xl shadow-[8px_8px_0px_0px_black] p-8 w-full max-w-lg space-y-6">
                <h1 className="text-3xl font-extrabold text-center mb-6 text-black">Update Product</h1>

                {preview && (
                    <div className="border-4 border-black rounded-lg p-2">
                        <img src={preview} alt="Preview" className="w-full h-64 object-cover rounded-lg" />
                        <p className="text-center mt-2 text-sm">{image ? "New Image Preview" : "Current Image"}</p>
                    </div>
                )}

                <div>
                    <label htmlFor="product-image" className="block font-semibold mb-2">Update Image (optional)</label>
                    <input
                        id="product-image"
                        type="file"
                        onChange={handleImageChange}
                        accept="image/*"
                        className="block w-full text-sm text-gray-500 file:mr-4 file:py-2 file:px-4 file:rounded-xl file:border-2 file:border-black file:text-sm file:font-semibold file:bg-[#d6d6d6] hover:file:bg-gray-200"
                    />
                </div>

                <div>
                    <label htmlFor="product-name" className="block font-semibold mb-2">Product Name</label>
                    <input
                        id="product-name"
                        type="text"
                        name="name"
                        value={product.name}
                        onChange={handleChange}
                        className="w-full p-3 border-2 border-black rounded-xl bg-[#e0e0e0]"
                        required
                    />
                </div>

                <div>
                    <label htmlFor="product-description" className="block font-semibold mb-2">Description</label>
                    <textarea
                        id="product-description"
                        name="description"
                        value={product.description}
                        onChange={handleChange}
                        className="w-full p-3 border-2 border-black rounded-xl bg-[#e0e0e0]"
                        required
                    />
                </div>

                <div>
                    <label htmlFor="product-brand" className="block font-semibold mb-2">Brand</label>
                    <input
                        id="product-brand"
                        type="text"
                        name="brand"
                        value={product.brand}
                        onChange={handleChange}
                        className="w-full p-3 border-2 border-black rounded-xl bg-[#e0e0e0]"
                        required
                    />
                </div>

                <div>
                    <label htmlFor="product-price" className="block font-semibold mb-2">Price</label>
                    <input
                        id="product-price"
                        type="number"
                        name="price"
                        value={product.price}
                        onChange={handleChange}
                        className="w-full p-3 border-2 border-black rounded-xl bg-[#e0e0e0]"
                        required
                    />
                </div>

                <div>
                    <label htmlFor="product-category" className="block font-semibold mb-2">Category</label>
                    <select
                        id="product-category"
                        name="category"
                        value={product.category}
                        onChange={handleChange}
                        className="w-full p-3 border-2 border-black rounded-xl bg-[#e0e0e0] focus:outline-none focus-visible:outline-4 focus-visible:outline-offset-2 focus-visible:outline-rose-500"
                        required
                    >
                        <option value="">Select a category</option>
                        {PRODUCT_CATEGORIES.map((c) => (
                            <option key={c.value} value={c.value}>
                                {c.label}
                            </option>
                        ))}
                    </select>
                </div>

                <div>
                    <label htmlFor="product-available" className="block font-semibold mb-2">Available</label>
                    <div className="flex items-center space-x-2">
                        <input
                            id="product-available"
                            type="checkbox"
                            name="available"
                            checked={product.available}
                            onChange={handleChange}
                            className="w-5 h-5 border-2 border-black"
                        />
                        <span className="text-black">In Stock</span>
                    </div>
                </div>

                {product.available && (
                    <div>
                        <label htmlFor="product-quantity" className="block font-semibold mb-2">Quantity</label>
                        <input
                            id="product-quantity"
                            type="number"
                            name="quantity"
                            value={product.quantity}
                            onChange={handleChange}
                            className="w-full p-3 border-2 border-black rounded-xl bg-[#e0e0e0]"
                            required
                        />
                    </div>
                )}

                <GalleryUploader
                    images={images}
                    onChange={setImages}
                    onBusyChange={setGalleryBusy}
                    disabled={isSubmitting}
                />

                <div className="flex gap-4">
                    <button
                        type="button"
                        onClick={() => navigate(-1)}
                        className="w-full py-3 px-6 bg-gray-500 text-white font-bold rounded-xl shadow-[4px_4px_0px_0px_black] hover:bg-gray-600 transition"
                    >
                        Cancel
                    </button>
                    <button
                        type="submit"
                        disabled={isSubmitting || galleryBusy}
                        className={`w-full py-3 px-6 font-bold rounded-xl shadow-[4px_4px_0px_0px_black] transition
                        ${isSubmitting
                            ? "bg-gray-400 cursor-not-allowed"
                            : "bg-black text-white hover:bg-gray-800"} 
                        `}
                    >
                        {isSubmitting ? "Updating..." : "Update"}
                    </button>
                </div>
            </form>
        </div>
    );
}
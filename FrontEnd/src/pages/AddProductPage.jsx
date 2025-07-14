// src/pages/AddProduct.jsx

import { useState } from "react";
import api from '../api/axios';

export default function AddProduct() {
    const [product, setProduct] = useState({
        name: "",
        description: "",
        brand: "",
        price: "",
        category: "",
        available: false,
        quantity: "",
        createdAt: "",
    });
    const [image, setImage] = useState(null);
    const [preview, setPreview] = useState(null);
    const [fileSize, setFileSize] = useState(null);
    const [isSubmitting, setIsSubmitting] = useState(false);

    const handleChange = (e) => {
        const { name, value, type, checked } = e.target;
        setProduct(prev => ({
            ...prev,
            [name]: type === "checkbox" ? checked : value
        }));
    };

    const handleImageChange = (e) => {
        const file = e.target.files[0];
        if (!file) return;

        const maxSize = 5 * 1024 * 1024; // 5MB limit
        const sizeInMB = (file.size / (1024 * 1024)).toFixed(2);

        if (file.size > maxSize) {
            alert(`File size is ${sizeInMB} MB. Please upload a file smaller than 5 MB.`);
            setImage(null);
            setPreview(null);
            setFileSize(null);
            return;
        }

        setImage(file);
        setPreview(URL.createObjectURL(file));
        setFileSize(sizeInMB);
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        if (isSubmitting) return;
        setIsSubmitting(true);

        try {
            let imageUrl = null;

            if (image) {
                const formData = new FormData();
                formData.append("image", image);

                const imageRes = await api.post("/upload-image", formData, {
                    headers: { 'Content-Type': 'multipart/form-data' }
                });

                if (imageRes.status === 200) {
                    imageUrl = imageRes.data;
                } else {
                    alert("Image upload failed");
                    setIsSubmitting(false);
                    return;
                }
            }

            const productWithImage = {
                ...product,
                createdAt: new Date().toISOString(),
                imageUrl,
            };

            const response = await api.post("/product", productWithImage);
            if (response.status === 201) {
                alert("Product added!");
                setProduct({
                    name: "", description: "", brand: "", price: "",
                    category: "", available: false, quantity: "", createdAt: "",
                });
                setImage(null); setPreview(null); setFileSize(null);
            } else {
                alert("Failed to add product.");
            }
        } catch (err) {
            console.error(err);
            alert("Something went wrong!");
        } finally {
            setIsSubmitting(false);
        }
    };

    return (
        <div className="min-h-screen flex items-center justify-center p-4">
            <form
                onSubmit={handleSubmit}
                className="bg-white border-4 border-black rounded-3xl shadow-[8px_8px_0px_0px_black] p-8 w-full max-w-lg space-y-6"
            >
                <h1 className="text-3xl font-extrabold text-center mb-6 text-black">
                    {isSubmitting ? "Adding Product..." : "Add New Product"}
                </h1>

                <div>
                    <label className="block text-black font-semibold mb-2">Product Name</label>
                    <input
                        type="text"
                        name="name"
                        value={product.name}
                        onChange={handleChange}
                        className="w-full p-3 border-2 border-black rounded-xl bg-[#e0e0e0] focus:outline-none"
                        required
                    />
                </div>

                <div>
                    <label className="block text-black font-semibold mb-2">Description</label>
                    <textarea
                        name="description"
                        value={product.description}
                        onChange={handleChange}
                        className="w-full p-3 border-2 border-black rounded-xl bg-[#e0e0e0] focus:outline-none"
                        required
                    />
                </div>

                <div>
                    <label className="block text-black font-semibold mb-2">Brand</label>
                    <input
                        type="text"
                        name="brand"
                        value={product.brand}
                        onChange={handleChange}
                        className="w-full p-3 border-2 border-black rounded-xl bg-[#e0e0e0] focus:outline-none"
                        required
                    />
                </div>

                <div>
                    <label className="block text-black font-semibold mb-2">Price</label>
                    <input
                        type="number"
                        name="price"
                        value={product.price}
                        onChange={handleChange}
                        className="w-full p-3 border-2 border-black rounded-xl bg-[#e0e0e0] focus:outline-none"
                        required
                    />
                </div>

                <div>
                    <label className="block text-black font-semibold mb-2">Category</label>
                    <select
                        name="category"
                        value={product.category}
                        onChange={handleChange}
                        className="w-full p-3 border-2 border-black rounded-xl bg-[#e0e0e0] focus:outline-none"
                        required
                    >
                        <option value="">Select a category</option>
                        <option value="ELECTRONICS">Electronics</option>
                        <option value="FASHION">Fashion</option>
                        <option value="HOME_KITCHEN">Home & Kitchen</option>
                        <option value="BEAUTY_PERSONAL_CARE">Beauty & Personal Care</option>
                        <option value="BOOKS_STATIONERY">Books & Stationery</option>
                        <option value="HEALTH_WELLNESS">Health & Wellness</option>
                        <option value="TOYS_GAMES">Toys & Games</option>
                        <option value="SPORTS_OUTDOORS">Sports & Outdoors</option>
                        <option value="AUTOMOTIVE">Automotive</option>
                        <option value="GROCERIES_GOURMET_FOOD">Groceries & Gourmet Food</option>
                    </select>
                </div>

                <div>
                    <label className="block text-black font-semibold mb-2">Available</label>
                    <div className="flex items-center space-x-2">
                        <input
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
                        <label className="block text-black font-semibold mb-2">Quantity</label>
                        <input
                            type="number"
                            name="quantity"
                            value={product.quantity}
                            onChange={handleChange}
                            className="w-full p-3 border-2 border-black rounded-xl bg-[#e0e0e0] focus:outline-none"
                            required
                        />
                    </div>
                )}

                <div>
                    <label className="block text-black font-semibold mb-2">Upload Image</label>
                    <input
                        type="file"
                        onChange={handleImageChange}
                        className="block w-full text-sm text-gray-500 file:mr-4 file:py-2 file:px-4 file:rounded-xl file:border-2 file:border-black file:text-sm file:font-semibold file:bg-[#d6d6d6] hover:file:bg-gray-200"
                        accept="image/*"
                        required
                    />
                    {preview && (
                        <div className="mt-4">
                            <img
                                src={preview}
                                alt="Preview"
                                className="w-full h-64 object-cover border-2 border-black rounded-lg"
                            />
                            <p className="mt-2 text-sm text-gray-700">
                                File Size: {fileSize} MB
                            </p>
                        </div>
                    )}
                </div>

                <button
                    type="submit"
                    disabled={isSubmitting}
                    className={`w-full py-3 px-6 font-bold rounded-xl shadow-[4px_4px_0px_0px_black] transition
                    ${isSubmitting
                        ? "bg-gray-400 cursor-not-allowed"
                        : "bg-black text-white hover:bg-gray-800"}
                    `}
                >
                    {isSubmitting ? "Adding..." : "Add Product"}
                </button>
            </form>
        </div>
    );
}

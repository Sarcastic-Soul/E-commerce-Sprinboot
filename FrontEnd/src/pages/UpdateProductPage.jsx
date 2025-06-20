// src/pages/UpdateProduct.jsx
import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { toast } from 'sonner';
import api from '../api/axios';

export default function UpdateProduct() {
    const { id } = useParams();
    const navigate = useNavigate();
    const [product, setProduct] = useState({
        name: '',
        description: '',
        brand: '',
        price: '',
        category: '',
        available: false,
        quantity: '',
        createdAt: ''
    });
    const [image, setImage] = useState(null);
    const [preview, setPreview] = useState(null);
    const [fileSize, setFileSize] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');

    useEffect(() => {
        const fetchProduct = async () => {
            try {
                const response = await api.get(`/product/${id}`);
                const data = response.data; // ✅ directly use .data
                setProduct(data);
                if (data.imageData) {
                    setPreview(`data:image/jpeg;base64,${data.imageData}`);
                }
                setLoading(false);
            } catch (err) {
                console.error('Error fetching product:', err);
                setError('Product not found');
                setLoading(false);
            }
        };
        fetchProduct();
    }, [id]);

    const handleImageChange = (e) => {
        const file = e.target.files[0];
        if (!file) return;

        const maxSize = 2 * 1024 * 1024; // 2MB limit
        const sizeInMB = (file.size / (1024 * 1024)).toFixed(2);

        if (file.size > maxSize) {
            alert(`File size is ${sizeInMB} MB. Please upload a file smaller than 2 MB.`);
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

        try {
            const formData = new FormData();

            // Create a JSON blob from the product object
            const productBlob = new Blob(
                [JSON.stringify(product)],
                { type: "application/json" }
            );

            formData.append("product", productBlob);

            if (image) {
                formData.append("image", image);
            }

            // Send PUT request with multipart/form-data
            const response = await api.put(`/product/${id}`, formData, {
                headers: {
                    'Content-Type': 'multipart/form-data',
                },
            });

            toast.success('Product updated successfully!');
            navigate(`/product/${id}`);
        } catch (error) {
            console.error('Update error:', error);

            if (error.response?.status === 403) {
                toast.error('You are not authorized to update this product.');
            } else if (error.response?.status === 404) {
                toast.error('Product not found.');
            } else {
                toast.error('Failed to update product. Please try again.');
            }
        }
    };


    const handleChange = (e) => {
        const { name, value, type, checked } = e.target;
        setProduct(prev => ({
            ...prev,
            [name]: type === 'checkbox' ? checked : value
        }));
    };

    if (loading) return <div>Loading...</div>;
    if (error) return <div>Error: {error}</div>;

    return (
        <div className="min-h-screen flex items-center justify-center p-4">
            <form onSubmit={handleSubmit} className="bg-white border-4 border-black rounded-3xl shadow-[8px_8px_0px_0px_black] p-8 w-full max-w-lg space-y-6">
                <h1 className="text-3xl font-extrabold text-center mb-6 text-black">Update Product</h1>

                {/* Existing Image Preview */}
                {product.imageData && !preview && (
                    <div className="border-4 border-black rounded-lg p-2">
                        <img
                            src={`data:image/jpeg;base64,${product.imageData}`}
                            alt="Current Product"
                            className="w-full h-64 object-cover rounded-lg"
                        />
                        <p className="text-center mt-2 text-sm">Current Image</p>
                    </div>
                )}

                {/* Image Upload Section */}
                <div>
                    <label className="block text-black font-semibold mb-2">
                        {image ? 'New Image Preview' : 'Update Image (optional)'}
                    </label>
                    <input
                        type="file"
                        onChange={handleImageChange}
                        className="block w-full text-sm text-gray-500 file:mr-4 file:py-2 file:px-4 file:rounded-xl file:border-2 file:border-black file:text-sm file:font-semibold file:bg-[#d6d6d6] hover:file:bg-gray-200"
                        accept="image/*"
                    />
                    {preview && (
                        <div className="mt-4">
                            <img
                                src={preview}
                                alt="New Preview"
                                className="w-40 h-40 object-cover border-2 border-black rounded-xl"
                            />
                            <p className="mt-2 text-sm text-gray-700">
                                New File Size: {fileSize} MB
                            </p>
                        </div>
                    )}
                </div>

                {/* Rest of your form fields */}
                <input
                    type="text"
                    name="name"
                    value={product.name}
                    onChange={handleChange}
                    placeholder="Product Name"
                    className="w-full p-3 border-2 border-black rounded-xl bg-[#e0e0e0] focus:outline-none"
                    required
                />

                {/* ... other form fields ... */}
                <textarea
                    name="description"
                    value={product.description}
                    onChange={handleChange}
                    placeholder="Product Description"
                    className="w-full p-3 border-2 border-black rounded-xl bg-[#e0e0e0] focus:outline-none"
                    required
                />

                <input
                    type="text"
                    name="brand"
                    value={product.brand}
                    onChange={handleChange}
                    placeholder="Brand"
                    className="w-full p-3 border-2 border-black rounded-xl bg-[#e0e0e0] focus:outline-none"
                    required
                />

                <input
                    type="number"
                    name="price"
                    value={product.price}
                    onChange={handleChange}
                    placeholder="Price"
                    className="w-full p-3 border-2 border-black rounded-xl bg-[#e0e0e0] focus:outline-none"
                    required
                />

                <input
                    type="text"
                    name="category"
                    value={product.category}
                    onChange={handleChange}
                    placeholder="Category"
                    className="w-full p-3 border-2 border-black rounded-xl bg-[#e0e0e0] focus:outline-none"
                    required
                />

                <div className="flex items-center space-x-2">
                    <input
                        type="checkbox"
                        name="available"
                        checked={product.available}
                        onChange={handleChange}
                        className="w-5 h-5 border-2 border-black"
                    />
                    <label className="text-black font-semibold">Available</label>
                </div>

                <input
                    type="number"
                    name="quantity"
                    value={product.quantity}
                    onChange={handleChange}
                    placeholder="Quantity"
                    className="w-full p-3 border-2 border-black rounded-xl bg-[#e0e0e0] focus:outline-none"
                    required
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
                        className="w-full py-3 px-6 bg-black text-white font-bold rounded-xl shadow-[4px_4px_0px_0px_black] hover:bg-gray-800 transition"
                    >
                        Update Product
                    </button>
                </div>
            </form>
        </div>
    );
}
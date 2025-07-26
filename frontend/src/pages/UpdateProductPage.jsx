import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { toast } from 'sonner';
import api from '../api/axios';

export default function UpdateProduct() {
    const { id } = useParams();
    const navigate = useNavigate();

    const [product, setProduct] = useState({
        name: '', description: '', brand: '', price: '', category: '',
        available: false, quantity: '', imageUrl: '', createdAt: ''
    });

    const [image, setImage] = useState(null);
    const [preview, setPreview] = useState(null);
    const [fileSize, setFileSize] = useState(null);
    const [loading, setLoading] = useState(true);
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [error, setError] = useState('');

    useEffect(() => {
        api.get(`/product/${id}`)
            .then((res) => {
                setProduct(res.data);
                if (res.data.imageUrl) setPreview(res.data.imageUrl);
                setLoading(false);
            })
            .catch((err) => {
                setError('Product not found');
                setLoading(false);
            });
    }, [id]);

    const handleImageChange = (e) => {
        const file = e.target.files[0];
        if (!file) return;

        const maxSize = 5 * 1024 * 1024;
        const sizeInMB = (file.size / (1024 * 1024)).toFixed(2);

        if (file.size > maxSize) {
            toast.error(`Image size is ${sizeInMB} MB. Max allowed: 5 MB`);
            return;
        }

        setImage(file);
        setPreview(URL.createObjectURL(file));
        setFileSize(sizeInMB);
    };

    const handleSubmit = async (e) => {
        e.preventDefault();

        try {
            let updatedProduct = { ...product };

            if (image) {
                const imgForm = new FormData();
                imgForm.append("image", image);

                const res = await api.post("/upload-image", imgForm, {
                    headers: { 'Content-Type': 'multipart/form-data' }
                });

                updatedProduct.imageUrl = res.data; // returned URL
            }

            await api.put(`/product/${id}`, updatedProduct);
            toast.success('Product updated!');
            navigate(`/product/${id}`);
        } catch (error) {
            console.error('Error updating:', error);
            const status = error.response?.status;

            if (status === 403) toast.error('Not authorized.');
            else if (status === 404) toast.error('Product not found.');
            else toast.error('Update failed.');
        }
    };

    const handleChange = (e) => {
        const { name, value, type, checked } = e.target;
        setProduct((prev) => ({ ...prev, [name]: type === "checkbox" ? checked : value }));
    };

    if (loading) return <div>Loading...</div>;
    if (error) return <div>Error: {error}</div>;

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
                    <label className="block font-semibold mb-2">Update Image (optional)</label>
                    <input
                        type="file"
                        onChange={handleImageChange}
                        accept="image/*"
                        className="block w-full text-sm text-gray-500 file:mr-4 file:py-2 file:px-4 file:rounded-xl file:border-2 file:border-black file:text-sm file:font-semibold file:bg-[#d6d6d6] hover:file:bg-gray-200"
                    />
                </div>

                <div>
                    <label className="block font-semibold mb-2">Product Name</label>
                    <input
                        type="text"
                        name="name"
                        value={product.name}
                        onChange={handleChange}
                        className="w-full p-3 border-2 border-black rounded-xl bg-[#e0e0e0]"
                        required
                    />
                </div>

                <div>
                    <label className="block font-semibold mb-2">Description</label>
                    <textarea
                        name="description"
                        value={product.description}
                        onChange={handleChange}
                        className="w-full p-3 border-2 border-black rounded-xl bg-[#e0e0e0]"
                        required
                    />
                </div>

                <div>
                    <label className="block font-semibold mb-2">Brand</label>
                    <input
                        type="text"
                        name="brand"
                        value={product.brand}
                        onChange={handleChange}
                        className="w-full p-3 border-2 border-black rounded-xl bg-[#e0e0e0]"
                        required
                    />
                </div>

                <div>
                    <label className="block font-semibold mb-2">Price</label>
                    <input
                        type="number"
                        name="price"
                        value={product.price}
                        onChange={handleChange}
                        className="w-full p-3 border-2 border-black rounded-xl bg-[#e0e0e0]"
                        required
                    />
                </div>

                <div>
                    <label className="block font-semibold mb-2">Category</label>
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
                    <label className="block font-semibold mb-2">Available</label>
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
                        <label className="block font-semibold mb-2">Quantity</label>
                        <input
                            type="number"
                            name="quantity"
                            value={product.quantity}
                            onChange={handleChange}
                            className="w-full p-3 border-2 border-black rounded-xl bg-[#e0e0e0]"
                            required
                        />
                    </div>
                )}

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
                        disabled={isSubmitting}
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
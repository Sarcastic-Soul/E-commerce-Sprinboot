import { useNavigate } from "react-router-dom";
import { Edit, ShoppingCart, Trash } from "lucide-react";
import api from '../api/axios';
import { useAuth } from '../context/AuthContext';
import { useTheme } from '../context/ThemeContext';
import { toast } from 'sonner';

// src/components/ProductDetail.jsx
export default function ProductDetail({ product, addToCart }) {
    const { darkMode } = useTheme();
    const { user } = useAuth();
    const isAdmin = user?.role === 'ADMIN';
    const navigate = useNavigate();
    const imageUrl = product.imageData
        ? `data:image/jpeg;base64,${product.imageData}`
        : 'https://picsum.photos/400';
    const createdAtDate = new Date(product.createdAt);
    const formattedCreatedAtDate = `${String(createdAtDate.getDate()).padStart(2, '0')}-${String(createdAtDate.getMonth() + 1).padStart(2, '0')}-${createdAtDate.getFullYear()}`;

    const handleDelete = async () => {
        if (!window.confirm('Are you sure you want to delete this product?')) return;

        try {
            await api.delete(`/product/${product.id}`);
            toast.success("Product deleted successfully!");
            navigate('/');
        } catch (error) {
            console.error('Delete error:', error);
            alert('Something went wrong!');
        }
    };

    return (
        <div>
            <div className={`grid md:grid-cols-2 gap-8 rounded-lg overflow-hidden border-4 p-6 shadow-lg
                ${darkMode ? 'bg-gray-800 border-gray-700' : 'bg-white border-black'}`}>
                <div className="overflow-hidden rounded-lg border-4 border-black">
                    <img
                        src={imageUrl}
                        alt={product.name}
                        loading="lazy"
                        className="w-full h-full object-cover"
                        onError={(e) => {
                            e.target.src = 'https://picsum.photos/400';
                        }}
                    />
                </div>

                <div>
                    <div className="mb-6">
                        <h2 className="text-3xl font-black">{product.name}</h2>
                        <div className={`inline-block px-3 py-1 rounded-lg text-sm font-bold mb-4
                            ${darkMode ? 'bg-gray-700' : 'bg-yellow-300'}`}>
                            {product.category}
                        </div>
                        <p className="text-3xl font-black mb-4">${product.price.toFixed(2)}</p>

                        <div className={`p-4 mb-4 rounded-lg ${darkMode ? 'bg-gray-700' : 'bg-gray-100'}`}>
                            <p>{product.description}</p>
                        </div>
                    </div>

                    <div className="grid grid-cols-2 gap-4 mb-6">
                        <div className={`p-4 rounded-lg ${darkMode ? 'bg-gray-700' : 'bg-gray-100'}`}>
                            <p className="text-sm opacity-70">Product ID</p>
                            <p className="font-bold">{product.id}</p>
                        </div>
                        <div className={`p-4 rounded-lg ${darkMode ? 'bg-gray-700' : 'bg-gray-100'}`}>
                            <p className="text-sm opacity-70">Creation Date</p>
                            <p className="font-bold">{formattedCreatedAtDate}</p>
                        </div>
                        <div className={`p-4 rounded-lg ${darkMode ? 'bg-gray-700' : 'bg-gray-100'}`}>
                            <p className="text-sm opacity-70">Available Quantity</p>
                            <p className="font-bold">{product.quantity}</p>
                        </div>
                        <div className={`p-4 rounded-lg ${darkMode ? 'bg-gray-700' : 'bg-gray-100'}`}>
                            <p className="text-sm opacity-70">Status</p>
                            <p className={`font-bold ${product.available ? 'text-green-500' : 'text-red-500'}`}>
                                {product.available ? 'In Stock' : 'Out of Stock'}
                            </p>
                        </div>
                    </div>

                    <button
                        onClick={() => {
                            if (!user) {
                                toast.error("Login required");
                                return navigate("/login");
                            }
                            if (product.available) addToCart(product);
                            toast.success("Item added to cart");
                        }}
                        disabled={!product.available}
                        className={`w-full py-3 px-6 rounded-lg font-bold text-lg flex items-center justify-center gap-2 border-4 border-black transition-colors duration-300 cursor-pointer
                            shadow-[8px_8px_0px_0px_rgba(0,0,0,1)]
                            ${product.available
                                ? darkMode
                                    ? 'bg-rose-600 hover:bg-rose-700'
                                    : 'bg-rose-500 hover:bg-rose-600 text-white'
                                : 'bg-gray-400 cursor-not-allowed'}
                            ${product.available ? 'active:translate-x-[4px] active:translate-y-[4px] active:shadow-[4px_4px_0px_0px_rgba(0,0,0,1)]' : ''}`}
                    >
                        <ShoppingCart size={20} />
                        {product.available ? 'Add to Cart' : 'Out of Stock'}
                    </button>
                </div>
            </div>
            {isAdmin && (
                <div className="flex gap-4 mt-6">
                    <button
                        onClick={() => navigate(`/update-product/${product.id}`)}
                        className={`py-3 px-6 rounded-lg font-bold flex items-center gap-2 border-4 border-black transition-colors
                ${darkMode ? 'bg-blue-600 hover:bg-blue-700' : 'bg-blue-500 hover:bg-blue-600 text-white'}
                shadow-[8px_8px_0px_0px_rgba(0,0,0,1)] active:translate-x-[4px] active:translate-y-[4px] active:shadow-[4px_4px_0px_0px_rgba(0,0,0,1)]`}
                    >
                        <Edit size={20} />
                        Edit Product
                    </button>

                    <button
                        onClick={handleDelete}
                        className={`py-3 px-6 rounded-lg font-bold flex items-center gap-2 border-4 border-black transition-colors
                ${darkMode ? 'bg-gray-600 hover:bg-gray-700' : 'bg-gray-500 hover:bg-gray-600 text-white'}
                shadow-[8px_8px_0px_0px_rgba(0,0,0,1)] active:translate-x-[4px] active:translate-y-[4px] active:shadow-[4px_4px_0px_0px_rgba(0,0,0,1)]`}
                    >
                        <Trash size={20} />
                        Delete Product
                    </button>
                </div>
            )}
        </div>
    );
}

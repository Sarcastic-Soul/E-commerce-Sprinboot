// src/components/ProductGrid.jsx
import { useNavigate } from 'react-router-dom';
import ProductCard from './ProductCard';

export default function ProductGrid({ products, addToCart }) {
    const navigate = useNavigate();

    return (
        <div>
            <h2 className="text-4xl font-black mb-8 border-b-4 border-black pb-2 inline-block">PRODUCTS</h2>
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
                {products.map(product => (
                    <ProductCard
                        key={product.id}
                        product={product}
                        onClick={() => navigate(`/product/${product.id}`)}
                        addToCart={addToCart}
                    />
                ))}
            </div>
        </div>
    );
}

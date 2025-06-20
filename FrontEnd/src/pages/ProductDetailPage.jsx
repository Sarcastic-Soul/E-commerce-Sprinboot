import { useParams } from 'react-router-dom';
import { useEffect, useState } from 'react';
import ProductDetail from '../components/ProductDetail';
import LoadingState from '../components/LoadingState';
import ErrorState from '../components/ErrorState';
import api from '../api/axios';
import { useTheme } from '../context/ThemeContext';


export default function ProductDetailPage({ addToCart }) {
    const { id } = useParams();
    const { darkMode} = useTheme();
    const [product, setProduct] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    useEffect(() => {
        const fetchProduct = async () => {
            try {
                setLoading(true);
                const { data } = await api.get(`/product/${id}`);
                setProduct(data);
            } catch (err) {
                console.error(err);
                setError('Failed to load product.');
            } finally {
                setLoading(false);
            }
        };
        fetchProduct();
    }, [id]);

    if (loading) return <LoadingState darkMode={darkMode} />;
    if (error) return <ErrorState error={error} darkMode={darkMode} />;

    return (
        <ProductDetail
            product={product}
            addToCart={addToCart}
            darkMode={darkMode}
        />
    );
}

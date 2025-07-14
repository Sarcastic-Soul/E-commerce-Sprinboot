import { createContext, useContext, useState, useEffect } from "react";
import api from "../api/axios";
import { useAuth } from "./AuthContext";

const CartContext = createContext();

export const useCart = () => useContext(CartContext);

export const CartProvider = ({ children }) => {
    const { user } = useAuth();
    const [cartItemCount, setCartItemCount] = useState(0);

    useEffect(() => {
        const fetchCartItems = async () => {
            if (!user?.username) return;

            try {
                const res = await api.get(`/cart/${user.username}`);
                const items = res.data;
                const count = items.reduce((sum, item) => sum + item.quantity, 0);
                setCartItemCount(count);
            } catch (error) {
                console.error("Failed to fetch cart items:", error);
            }
        };

        fetchCartItems();
    }, [user?.username]);

    return (
        <CartContext.Provider value={{ cartItemCount, setCartItemCount }}>
            {children}
        </CartContext.Provider>
    );
};

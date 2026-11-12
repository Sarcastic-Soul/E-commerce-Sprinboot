import {
    createContext,
    useContext,
    useState,
    useEffect,
    type Dispatch,
    type ReactNode,
    type SetStateAction,
} from "react";
import api from "../api/axios";
import { useAuth } from "./AuthContext";
import type { CartItem } from "../api/types";

interface CartContextValue {
    cartItemCount: number;
    setCartItemCount: Dispatch<SetStateAction<number>>;
}

const CartContext = createContext<CartContextValue | null>(null);

export function useCart(): CartContextValue {
    const ctx = useContext(CartContext);
    if (!ctx) throw new Error("useCart must be used inside <CartProvider>");
    return ctx;
}

export const CartProvider = ({ children }: { children: ReactNode }) => {
    const { user } = useAuth();
    const [cartItemCount, setCartItemCount] = useState(0);

    useEffect(() => {
        const fetchCartItems = async () => {
            // Logged out: clear the count instead of leaving the previous user's
            // badge on screen.
            if (!user?.username) {
                setCartItemCount(0);
                return;
            }

            try {
                const res = await api.get<CartItem[]>(`/cart/${user.username}`);
                const items = res.data;
                const count = items.reduce((sum, item) => sum + item.quantity, 0);
                setCartItemCount(count);
            } catch (error) {
                console.error("Failed to fetch cart items:", error);
            }
        };

        void fetchCartItems();
    }, [user?.username]);

    return (
        <CartContext.Provider value={{ cartItemCount, setCartItemCount }}>
            {children}
        </CartContext.Provider>
    );
};

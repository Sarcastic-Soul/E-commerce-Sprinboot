// src/components/Cart.jsx
import { X, Plus, Minus } from 'lucide-react';
import { useEffect, useRef } from 'react';
import { useTheme } from '../context/ThemeContext';

export default function Cart({ cart, closeCart, removeFromCart, updateQuantity, cartTotal }) {
    const { darkMode } = useTheme();
    const cartRef = useRef(null);

    useEffect(() => {
        // Add animation class after component mounts
        const timer = setTimeout(() => {
            if (cartRef.current) {
                cartRef.current.classList.add('translate-x-0');
            }
        }, 10);

        // Handle click outside to close cart
        const handleClickOutside = (event) => {
            if (cartRef.current && !cartRef.current.contains(event.target)) {
                closeCart();
            }
        };

        document.addEventListener('mousedown', handleClickOutside);

        return () => {
            clearTimeout(timer);
            document.removeEventListener('mousedown', handleClickOutside);
        };
    }, [closeCart]);

    return (
        <div
            ref={cartRef}
            className={`w-full max-w-md transform transition-transform duration-500 ease-out -translate-x-full
                fixed left-0 inset-y-0 z-50
                ${darkMode ? 'bg-gray-800 text-white' : 'bg-yellow-50 text-gray-900'} 
                h-full overflow-y-auto shadow-lg border-r-4 ${darkMode ? 'border-gray-700' : 'border-black'}`}
        >
            <div className="p-6">
                <div className="flex justify-between items-center mb-6">
                    <h2 className="text-2xl font-black">YOUR CART</h2>
                    <button
                        onClick={closeCart}
                        className={`p-2 rounded-full ${darkMode ? 'bg-gray-700 hover:bg-gray-600' : 'bg-black hover:bg-gray-800 text-white'}`}
                    >
                        <X size={24} />
                    </button>
                </div>

                {cart.length === 0 ? (
                    <div className="text-center py-10">
                        <p className="text-xl font-bold mb-4">Your cart is empty</p>
                        <button
                            onClick={closeCart}
                            className={`px-4 py-2 rounded font-bold
                  ${darkMode ? 'bg-blue-600 hover:bg-blue-700' : 'bg-black hover:bg-gray-800 text-white'} 
                  transition-colors duration-300 border-2 border-black shadow-[4px_4px_0px_0px_rgba(0,0,0,1)]
                  active:translate-x-[2px] active:translate-y-[2px] active:shadow-[2px_2px_0px_0px_rgba(0,0,0,1)]`}
                        >
                            Continue Shopping
                        </button>
                    </div>
                ) : (
                    <>
                        <div className="space-y-4 mb-6">
                            {cart.map(item => {
                                const imageUrl = item.imageData
                                    ? `data:image/jpeg;base64,${item.imageData}`
                                    : 'https://picsum.photos/400';

                                return (
                                    <div
                                        key={item.id}
                                        className={`${darkMode ? 'bg-gray-700' : 'bg-white'} rounded-lg overflow-hidden shadow border-2 ${darkMode ? 'border-gray-600' : 'border-black'} p-4 flex`}
                                    >
                                        <img
                                            src={imageUrl}
                                            loading="lazy"
                                            alt={item.name}
                                            className="w-20 h-20 object-cover rounded mr-4 border-2 border-black"
                                        />

                                        <div className="flex-1">
                                            <div className="flex justify-between">
                                                <h3 className="font-bold">{item.name}</h3>
                                                <button
                                                    onClick={() => removeFromCart(item.id)}
                                                    className={`${darkMode ? 'text-gray-400 hover:text-white' : 'text-gray-500 hover:text-black'}`}
                                                >
                                                    <X size={16} />
                                                </button>
                                            </div>

                                            <p className="text-sm opacity-70">{item.category}</p>
                                            <p className="font-bold">${item.price.toFixed(2)}</p>

                                            <div className="flex items-center mt-2">
                                                <button
                                                    onClick={() => updateQuantity(item.id, -1)}
                                                    disabled={item.quantity <= 1}
                                                    className={`p-1 rounded ${item.quantity <= 1 ? 'opacity-50 cursor-not-allowed' : ''} 
                              ${darkMode ? 'bg-gray-600 hover:bg-gray-500' : 'bg-gray-200 hover:bg-gray-300'}`}
                                                >
                                                    <Minus size={14} />
                                                </button>

                                                <span className="mx-2 font-bold">{item.quantity}</span>

                                                <button
                                                    onClick={() => updateQuantity(item.id, 1)}
                                                    className={`p-1 rounded
                              ${darkMode ? 'bg-gray-600 hover:bg-gray-500' : 'bg-gray-200 hover:bg-gray-300'}`}
                                                >
                                                    <Plus size={14} />
                                                </button>
                                            </div>
                                        </div>
                                    </div>
                                );
                            })}
                        </div>

                        <div className={`p-4 rounded-lg mb-6 ${darkMode ? 'bg-gray-700' : 'bg-black text-white'}`}>
                            <div className="flex justify-between items-center">
                                <span className="text-lg">Total:</span>
                                <span className="text-2xl font-black">${cartTotal.toFixed(2)}</span>
                            </div>
                        </div>

                        <button
                            className={`w-full py-3 px-6 rounded-lg font-bold text-lg mb-4
                  ${darkMode ? 'bg-green-600 hover:bg-green-700' : 'bg-green-500 hover:bg-green-600'} text-white
                  transition-colors duration-300 border-4 border-black
                  shadow-[8px_8px_0px_0px_rgba(0,0,0,1)]
                  active:translate-x-[4px] active:translate-y-[4px] active:shadow-[4px_4px_0px_0px_rgba(0,0,0,1)]`}
                            onClick={()=> alert("Thank You Shopping with us :)")}
                        >
                            Checkout
                        </button>

                        <button
                            onClick={closeCart}
                            className={`w-full py-2 px-4 rounded font-bold
                  ${darkMode ? 'bg-transparent border-2 border-white' : 'bg-transparent border-2 border-black'} 
                  hover:opacity-70 transition-opacity duration-300`}
                        >
                            Continue Shopping
                        </button>
                    </>
                )}
            </div>
        </div>
        // </div>
    );
}
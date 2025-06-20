import { ShoppingCart, Moon, Sun, Plus, LogOut } from 'lucide-react';
import { Link } from 'react-router-dom';
import { useState, useEffect } from 'react';
import { Search } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import api from '../api/axios';
import { useTheme } from '../context/ThemeContext';

export default function Navbar({
                                   cartItemCount,
                                   setShowCart,
                                   onSearchResults
                               }) {
    const [searchTerm, setSearchTerm] = useState('');
    const [debouncedTerm, setDebouncedTerm] = useState('');
    const { user, logout } = useAuth();
    const { darkMode, toggleDarkMode } = useTheme();

    useEffect(() => {
        const handler = setTimeout(() => setDebouncedTerm(searchTerm), 500);
        return () => clearTimeout(handler);
    }, [searchTerm]);

    useEffect(() => {
        const fetchData = async () => {
            try {
                const url =
                    debouncedTerm.trim() === ''
                        ? '/products'
                        : `/products/search?keyword=${debouncedTerm}`;
                const { data } = await api.get(url);
                onSearchResults(data);
            } catch (error) {
                console.error('Search error:', error);
            }
        };
        fetchData();
    }, [debouncedTerm, onSearchResults]);

    return (
        <nav className={`${darkMode ? 'bg-gray-800 text-white' : 'bg-black text-white'} py-4 px-6 shadow-lg`}>
            <div className="container mx-auto flex flex-wrap gap-4 justify-between items-center">
                <h1 className="text-3xl font-black tracking-tighter">
                    <Link to="/">BRUT SHOP</Link>
                </h1>

                <div className="relative">
                    <Search className={`absolute left-3 top-1/2 -translate-y-1/2 ${darkMode ? 'text-white' : 'text-black'}`} size={18} />
                    <input
                        type="text"
                        placeholder="Search products..."
                        value={searchTerm}
                        onChange={(e) => setSearchTerm(e.target.value)}
                        className={`pl-10 pr-4 py-2 rounded-lg font-bold border-4 border-black focus:outline-none w-64
              ${darkMode ? 'bg-gray-900 text-white placeholder-gray-400' : 'bg-white text-black placeholder-gray-500'}`}
                    />
                </div>

                <div className="flex items-center gap-4">
                    {/* ✅ Only show Add Product if user is ADMIN */}
                    {user?.role === 'ADMIN' && (
                        <Link
                            to="/add-product"
                            className="flex items-center gap-2 p-2 rounded-lg bg-yellow-400 text-black font-semibold hover:bg-yellow-300 transition-all duration-300"
                        >
                            <Plus size={20} />
                            Add Product
                        </Link>
                    )}

                    <button
                        onClick={toggleDarkMode}
                        className={`p-2 rounded-lg ${darkMode ? 'bg-gray-700' : 'bg-gray-900'} hover:bg-opacity-80 transition-all duration-300`}
                    >
                        {darkMode ? <Sun size={20} /> : <Moon size={20} />}
                    </button>

                    <button
                        onClick={() => setShowCart(true)}
                        className="relative p-2 rounded-lg bg-rose-600 hover:bg-rose-700 transition-all duration-300"
                    >
                        <ShoppingCart size={20} />
                        {cartItemCount > 0 && (
                            <span className="absolute -top-2 -right-2 bg-yellow-400 text-black text-xs font-bold px-2 py-1 rounded-full border-2 border-black">
                {cartItemCount}
              </span>
                        )}
                    </button>

                    {/* ✅ Show Login if not logged in */}
                    {!user ? (
                        <Link
                            to="/login"
                            className="px-4 py-2 bg-blue-600 text-white font-semibold rounded-lg border-2 border-blue-700 hover:bg-blue-700 transition"
                        >
                            Login
                        </Link>
                    ) : (
                        <div className="flex items-center gap-3">
                            <span className="font-bold text-yellow-400">Hi, {user.username}</span>
                            <button
                                onClick={logout}
                                title="Logout"
                                className="flex items-center gap-1 px-3 py-2 rounded-lg font-semibold border-2 border-red-600 bg-red-500 hover:bg-red-600 transition"
                            >
                                <LogOut size={18} />
                                Logout
                            </button>
                        </div>
                    )}
                </div>
            </div>
        </nav>
    );
}

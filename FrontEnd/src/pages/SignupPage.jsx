import { useState, useEffect } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { useTheme } from '../context/ThemeContext';
import { Eye, EyeOff } from 'lucide-react';
import {toast} from "sonner";

export default function SignupPage() {
    const { signup, user } = useAuth();
    const { darkMode } = useTheme();
    const [username, setUsername] = useState('');
    const [password, setPassword] = useState('');
    const [confirmPassword, setConfirmPassword] = useState('');
    const [showPassword, setShowPassword] = useState(false);
    const [showConfirm, setShowConfirm] = useState(false);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');
    const navigate = useNavigate();

    useEffect(() => {
        if (user) navigate('/');
    }, [user, navigate]);

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError('');
        if (password !== confirmPassword) {
            setError('Passwords do not match');
            return;
        }
        setLoading(true);
        try {
            await signup(username, password);
            toast.success("Signup Successful")
            navigate('/');
        } catch (err) {
            setError(err.message);
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className={`flex justify-center items-center min-h-screen transition-colors duration-300 ${darkMode ? 'bg-gray-900 text-white' : 'bg-yellow-50 text-black'}`}>
            <form
                onSubmit={handleSubmit}
                className={`p-8 border-4 rounded-2xl shadow-[6px_6px_0px_0px_black] w-full max-w-md space-y-6
                ${darkMode ? 'bg-gray-800 border-gray-600 text-white' : 'bg-white border-black text-black'}`}
            >
                <h1 className="text-3xl font-extrabold text-center">Sign Up</h1>

                {error && (
                    <div className="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded-lg text-sm font-semibold">
                        {error}
                    </div>
                )}

                <input
                    type="text"
                    placeholder="Username"
                    className={`w-full p-3 border-2 rounded-xl ${darkMode ? 'bg-gray-700 border-gray-500 text-white' : 'bg-[#e0e0e0] border-black text-black'}`}
                    value={username}
                    onChange={(e) => setUsername(e.target.value)}
                    required
                />

                <div className="relative">
                    <input
                        type={showPassword ? 'text' : 'password'}
                        placeholder="Password"
                        className={`w-full p-3 border-2 rounded-xl pr-12 ${darkMode ? 'bg-gray-700 border-gray-500 text-white' : 'bg-[#e0e0e0] border-black text-black'}`}
                        value={password}
                        onChange={(e) => setPassword(e.target.value)}
                        required
                    />
                    <button
                        type="button"
                        onClick={() => setShowPassword(!showPassword)}
                        className="absolute top-1/2 right-3 -translate-y-1/2 text-gray-400 hover:text-white"
                        tabIndex={-1}
                    >
                        {showPassword ? <EyeOff size={20} /> : <Eye size={20} />}
                    </button>
                </div>

                <div className="relative">
                    <input
                        type={showConfirm ? 'text' : 'password'}
                        placeholder="Confirm Password"
                        className={`w-full p-3 border-2 rounded-xl pr-12 ${darkMode ? 'bg-gray-700 border-gray-500 text-white' : 'bg-[#e0e0e0] border-black text-black'}`}
                        value={confirmPassword}
                        onChange={(e) => setConfirmPassword(e.target.value)}
                        required
                    />
                    <button
                        type="button"
                        onClick={() => setShowConfirm(!showConfirm)}
                        className="absolute top-1/2 right-3 -translate-y-1/2 text-gray-400 hover:text-white"
                        tabIndex={-1}
                    >
                        {showConfirm ? <EyeOff size={20} /> : <Eye size={20} />}
                    </button>
                </div>

                <button
                    type="submit"
                    disabled={loading}
                    className="w-full py-3 px-6 font-bold rounded-xl shadow-[4px_4px_0px_0px_black] bg-black text-white hover:bg-gray-800 transition"
                >
                    {loading ? 'Signing up...' : 'Sign Up'}
                </button>

                <p className="text-center text-sm opacity-80">
                    Already have an account?{' '}
                    <Link to="/login" className="text-blue-500 font-bold hover:underline">
                        Login
                    </Link>
                </p>
            </form>
        </div>
    );
}
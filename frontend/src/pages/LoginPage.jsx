import { useState, useEffect } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { useTheme } from '../context/ThemeContext';
import { EyeIcon as Eye, EyeSlashIcon as EyeOff, UserIcon as User, ShieldCheckIcon as ShieldCheck } from '@phosphor-icons/react';
import {toast} from "sonner";

// Seeded by DevSeeder on first boot. Also published in the README, so there is
// nothing secret here — the buttons just save reviewers from typing them.
const DEMO_ACCOUNTS = [
    { label: 'User', username: 'user', password: 'user123', Icon: User },
    { label: 'Admin', username: 'admin', password: 'admin123', Icon: ShieldCheck },
];

export default function LoginPage() {
    const { login, user } = useAuth();
    const { darkMode } = useTheme();
    const [username, setUsername] = useState('');
    const [password, setPassword] = useState('');
    const [showPassword, setShowPassword] = useState(false);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');
    const navigate = useNavigate();

    useEffect(() => {
        if (user) {
            navigate('/');
        }
    }, [user, navigate]);

    // Takes the credentials as arguments rather than reading state, so the demo
    // buttons can fill the inputs and sign in within the same click.
    const submitLogin = async (name, pass) => {
        setLoading(true);
        setError('');
        try {
            await login(name, pass);
            toast.success("User Logged In")
            navigate('/');
        } catch {
            setError('Invalid username or password');
        } finally {
            setLoading(false);
        }
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        await submitLogin(username, password);
    };

    const handleDemoLogin = async (account) => {
        setUsername(account.username);
        setPassword(account.password);
        await submitLogin(account.username, account.password);
    };

    return (
        <div className={`flex justify-center items-center min-h-screen transition-colors duration-300 ${darkMode ? 'bg-gray-900 text-white' : 'bg-yellow-50 text-black'}`}>
            <form
                onSubmit={handleSubmit}
                className={`p-8 border-4 rounded-2xl shadow-[6px_6px_0px_0px_black] w-full max-w-md space-y-6
                ${darkMode ? 'bg-gray-800 border-black text-white' : 'bg-white border-black text-black'}`}
            >
                <h1 className="text-3xl font-extrabold text-center">Login</h1>

                {error && (
                    <div className="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded-lg text-sm font-semibold">
                        {error}
                    </div>
                )}

                <input
                    type="text"
                    placeholder="Username"
                    className={`w-full p-3 border-2 rounded-xl ${darkMode ? 'bg-gray-900 border-black text-white placeholder-gray-400' : 'bg-[#e0e0e0] border-black text-black'}`}
                    value={username}
                    onChange={(e) => setUsername(e.target.value)}
                    required
                />

                <div className="relative">
                    <input
                        type={showPassword ? 'text' : 'password'}
                        placeholder="Password"
                        className={`w-full p-3 border-2 rounded-xl pr-12 ${darkMode ? 'bg-gray-900 border-black text-white placeholder-gray-400' : 'bg-[#e0e0e0] border-black text-black'}`}
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

                <button
                    type="submit"
                    disabled={loading}
                    className={`w-full py-3 px-6 font-bold rounded-xl border-2 border-black shadow-[4px_4px_0px_0px_black] transition disabled:opacity-60 ${darkMode ? "bg-yellow-400 text-black hover:bg-yellow-300" : "bg-black text-white hover:bg-gray-800"}`}
                >
                    {loading ? 'Logging in...' : 'Login'}
                </button>

                {/* One-click demo sign-in */}
                <div className="space-y-3">
                    <div className="flex items-center gap-3">
                        <span className={`h-0.5 flex-1 bg-black`} />
                        <span className="text-xs font-black uppercase tracking-widest opacity-70">
                            Try a demo account
                        </span>
                        <span className={`h-0.5 flex-1 bg-black`} />
                    </div>

                    <div className="grid grid-cols-2 gap-3">
                        {DEMO_ACCOUNTS.map((account) => (
                            <button
                                key={account.label}
                                type="button"
                                onClick={() => handleDemoLogin(account)}
                                disabled={loading}
                                title={`${account.username} / ${account.password}`}
                                className={`flex flex-col items-center gap-1 py-3 px-2 border-2 font-bold rounded-xl shadow-[4px_4px_0px_0px_black]
                                transition-transform active:translate-x-[2px] active:translate-y-[2px] active:shadow-none disabled:opacity-60
                                ${darkMode
                                    ? 'bg-yellow-400 border-black text-black hover:bg-yellow-300'
                                    : 'bg-yellow-400 border-black text-black hover:bg-yellow-500'}`}
                            >
                                <span className="flex items-center gap-2">
                                    <account.Icon size={18} />
                                    {account.label}
                                </span>
                                <span className="text-[11px] font-mono opacity-80">
                                    {account.username} / {account.password}
                                </span>
                            </button>
                        ))}
                    </div>
                </div>

                <p className="text-center text-sm opacity-80">
                    Don't have an account?{' '}
                    <Link to="/signup" className="font-black underline decoration-4 decoration-yellow-400 underline-offset-4 hover:bg-yellow-400 hover:text-black">
                        Sign up
                    </Link>
                </p>
            </form>
        </div>
    );
}

// src/pages/NotFound.jsx
import { Link } from 'react-router-dom';

export default function NotFound() {
    return (
        <div className="min-h-screen flex flex-col items-center justify-center bg-yellow-50 text-black px-4 text-center">
            <h1 className="text-6xl font-extrabold mb-4">404</h1>
            <p className="text-2xl font-semibold mb-2">Page Not Found</p>
            <p className="mb-6 text-gray-600">The page you're looking for doesn't exist or has been moved.</p>
            <Link
                to="/"
                className="py-3 px-6 font-bold rounded-xl shadow-[4px_4px_0px_0px_black] bg-black text-white hover:bg-gray-800 transition"
            >
                Go Home
            </Link>
        </div>
    );
}

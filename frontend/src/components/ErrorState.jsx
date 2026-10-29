// src/components/ErrorState.jsx
import { useTheme } from '../context/ThemeContext';

export default function ErrorState({ error, onRetry }) {
    const { darkMode } = useTheme();

    return (
        <div className={`text-center py-16 px-4 rounded-lg border-4 ${darkMode ? 'border-red-600 bg-gray-800' : 'border-black bg-rose-100'} mx-auto max-w-lg`}>
            <h3 className="text-2xl font-black mb-4">OOPS!</h3>
            <p className="text-lg mb-4">{error}</p>
            <button
                onClick={onRetry || (() => window.location.reload())}
                className={`px-6 py-3 rounded-lg font-bold min-h-[44px] focus:outline-none focus-visible:outline-4 focus-visible:outline-offset-2 focus-visible:outline-rose-500
          ${darkMode ? 'bg-yellow-400 text-black hover:bg-yellow-300' : 'bg-black text-white hover:bg-gray-800'} 
          transition-colors duration-300 border-2 border-black shadow-[4px_4px_0px_0px_rgba(0,0,0,1)]
          active:translate-x-[2px] active:translate-y-[2px] active:shadow-[2px_2px_0px_0px_rgba(0,0,0,1)]`}
            >
                Try Again
            </button>
        </div>
    );
}
// src/components/LoadingState.jsx
import { useTheme } from '../context/ThemeContext';

export default function LoadingState() {
    const { darkMode} = useTheme();
    return (
        <div className="flex flex-col items-center justify-center py-20">
            <div className={`w-16 h-16 border-4 rounded-full border-t-transparent animate-spin mb-4
        ${darkMode ? 'border-blue-500' : 'border-black'}`}>
            </div>
            <h3 className="text-xl font-bold">LOADING PRODUCTS...</h3>
        </div>
    );
}
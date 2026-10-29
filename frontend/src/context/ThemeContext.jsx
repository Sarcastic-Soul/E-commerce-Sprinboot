// src/context/ThemeContext.jsx
import { createContext, useContext, useEffect, useState } from 'react';

const ThemeContext = createContext();
const STORAGE_KEY = 'brutshop-theme';

// A saved choice wins; otherwise follow the operating system setting.
function initialDarkMode() {
    try {
        const saved = localStorage.getItem(STORAGE_KEY);
        if (saved) return saved === 'dark';
    } catch {
        // Storage can be blocked (private mode); fall through to the OS setting.
    }
    return window.matchMedia?.('(prefers-color-scheme: dark)').matches ?? false;
}

export function ThemeProvider({ children }) {
    const [darkMode, setDarkMode] = useState(initialDarkMode);

    const toggleDarkMode = () =>
        setDarkMode(prev => {
            try {
                localStorage.setItem(STORAGE_KEY, prev ? 'light' : 'dark');
            } catch {
                // Not saved; the toggle still works for this visit.
            }
            return !prev;
        });

    // The class drives the warm dark palette in index.css; color-scheme makes
    // native controls (selects, scrollbars, date pickers) match the theme.
    useEffect(() => {
        const root = document.documentElement;
        root.classList.toggle('dark', darkMode);
        root.style.colorScheme = darkMode ? 'dark' : 'light';
    }, [darkMode]);

    return (
        <ThemeContext.Provider value={{ darkMode, toggleDarkMode }}>
            {children}
        </ThemeContext.Provider>
    );
}

export function useTheme() {
    return useContext(ThemeContext);
}

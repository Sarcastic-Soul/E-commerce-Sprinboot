// src/components/ToastProvider.jsx
import { Toaster } from 'sonner';
import { useTheme } from '../context/ThemeContext';

export default function ToastProvider() {
    const { darkMode } = useTheme();

    return (
        <Toaster
            position="top-right"
            toastOptions={{
                closeButton: true,
                className: 'border-4 shadow-[6px_6px_0_0_rgba(0,0,0,1)] rounded-lg font-bold',
                style: {
                    borderColor: '#000000',
                },
                success: {
                    style: {
                        backgroundColor: darkMode ? '#15803d' : '#bbf7d0',
                        color: darkMode ? '#ffffff' : '#064e3b',
                    },
                },
                error: {
                    style: {
                        backgroundColor: darkMode ? '#7f1d1d' : '#fecaca',
                        color: darkMode ? '#ffffff' : '#7f1d1d',
                    },
                },
                info: {
                    style: {
                        backgroundColor: darkMode ? '#1e40af' : '#dbeafe',
                        color: darkMode ? '#ffffff' : '#1e3a8a',
                    },
                },
                warning: {
                    style: {
                        backgroundColor: darkMode ? '#78350f' : '#fef3c7',
                        color: darkMode ? '#ffffff' : '#92400e',
                    },
                },
            }}
        />
    );
}

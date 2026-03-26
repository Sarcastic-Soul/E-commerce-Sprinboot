import { useState, useEffect, useRef } from "react";
import { Bell, Check } from "lucide-react";
import api from "../api/axios";
import { useTheme } from "../context/ThemeContext";

export default function NotificationBell() {
    const [notifications, setNotifications] = useState([]);
    const [isOpen, setIsOpen] = useState(false);
    const { darkMode } = useTheme();
    const dropdownRef = useRef(null);

    // Fetch notifications on load
    useEffect(() => {
        fetchNotifications();
    }, []);

    // Close dropdown if clicking outside of it
    useEffect(() => {
        const handleClickOutside = (event) => {
            if (
                dropdownRef.current &&
                !dropdownRef.current.contains(event.target)
            ) {
                setIsOpen(false);
            }
        };
        document.addEventListener("mousedown", handleClickOutside);
        return () =>
            document.removeEventListener("mousedown", handleClickOutside);
    }, []);

    const fetchNotifications = async () => {
        try {
            const response = await api.get("/user/notifications");
            setNotifications(response.data);
        } catch (error) {
            console.error("Failed to fetch notifications:", error);
        }
    };

    const handleMarkAsRead = async (id, e) => {
        e.stopPropagation(); // Don't close the dropdown
        try {
            await api.put(`/user/notifications/${id}/read`);
            // Update local state instantly so the UI feels fast
            setNotifications((current) =>
                current.map((n) => (n.id === id ? { ...n, read: true } : n)),
            );
        } catch (error) {
            console.error("Failed to mark read:", error);
        }
    };

    const unreadCount = notifications.filter((n) => !n.read).length;

    return (
        <div className="relative" ref={dropdownRef}>
            <button
                onClick={() => setIsOpen(!isOpen)}
                className={`relative p-2 rounded-full border-2 border-black transition-all transform active:translate-x-[2px] active:translate-y-[2px] shadow-[4px_4px_0px_0px_rgba(0,0,0,1)] active:shadow-none
                ${darkMode ? "bg-gray-800 hover:bg-gray-700 text-white" : "bg-white hover:bg-gray-50 text-black"}`}
                title="Notifications"
            >
                <Bell size={24} />
                {unreadCount > 0 && (
                    <span className="absolute -top-2 -right-2 bg-red-500 text-white text-xs font-bold w-6 h-6 flex items-center justify-center rounded-full border-2 border-black">
                        {unreadCount}
                    </span>
                )}
            </button>

            {/* Dropdown Menu */}
            {isOpen && (
                <div
                    className={`absolute right-0 mt-4 w-80 md:w-96 border-4 border-black rounded-lg shadow-[8px_8px_0px_0px_rgba(0,0,0,1)] z-50 overflow-hidden
                                ${darkMode ? "bg-gray-800 text-white" : "bg-white text-black"}`}
                >
                    <div
                        className={`p-4 border-b-4 border-black flex justify-between items-center ${darkMode ? "bg-gray-900 text-white" : "bg-yellow-300 text-black"}`}
                    >
                        <h3 className="font-black text-lg">Notifications</h3>
                        <span className="text-sm font-bold bg-white text-black px-2 py-1 rounded border-2 border-black">
                            {unreadCount} New
                        </span>
                    </div>

                    <div className="max-h-96 overflow-y-auto">
                        {notifications.length === 0 ? (
                            <div
                                className={`p-6 text-center font-bold ${darkMode ? "text-gray-400" : "text-gray-600"}`}
                            >
                                You have no notifications.
                            </div>
                        ) : (
                            notifications.map((notification) => (
                                <div
                                    key={notification.id}
                                    className={`p-4 border-b-2 border-black last:border-b-0 flex justify-between items-start gap-3 transition-colors
                                                    ${!notification.read ? (darkMode ? "bg-gray-700" : "bg-yellow-50") : darkMode ? "bg-gray-800" : "bg-white"}
                                                    ${darkMode ? "hover:bg-gray-600" : "hover:bg-gray-100"}
                                                `}
                                >
                                    <div>
                                        <p
                                            className={`text-sm ${!notification.read ? "font-black" : "font-medium"} ${darkMode ? "text-gray-100" : "text-black"}`}
                                        >
                                            {notification.message}
                                        </p>
                                        <p
                                            className={`text-xs mt-1 font-bold ${darkMode ? "text-gray-400" : "text-gray-500"}`}
                                        >
                                            {new Date(
                                                notification.createdAt,
                                            ).toLocaleDateString()}
                                        </p>
                                    </div>
                                    {!notification.read && (
                                        <button
                                            onClick={(e) =>
                                                handleMarkAsRead(
                                                    notification.id,
                                                    e,
                                                )
                                            }
                                            className="p-1 rounded-full bg-green-500 text-white border-2 border-black shadow-[2px_2px_0px_0px_rgba(0,0,0,1)] active:translate-x-[1px] active:translate-y-[1px] active:shadow-none flex-shrink-0"
                                            title="Mark as read"
                                        >
                                            <Check size={16} />
                                        </button>
                                    )}
                                </div>
                            ))
                        )}
                    </div>
                </div>
            )}
        </div>
    );
}

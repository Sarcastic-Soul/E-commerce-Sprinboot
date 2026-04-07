import { createContext, useContext, useState } from "react";
import axios from "../api/axios";

const AuthContext = createContext();

// Helper function to decode token IMMEDIATELY (No useEffect delays)
const getUserFromToken = (token) => {
    if (!token) return null;
    try {
        const payload = JSON.parse(atob(token.split(".")[1]));
        const role =
            payload.role ||
            (Array.isArray(payload.roles) ? payload.roles[0] : undefined);
        return { username: payload.sub, role };
    } catch (e) {
        console.error("Invalid token", e);
        return null;
    }
};

export const AuthProvider = ({ children }) => {
    const initialToken = localStorage.getItem("token");

    // Initialize token and user synchronously!
    const [token, setToken] = useState(initialToken);
    const [user, setUser] = useState(() => getUserFromToken(initialToken));

    const saveTokens = (newToken, newRefreshToken) => {
        localStorage.setItem("token", newToken);
        if (newRefreshToken) {
            localStorage.setItem("refreshToken", newRefreshToken);
        }
        setToken(newToken);
        setUser(getUserFromToken(newToken));
    };

    const login = async (username, password) => {
        try {
            const res = await axios.post("/auth/login", { username, password });
            // Save both the access token and the refresh token returned from the backend
            saveTokens(res.data.token, res.data.refreshToken);
        } catch (err) {
            throw new Error("Login failed");
        }
    };

    const signup = async (username, password) => {
        try {
            await axios.post("/auth/signup", { username, password });
            await login(username, password); // auto-login
        } catch (err) {
            const message = err.response?.data || "Signup failed";
            throw new Error(message);
        }
    };

    const logout = async () => {
        try {
            const refreshToken = localStorage.getItem("refreshToken");
            if (refreshToken) {
                // Optionally notify backend to revoke the token in Redis
                await axios.post("/auth/logout", { refreshToken });
            }
        } catch (err) {
            console.error("Logout request failed on server", err);
        } finally {
            // Ensure local state and storage are cleared regardless of server response
            localStorage.removeItem("token");
            localStorage.removeItem("refreshToken");
            setToken(null);
            setUser(null);
            window.location.reload();
        }
    };

    return (
        <AuthContext.Provider value={{ user, token, login, signup, logout }}>
            {children}
        </AuthContext.Provider>
    );
};

export const useAuth = () => useContext(AuthContext);

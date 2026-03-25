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

    const saveToken = (newToken) => {
        localStorage.setItem("token", newToken);
        setToken(newToken);
        setUser(getUserFromToken(newToken));
    };

    const login = async (username, password) => {
        try {
            const res = await axios.post("/auth/login", { username, password });
            saveToken(res.data.token);
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

    const logout = () => {
        localStorage.removeItem("token");
        setToken(null);
        setUser(null);
        window.location.reload();
    };

    return (
        <AuthContext.Provider value={{ user, token, login, signup, logout }}>
            {children}
        </AuthContext.Provider>
    );
};

export const useAuth = () => useContext(AuthContext);

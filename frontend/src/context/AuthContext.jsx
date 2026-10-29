import { createContext, useContext, useEffect, useState } from "react";
import axios from "../api/axios";

const AuthContext = createContext();

const toUser = (data) => ({
    username: data.username,
    role: Array.isArray(data.roles) ? data.roles[0] : data.roles,
});

export const AuthProvider = ({ children }) => {
    const [user, setUser] = useState(null);
    // The token is an httpOnly cookie now, invisible to JS, so there is nothing to
    // read synchronously on mount. /auth/me tells us who (if anyone) it belongs to;
    // authLoading gates ProtectedRoute so it doesn't redirect a logged-in user to
    // /login just because that check hasn't resolved yet.
    const [authLoading, setAuthLoading] = useState(true);

    useEffect(() => {
        let cancelled = false;
        axios
            .get("/auth/me")
            .then((res) => {
                if (!cancelled) setUser(toUser(res.data));
            })
            .catch(() => {
                if (!cancelled) setUser(null);
            })
            .finally(() => {
                if (!cancelled) setAuthLoading(false);
            });
        return () => {
            cancelled = true;
        };
    }, []);

    const login = async (username, password) => {
        try {
            const res = await axios.post("/auth/login", { username, password });
            setUser(toUser(res.data));
        } catch (err) {
            throw new Error("Login failed");
        }
    };

    const signup = async (username, password) => {
        try {
            await axios.post("/auth/signup", { username, password });
            await login(username, password); // auto-login
        } catch (err) {
            const message = err.response?.data?.message || "Signup failed";
            throw new Error(message);
        }
    };

    const logout = async () => {
        try {
            await axios.post("/auth/logout");
        } catch (err) {
            console.error("Logout request failed on server", err);
        } finally {
            // No window.location.reload() here: clearing this one piece of state
            // already re-renders every consumer, and a reload re-downloaded the
            // whole bundle. Callers navigate afterwards (AuthProvider sits outside
            // <Router>, so useNavigate is not available in this file).
            setUser(null);
        }
    };

    return (
        <AuthContext.Provider value={{ user, authLoading, login, signup, logout }}>
            {children}
        </AuthContext.Provider>
    );
};

export const useAuth = () => useContext(AuthContext);

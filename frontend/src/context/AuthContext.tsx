import { createContext, useContext, useEffect, useState, type ReactNode } from "react";
import axios from "../api/axios";
import { errorMessage } from "../utils/format";
import type { AuthUser } from "../api/types";
import { queryClient } from "../api/queryClient";

export interface User {
    username: string;
    role: string | undefined;
}

interface AuthContextValue {
    user: User | null;
    authLoading: boolean;
    login: (username: string, password: string) => Promise<void>;
    signup: (username: string, password: string) => Promise<void>;
    logout: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | null>(null);

const toUser = (data: AuthUser): User => ({
    username: data.username,
    role: Array.isArray(data.roles) ? data.roles[0] : data.roles,
});

export const AuthProvider = ({ children }: { children: ReactNode }) => {
    const [user, setUser] = useState<User | null>(null);
    // The token is an httpOnly cookie now, invisible to JS, so there is nothing to
    // read synchronously on mount. /auth/me tells us who (if anyone) it belongs to;
    // authLoading gates ProtectedRoute so it doesn't redirect a logged-in user to
    // /login just because that check hasn't resolved yet.
    const [authLoading, setAuthLoading] = useState(true);

    useEffect(() => {
        let cancelled = false;
        axios
            .get<AuthUser>("/auth/me")
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

    const login = async (username: string, password: string) => {
        try {
            const res = await axios.post<AuthUser>("/auth/login", { username, password });
            // Drop anything cached for a guest or a previous account.
            queryClient.clear();
            setUser(toUser(res.data));
        } catch (err) {
            throw new Error("Login failed", { cause: err });
        }
    };

    const signup = async (username: string, password: string) => {
        try {
            await axios.post("/auth/signup", { username, password });
            await login(username, password); // auto-login
        } catch (err) {
            const message = errorMessage(err, "Signup failed");
            throw new Error(message, { cause: err });
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
            // Cached orders, addresses and admin data belong to the old session.
            queryClient.clear();
            setUser(null);
        }
    };

    return (
        <AuthContext.Provider value={{ user, authLoading, login, signup, logout }}>
            {children}
        </AuthContext.Provider>
    );
};

export function useAuth(): AuthContextValue {
    const ctx = useContext(AuthContext);
    if (!ctx) throw new Error("useAuth must be used inside <AuthProvider>");
    return ctx;
}

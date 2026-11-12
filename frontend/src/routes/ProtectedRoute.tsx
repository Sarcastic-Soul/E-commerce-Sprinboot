import type { ReactNode } from "react";
import { Navigate } from "react-router";
import { useAuth } from "../context/AuthContext";
import LoadingState from "../components/LoadingState";

interface ProtectedRouteProps {
    children: ReactNode;
    /** When set, only users with this role get in (e.g. "ADMIN"). */
    role?: string;
}

export default function ProtectedRoute({ children, role }: ProtectedRouteProps) {
    const { user, authLoading } = useAuth();

    // Wait for the /auth/me bootstrap to resolve before deciding — otherwise every
    // logged-in user briefly flashes to /login on a hard refresh.
    if (authLoading) return <LoadingState variant="page" />;
    if (!user) return <Navigate to="/login" />;
    if (role && user.role !== role) return <Navigate to="/" />;

    return children;
}

import { Navigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import LoadingState from "../components/LoadingState";

export default function ProtectedRoute({ children, role }) {
    const { user, authLoading } = useAuth();

    // Wait for the /auth/me bootstrap to resolve before deciding — otherwise every
    // logged-in user briefly flashes to /login on a hard refresh.
    if (authLoading) return <LoadingState variant="page" />;
    if (!user) return <Navigate to="/login" />;
    if (role && user.role !== role) return <Navigate to="/" />;

    return children;
}

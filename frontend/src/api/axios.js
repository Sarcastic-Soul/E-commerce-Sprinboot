import axios from "axios";

const api = axios.create({
    baseURL: "/api",
    withCredentials: true,
});

// Automatically attach token to requests
api.interceptors.request.use(
    (config) => {
        const token = localStorage.getItem("token");
        if (token) {
            config.headers.Authorization = `Bearer ${token}`;
        }
        return config;
    },
    (error) => Promise.reject(error),
);

// Endpoints that must never trigger a refresh-and-retry: they either issue the
// tokens themselves or are the refresh call, so retrying them would loop.
const AUTH_ENDPOINTS = [
    "/auth/login",
    "/auth/signup",
    "/auth/refresh",
    "/auth/logout",
];

// Handle token expiration and refresh
api.interceptors.response.use(
    (response) => response,
    async (error) => {
        const originalRequest = error.config;

        // Only 401 means "not authenticated" (missing or expired token) and is worth
        // a refresh. A 403 means we ARE authenticated but lack permission — e.g. a
        // normal user hitting an admin route — and refreshing cannot change that.
        if (
            error.response?.status === 401 &&
            !originalRequest._retry &&
            !AUTH_ENDPOINTS.includes(originalRequest.url)
        ) {
            originalRequest._retry = true;

            try {
                const refreshToken = localStorage.getItem("refreshToken");
                if (!refreshToken) {
                    throw new Error("No refresh token available");
                }

                // Make the refresh request using standard axios to prevent interceptor loops
                const res = await axios.post("/api/auth/refresh", {
                    refreshToken,
                });

                const newAccessToken = res.data.token;
                const newRefreshToken = res.data.refreshToken;

                // Save the new tokens
                localStorage.setItem("token", newAccessToken);
                if (newRefreshToken) {
                    localStorage.setItem("refreshToken", newRefreshToken);
                }

                // Update the original request's authorization header
                originalRequest.headers.Authorization = `Bearer ${newAccessToken}`;

                // Retry the original request with the new token
                return api(originalRequest);
            } catch (refreshError) {
                // If the refresh token is also invalid or expired, force logout
                localStorage.removeItem("token");
                localStorage.removeItem("refreshToken");
                window.location.href = "/login";
                return Promise.reject(refreshError);
            }
        }

        return Promise.reject(error);
    },
);

export default api;

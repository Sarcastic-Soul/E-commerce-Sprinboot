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

// Handle token expiration and refresh
api.interceptors.response.use(
    (response) => response,
    async (error) => {
        const originalRequest = error.config;

        // If the error is 401 or 403 (unauthorized/forbidden) and we haven't retried yet
        if (
            (error.response?.status === 401 ||
                error.response?.status === 403) &&
            !originalRequest._retry &&
            originalRequest.url !== "/auth/login" // Prevent loop on login
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

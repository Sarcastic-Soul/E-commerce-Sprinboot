import axios from "axios";

const api = axios.create({
    baseURL: "/api",
    withCredentials: true,
    // Access/refresh tokens now live in httpOnly cookies (never JS-readable), so
    // there is no token to attach here. CSRF protection reads the double-submit
    // XSRF-TOKEN cookie and echoes it back as a header on mutating requests.
    xsrfCookieName: "XSRF-TOKEN",
    xsrfHeaderName: "X-XSRF-TOKEN",
    withXSRFToken: true,
});

// Fired once per page load so the XSRF-TOKEN cookie exists before the first
// mutating request needs to echo it back. The endpoint does nothing but resolve
// Spring Security's deferred CSRF token, which is what makes it write the cookie.
api.get("/auth/csrf").catch(() => {});

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
                // The refresh token travels as an httpOnly cookie; withCredentials
                // is enough, there is nothing to read from storage or attach here.
                await api.post("/auth/refresh");

                // Retry the original request; the new access-token cookie is
                // already attached automatically by the browser.
                return api(originalRequest);
            } catch (refreshError) {
                // No forced navigation here: plenty of pages (home, product detail)
                // are public, so a failed refresh just means "browsing as a guest."
                // ProtectedRoute is what sends a logged-out user to /login when
                // they actually need to be authenticated.
                return Promise.reject(refreshError);
            }
        }

        return Promise.reject(error);
    },
);

export default api;

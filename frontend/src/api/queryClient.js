import { QueryClient } from "@tanstack/react-query";

// One shared client. Data stays fresh for 30s so moving between pages does not
// refetch everything, and failed reads retry twice (helps while the serverless
// backend is cold-starting). 401/403/404 are real answers, so never retry them.
export const queryClient = new QueryClient({
    defaultOptions: {
        queries: {
            staleTime: 30_000,
            refetchOnWindowFocus: false,
            retry: (count, error) => {
                const status = error?.response?.status;
                if (status && status >= 400 && status < 500) return false;
                return count < 2;
            },
        },
    },
});

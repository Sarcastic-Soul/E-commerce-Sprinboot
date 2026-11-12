import { QueryClient } from "@tanstack/react-query";
import { isAxiosError, type AxiosError } from "axios";
import type { ApiError } from "./types";

// One shared client. Data stays fresh for 30s so moving between pages does not
// refetch everything, and failed reads retry twice (helps while the serverless
// backend is cold-starting). 401/403/404 are real answers, so never retry them.
export const queryClient = new QueryClient({
    defaultOptions: {
        queries: {
            staleTime: 30_000,
            refetchOnWindowFocus: false,
            retry: (count, error) => {
                const status = isAxiosError(error) ? error.response?.status : undefined;
                if (status && status >= 400 && status < 500) return false;
                return count < 2;
            },
        },
    },
});

// Every query and mutation goes through the axios client, so their errors are
// AxiosErrors carrying the backend's ApiErrorResponse body.
declare module "@tanstack/react-query" {
    interface Register {
        defaultError: AxiosError<ApiError>;
    }
}

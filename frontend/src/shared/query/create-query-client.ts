import { QueryClient } from "@tanstack/react-query";
import { ApiError } from "@/shared/api/api-error";

const STALE_TIME_MS = 60_000;
const QUERY_RETRY_COUNT = 1;

const shouldRetry = (failureCount: number, error: Error) =>
  failureCount < QUERY_RETRY_COUNT && !(error instanceof ApiError && error.status > 0);

export const createQueryClient = (): QueryClient =>
  new QueryClient({
    defaultOptions: {
      queries: { staleTime: STALE_TIME_MS, retry: shouldRetry, refetchOnWindowFocus: false },
      mutations: { retry: false },
    },
  });

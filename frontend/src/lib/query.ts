import { QueryClient } from '@tanstack/react-query';

/**
 * Shared TanStack Query client.
 *
 * Defaults tuned for an MVP:
 *   - 30s stale time keeps the UI snappy when navigating between pages.
 *   - 1 retry on failure (axios interceptor already handles the 401-refresh
 *     case; a single network retry catches transient errors).
 */
export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 30_000,
      retry: 1,
      refetchOnWindowFocus: false,
    },
  },
});

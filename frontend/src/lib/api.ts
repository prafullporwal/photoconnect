// =============================================================================
// HTTP client + token refresh
// =============================================================================
// All API calls go through this axios instance. Two interceptors do the work:
//
//   1. REQUEST  — attach `Authorization: Bearer <accessToken>` if we have one.
//   2. RESPONSE — on 401, attempt ONE refresh (deduplicated across parallel
//      failures) and retry the original request. If refresh fails, clear all
//      tokens and let the UI route the user to /login.
//
// Token storage lives in localStorage. For a learning project this is fine; in
// production you'd push to httpOnly cookies for XSS resistance.
// =============================================================================

import axios, { AxiosError, type InternalAxiosRequestConfig } from 'axios';
import type { AuthResponse } from './types';

// ── Token storage ────────────────────────────────────────────────────────────

const ACCESS_KEY  = 'pc.accessToken';
const REFRESH_KEY = 'pc.refreshToken';
const USER_KEY    = 'pc.user';

export interface StoredUser {
  userId: string;
  email: string;
  role: 'PHOTOGRAPHER' | 'CUSTOMER';
}

export const tokens = {
  get access(): string | null    { return localStorage.getItem(ACCESS_KEY); },
  get refresh(): string | null   { return localStorage.getItem(REFRESH_KEY); },
  get user(): StoredUser | null  {
    const raw = localStorage.getItem(USER_KEY);
    return raw ? (JSON.parse(raw) as StoredUser) : null;
  },
  save(auth: AuthResponse) {
    localStorage.setItem(ACCESS_KEY,  auth.accessToken);
    localStorage.setItem(REFRESH_KEY, auth.refreshToken);
    localStorage.setItem(USER_KEY, JSON.stringify({
      userId: auth.userId, email: auth.email, role: auth.role,
    }));
  },
  clear() {
    localStorage.removeItem(ACCESS_KEY);
    localStorage.removeItem(REFRESH_KEY);
    localStorage.removeItem(USER_KEY);
  },
};

// ── Axios instance ───────────────────────────────────────────────────────────

export const api = axios.create({
  baseURL: '/api/v1',     // Vite proxies /api → gateway :8080 in dev
  timeout: 10_000,
});

api.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const t = tokens.access;
  if (t) {
    config.headers.set('Authorization', `Bearer ${t}`);
  }
  return config;
});

// ── Refresh coordination ─────────────────────────────────────────────────────
// Multiple parallel requests can all 401 at once after the access token
// expires. We must only call /auth/refresh ONCE — auth-service rotates the
// refresh token on each use, so a second call would invalidate the chain.
// Pattern: store the in-flight refresh promise; every concurrent 401 awaits it.

let refreshPromise: Promise<string> | null = null;

async function refreshAccessToken(): Promise<string> {
  if (!refreshPromise) {
    const refresh = tokens.refresh;
    if (!refresh) throw new Error('No refresh token');

    refreshPromise = axios
      .post<AuthResponse>('/api/v1/auth/refresh', { refreshToken: refresh })
      .then(res => {
        tokens.save(res.data);
        return res.data.accessToken;
      })
      .finally(() => {
        // Allow future cycles to start their own refresh
        refreshPromise = null;
      });
  }
  return refreshPromise;
}

api.interceptors.response.use(
  res => res,
  async (error: AxiosError) => {
    const original = error.config as InternalAxiosRequestConfig & { _retried?: boolean };
    const status   = error.response?.status;

    // Only retry once, and only for 401 on a token-bearing request. /auth/login
    // and /auth/register failing with 401 are real auth failures — bubble those up.
    const isAuthEndpoint = original?.url?.includes('/auth/');
    if (status === 401 && !original?._retried && !isAuthEndpoint && tokens.refresh) {
      original._retried = true;
      try {
        const fresh = await refreshAccessToken();
        original.headers.set('Authorization', `Bearer ${fresh}`);
        return api(original);
      } catch {
        tokens.clear();
        // Force a hard redirect so React state resets. The router will land on /login.
        window.location.href = '/login';
        return Promise.reject(error);
      }
    }
    return Promise.reject(error);
  },
);

// ── Helper: pull a friendly message out of an error response ────────────────

import type { ApiErrorResponse } from './types';

export function errorMessage(err: unknown): string {
  if (axios.isAxiosError(err)) {
    const body = err.response?.data as Partial<ApiErrorResponse> | undefined;
    if (body?.message) return body.message;
    if (err.message) return err.message;
  }
  if (err instanceof Error) return err.message;
  return 'Something went wrong';
}

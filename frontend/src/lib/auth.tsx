// =============================================================================
// Auth context — single source of truth for "who is logged in"
// =============================================================================
// State lives in two places:
//   - localStorage (via the `tokens` helper) — survives page reloads
//   - React state                            — drives re-renders
//
// They start in sync on mount and stay that way because every mutation flows
// through this provider.
// =============================================================================

import { createContext, useCallback, useContext, useMemo, useState, type ReactNode } from 'react';
import { api, tokens, type StoredUser } from './api';
import type { AuthResponse, LoginRequest, RegisterRequest } from './types';

interface AuthContextValue {
  user: StoredUser | null;

  /** True while a login or register call is in flight. */
  isAuthLoading: boolean;

  /** Returns the freshly-authenticated user so the caller can route on role
   *  without waiting a render cycle for the context state to settle. */
  login: (req: LoginRequest) => Promise<StoredUser>;
  register: (req: RegisterRequest) => Promise<StoredUser>;
  logout: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<StoredUser | null>(() => tokens.user);
  const [isAuthLoading, setIsAuthLoading] = useState(false);

  const login = useCallback(async (req: LoginRequest): Promise<StoredUser> => {
    setIsAuthLoading(true);
    try {
      const { data } = await api.post<AuthResponse>('/auth/login', req);
      tokens.save(data);
      const stored: StoredUser = { userId: data.userId, email: data.email, role: data.role };
      setUser(stored);
      return stored;
    } finally {
      setIsAuthLoading(false);
    }
  }, []);

  const register = useCallback(async (req: RegisterRequest): Promise<StoredUser> => {
    setIsAuthLoading(true);
    try {
      const { data } = await api.post<AuthResponse>('/auth/register', req);
      tokens.save(data);
      const stored: StoredUser = { userId: data.userId, email: data.email, role: data.role };
      setUser(stored);
      return stored;
    } finally {
      setIsAuthLoading(false);
    }
  }, []);

  const logout = useCallback(async () => {
    // Best-effort server-side revoke — we don't block UI on it. If the call
    // fails (network, expired token), we still clear local state so the user
    // is "logged out" in every sense that matters to them.
    try {
      await api.post('/auth/logout');
    } catch {
      /* ignore */
    }
    tokens.clear();
    setUser(null);
  }, []);

  const value = useMemo<AuthContextValue>(
    () => ({ user, isAuthLoading, login, register, logout }),
    [user, isAuthLoading, login, register, logout],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used inside <AuthProvider>');
  return ctx;
}

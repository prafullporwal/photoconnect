import { Navigate, useLocation } from 'react-router-dom';
import type { ReactNode } from 'react';
import { useAuth } from '../lib/auth';
import type { Role } from '../lib/types';

/**
 * Guard for authenticated routes.
 *   - Unauthenticated      → redirect to /login (preserving the intended path).
 *   - Wrong role           → redirect to /  (with a flash via state, the UI can show a banner).
 *   - Otherwise            → render children.
 */
export function ProtectedRoute({
  children,
  allow,
}: {
  children: ReactNode;
  /** Optional role allow-list. If omitted, any authenticated user passes. */
  allow?: Role[];
}) {
  const { user } = useAuth();
  const location = useLocation();

  if (!user) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  }
  if (allow && !allow.includes(user.role)) {
    return <Navigate to="/" replace state={{ wrongRole: true }} />;
  }
  return <>{children}</>;
}

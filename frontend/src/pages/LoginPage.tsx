import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useState } from 'react';
import { useAuth } from '../lib/auth';
import { errorMessage } from '../lib/api';
import type { Role } from '../lib/types';

// Client-side validation. Mirrors the backend's @NotBlank constraints so the
// user gets feedback before a round-trip. The backend is still the source of
// truth — we never assume client validation has run.
const schema = z.object({
  email: z.string().email('Enter a valid email address'),
  password: z.string().min(1, 'Password is required'),
});

type LoginValues = z.infer<typeof schema>;

export function LoginPage() {
  const { login, isAuthLoading } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [serverError, setServerError] = useState<string | null>(null);

  // If a ProtectedRoute bounced the user here, `from` carries the original
  // target — we honour it ONLY when it's a route the user's role can see.
  // Otherwise we send them to the role's natural landing page:
  //   CUSTOMER     → /            (the marketplace browse page)
  //   PHOTOGRAPHER → /me/profile  (their own dashboard)
  const fromPath = (location.state as { from?: string })?.from;

  const form = useForm<LoginValues>({
    resolver: zodResolver(schema),
    defaultValues: { email: '', password: '' },
  });

  const onSubmit = async (values: LoginValues) => {
    setServerError(null);
    try {
      const user = await login(values);
      navigate(landingFor(user.role, fromPath), { replace: true });
    } catch (e) {
      setServerError(errorMessage(e));
    }
  };

  return (
    <div className="mx-auto max-w-md py-12">
      <h1 className="mb-6 text-2xl font-semibold text-gray-900">Log in</h1>

      <form
        onSubmit={form.handleSubmit(onSubmit)}
        className="space-y-4 rounded-lg border border-gray-200 bg-white p-6 shadow-sm"
      >
        <Field label="Email" error={form.formState.errors.email?.message}>
          <input
            type="email"
            autoComplete="email"
            {...form.register('email')}
            className="block w-full rounded-md border border-gray-300 px-3 py-2 focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
          />
        </Field>
        <Field label="Password" error={form.formState.errors.password?.message}>
          <input
            type="password"
            autoComplete="current-password"
            {...form.register('password')}
            className="block w-full rounded-md border border-gray-300 px-3 py-2 focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
          />
        </Field>

        {serverError && (
          <p className="rounded-md bg-red-50 px-3 py-2 text-sm text-red-700">
            {serverError}
          </p>
        )}

        <button
          type="submit"
          disabled={isAuthLoading}
          className="w-full rounded-md bg-indigo-600 px-4 py-2 font-medium text-white hover:bg-indigo-700 disabled:opacity-60"
        >
          {isAuthLoading ? 'Logging in…' : 'Log in'}
        </button>

        <p className="text-center text-sm text-gray-600">
          No account?{' '}
          <Link to="/register" className="font-medium text-indigo-600 hover:underline">
            Sign up
          </Link>
        </p>
      </form>
    </div>
  );
}

function Field({
  label,
  error,
  children,
}: {
  label: string;
  error?: string;
  children: React.ReactNode;
}) {
  return (
    <label className="block">
      <span className="mb-1 block text-sm font-medium text-gray-700">{label}</span>
      {children}
      {error && <span className="mt-1 block text-sm text-red-600">{error}</span>}
    </label>
  );
}

/**
 * Decide where to send a user immediately after login.
 *
 *   - If the user was deep-linked / bounced from a protected page, prefer that
 *     path UNLESS it isn't appropriate for their role (e.g. a photographer
 *     trying to land on a customer-only inbox).
 *   - Otherwise, fall back to the role's natural landing page.
 */
function landingFor(role: Role, fromPath: string | undefined): string {
  const customerHome     = '/';
  const photographerHome = '/me/profile';

  if (!fromPath) {
    return role === 'CUSTOMER' ? customerHome : photographerHome;
  }
  // Routes only a specific role can reach — don't bounce someone to a path
  // they'll just get redirected away from.
  const customerOnly     = ['/me/inquiries', '/inquiries/new'];
  const photographerOnly = ['/me/inbox', '/me/portfolio'];

  if (role === 'CUSTOMER' && photographerOnly.some(p => fromPath.startsWith(p))) {
    return customerHome;
  }
  if (role === 'PHOTOGRAPHER' && customerOnly.some(p => fromPath.startsWith(p))) {
    return photographerHome;
  }
  // Photographers cannot browse the marketplace — short-circuit to their home.
  if (role === 'PHOTOGRAPHER' && (fromPath === '/' || fromPath.startsWith('/photographers/'))) {
    return photographerHome;
  }
  return fromPath;
}

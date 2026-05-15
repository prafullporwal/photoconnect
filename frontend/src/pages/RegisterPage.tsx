import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Link, useNavigate } from 'react-router-dom';
import { useState } from 'react';
import { useAuth } from '../lib/auth';
import { errorMessage } from '../lib/api';

// Match the backend constraints: password min 8 chars (RegisterRequest @Size).
// Email format validation is purely UX — the server still has the final say.
const schema = z.object({
  email: z.string().email('Enter a valid email address'),
  password: z.string().min(8, 'Password must be at least 8 characters'),
  role: z.enum(['PHOTOGRAPHER', 'CUSTOMER']),
});

type RegisterValues = z.infer<typeof schema>;

export function RegisterPage() {
  const { register: registerUser, isAuthLoading } = useAuth();
  const navigate = useNavigate();
  const [serverError, setServerError] = useState<string | null>(null);

  const form = useForm<RegisterValues>({
    resolver: zodResolver(schema),
    defaultValues: { email: '', password: '', role: 'CUSTOMER' },
  });

  const onSubmit = async (values: RegisterValues) => {
    setServerError(null);
    try {
      await registerUser(values);
      // New accounts always start without a profile — go set one up.
      navigate('/me/profile', { replace: true });
    } catch (e) {
      setServerError(errorMessage(e));
    }
  };

  return (
    <div className="mx-auto max-w-md py-12">
      <h1 className="mb-6 text-2xl font-semibold text-gray-900">Create an account</h1>

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
            autoComplete="new-password"
            {...form.register('password')}
            className="block w-full rounded-md border border-gray-300 px-3 py-2 focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
          />
        </Field>

        <Field label="I am a..." error={form.formState.errors.role?.message}>
          <div className="grid grid-cols-2 gap-2">
            {(['CUSTOMER', 'PHOTOGRAPHER'] as const).map(role => (
              <label
                key={role}
                className="flex cursor-pointer items-center justify-center gap-2 rounded-md border border-gray-300 px-3 py-2 text-sm font-medium hover:bg-gray-50 has-[:checked]:border-indigo-500 has-[:checked]:bg-indigo-50 has-[:checked]:text-indigo-700"
              >
                <input
                  type="radio"
                  value={role}
                  {...form.register('role')}
                  className="sr-only"
                />
                {role === 'CUSTOMER' ? 'Customer' : 'Photographer'}
              </label>
            ))}
          </div>
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
          {isAuthLoading ? 'Creating account…' : 'Create account'}
        </button>

        <p className="text-center text-sm text-gray-600">
          Already have an account?{' '}
          <Link to="/login" className="font-medium text-indigo-600 hover:underline">
            Log in
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

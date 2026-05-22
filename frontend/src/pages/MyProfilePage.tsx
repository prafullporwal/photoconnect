import { useEffect, useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import axios from 'axios';
import { Link } from 'react-router-dom';
import { api, errorMessage } from '../lib/api';
import { useAuth } from '../lib/auth';
import { PortfolioGallery } from '../components/PortfolioGallery';
import type {
  ContactMethod,
  Customer,
  CreateCustomerRequest,
  UpdateCustomerRequest,
  PhotographerProfile,
  CreatePhotographerProfileRequest,
  UpdatePhotographerProfileRequest,
  PortfolioItem,
} from '../lib/types';

/**
 * Top-level dispatch: render the photographer editor or the customer editor
 * based on the authenticated user's role. Both sub-components share the same
 * load-or-create pattern but talk to different endpoints.
 */
export function MyProfilePage() {
  const { user } = useAuth();
  if (!user) return null;
  return user.role === 'PHOTOGRAPHER' ? <PhotographerEditor /> : <CustomerEditor />;
}

// ─────────────────────────────────────────────────────────────────────────────
// Photographer editor
// ─────────────────────────────────────────────────────────────────────────────

const SPECIALTIES = [
  'Reels', 'Wedding', 'Pre-Wedding', 'Maternity', 'Birthday', 'Corporate',
] as const;

const photographerSchema = z.object({
  displayName:       z.string().min(1, 'Display name is required').max(100),
  bio:               z.string().max(2000).optional(),
  location:          z.string().min(1, 'Location is required').max(200),
  // Plain z.number() (not z.coerce.number()) keeps zod input == output so the
  // RHF resolver generics line up. We let RHF do the string→number coercion at
  // the input layer via `valueAsNumber: true` on the register() calls below.
  yearsOfExperience: z.number().int().min(0).max(60),
  pricePerHour:      z.number().min(0),
  specialties:       z.array(z.string()),
  available:         z.boolean().optional(),
});
type PhotographerValues = z.infer<typeof photographerSchema>;

function PhotographerEditor() {
  const qc = useQueryClient();
  const [serverError, setServerError] = useState<string | null>(null);
  /**
   * View vs edit:
   *   - First visit (no profile yet) → form (forced into "create" mode below)
   *   - Returning with a profile      → summary view by default; "Edit profile"
   *                                     button flips this to true
   */
  const [editing, setEditing] = useState(false);

  // GET /photographers/me — may 404 if no profile exists yet.
  const { data, isLoading, error } = useQuery({
    queryKey: ['my-photographer-profile'],
    queryFn: async () => (await api.get<PhotographerProfile>('/photographers/me')).data,
    retry: (failureCount, err) => {
      // Don't retry a real 404 — it's the "no profile yet" signal.
      if (axios.isAxiosError(err) && err.response?.status === 404) return false;
      return failureCount < 1;
    },
  });

  const isNotFound = axios.isAxiosError(error) && error.response?.status === 404;
  const mode: 'create' | 'edit' = data ? 'edit' : 'create';

  const form = useForm<PhotographerValues>({
    resolver: zodResolver(photographerSchema),
    defaultValues: {
      displayName: '',
      bio: '',
      location: '',
      yearsOfExperience: 0,
      pricePerHour: 0,
      specialties: [],
      available: true,
    },
  });

  // Pre-fill the form once the GET completes successfully.
  useEffect(() => {
    if (data) {
      form.reset({
        displayName: data.displayName,
        bio: data.bio ?? '',
        location: data.location,
        yearsOfExperience: data.yearsOfExperience,
        pricePerHour: data.pricePerHour,
        specialties: data.specialties,
        available: data.available,
      });
    }
  }, [data, form]);

  const mutation = useMutation({
    mutationFn: async (values: PhotographerValues) => {
      if (mode === 'create') {
        const body: CreatePhotographerProfileRequest = {
          displayName: values.displayName,
          bio: values.bio || undefined,
          location: values.location,
          yearsOfExperience: values.yearsOfExperience,
          pricePerHour: values.pricePerHour,
          specialties: values.specialties,
        };
        const res = await api.post<PhotographerProfile>('/photographers/me', body);
        return res.data;
      }
      const body: UpdatePhotographerProfileRequest = {
        displayName: values.displayName,
        bio: values.bio || undefined,
        location: values.location,
        yearsOfExperience: values.yearsOfExperience,
        pricePerHour: values.pricePerHour,
        specialties: values.specialties,
        available: values.available ?? true,
      };
      const res = await api.put<PhotographerProfile>('/photographers/me', body);
      return res.data;
    },
    onSuccess: profile => {
      qc.setQueryData(['my-photographer-profile'], profile);
      qc.invalidateQueries({ queryKey: ['photographers'] });   // marketplace listing
      qc.invalidateQueries({ queryKey: ['feed'] });            // marketplace feed
      // Drop back to the summary view after a successful edit.
      setEditing(false);
    },
  });

  if (isLoading) return <PageLoading />;
  if (error && !isNotFound) {
    return <PageError message={errorMessage(error)} />;
  }

  // Default state for returning photographers: show their public profile +
  // portfolio. The form only renders during initial setup or explicit edit.
  if (data && !editing) {
    return <PhotographerSummaryView profile={data} onEdit={() => setEditing(true)} />;
  }

  return (
    <FormShell
      title={mode === 'create' ? 'Set up your photographer profile' : 'Your photographer profile'}
      subtitle={
        mode === 'create'
          ? 'Customers find you by browsing this list — fill these in carefully.'
          : null
      }
    >
      <form
        onSubmit={form.handleSubmit(values => {
          setServerError(null);
          mutation.mutate(values, { onError: e => setServerError(errorMessage(e)) });
        })}
        className="space-y-4"
      >
        <Field label="Display name" error={form.formState.errors.displayName?.message}>
          <Input {...form.register('displayName')} />
        </Field>

        <Field label="Bio" error={form.formState.errors.bio?.message} hint="Optional, up to 2000 characters.">
          <textarea
            rows={4}
            {...form.register('bio')}
            className="block w-full rounded-md border border-gray-300 px-3 py-2 focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
          />
        </Field>

        <div className="grid gap-4 sm:grid-cols-2">
          <Field label="Location" error={form.formState.errors.location?.message}>
            <Input {...form.register('location')} />
          </Field>
          <Field label="Years of experience" error={form.formState.errors.yearsOfExperience?.message}>
            <Input type="number" min={0} max={60} {...form.register('yearsOfExperience', { valueAsNumber: true })} />
          </Field>
        </div>

        <Field label="Price per hour (USD)" error={form.formState.errors.pricePerHour?.message}>
          <Input type="number" step="0.01" min={0} {...form.register('pricePerHour', { valueAsNumber: true })} />
        </Field>

        <fieldset>
          <legend className="mb-2 block text-sm font-medium text-gray-700">Specialties</legend>
          <div className="grid grid-cols-2 gap-2 sm:grid-cols-3">
            {SPECIALTIES.map(s => (
              <label key={s} className="flex items-center gap-2 text-sm text-gray-700">
                <input
                  type="checkbox"
                  value={s}
                  {...form.register('specialties')}
                  className="rounded border-gray-300 text-indigo-600 focus:ring-indigo-500"
                />
                {s}
              </label>
            ))}
          </div>
        </fieldset>

        {mode === 'edit' && (
          <label className="flex items-center gap-2 text-sm text-gray-700">
            <input type="checkbox" {...form.register('available')} className="rounded" />
            Open for new bookings
          </label>
        )}

        {serverError && <ErrorBanner message={serverError} />}

        <div className="flex items-center gap-2">
          <button
            type="submit"
            disabled={mutation.isPending}
            className="flex-1 rounded-md bg-indigo-600 px-4 py-2 font-medium text-white hover:bg-indigo-700 disabled:opacity-60"
          >
            {mutation.isPending
              ? 'Saving…'
              : mode === 'create'
                ? 'Create profile'
                : 'Save changes'}
          </button>

          {mode === 'edit' && (
            <button
              type="button"
              onClick={() => setEditing(false)}
              disabled={mutation.isPending}
              className="rounded-md border border-gray-300 px-4 py-2 font-medium text-gray-700 hover:bg-gray-50 disabled:opacity-60"
            >
              Cancel
            </button>
          )}
        </div>
      </form>
    </FormShell>
  );
}

// ─────────────────────────────────────────────────────────────────────────────
// Photographer summary view — shown when the profile already exists.
// Renders the profile beautifully (not as a form) plus the photographer's
// portfolio gallery, so the page doubles as a "this is what customers see"
// preview. The Edit button flips back to the form.
// ─────────────────────────────────────────────────────────────────────────────

function PhotographerSummaryView({
  profile,
  onEdit,
}: {
  profile: PhotographerProfile;
  onEdit: () => void;
}) {
  // Photographers can't hit /photographers/{id}/portfolio (blocked by @PreAuthorize),
  // so we use the owner-only endpoint to see their own work.
  const { data: portfolio, isLoading: portfolioLoading } = useQuery({
    queryKey: ['my-portfolio'],
    queryFn: async () =>
      (await api.get<PortfolioItem[]>('/photographers/me/portfolio')).data,
  });

  return (
    <div className="mx-auto max-w-4xl px-4 py-8">
      {/* ── Hero card ─────────────────────────────────────────────────────── */}
      <div className="overflow-hidden rounded-lg border border-gray-200 bg-white shadow-sm">
        {/* Top band: name + status pill + Edit */}
        <div className="bg-gradient-to-br from-indigo-50 via-white to-white px-6 py-7">
          <div className="flex flex-wrap items-start justify-between gap-3">
            <div>
              <div className="flex items-center gap-2">
                <h1 className="text-3xl font-semibold text-gray-900">
                  {profile.displayName}
                </h1>
                <AvailabilityPill available={profile.available} />
              </div>
              <p className="mt-1 text-gray-600">
                {profile.location} · {profile.yearsOfExperience} years experience
              </p>
            </div>
            <div className="flex flex-col items-end gap-2">
              <p className="text-3xl font-semibold text-gray-900">
                ${profile.pricePerHour.toFixed(2)}
                <span className="text-base font-normal text-gray-500">/hour</span>
              </p>
              <button
                onClick={onEdit}
                className="rounded-md border border-gray-300 bg-white px-3 py-1.5 text-sm font-medium text-gray-700 shadow-sm hover:bg-gray-50"
              >
                Edit profile
              </button>
            </div>
          </div>
        </div>

        {/* Body: bio + specialties */}
        <div className="space-y-6 px-6 py-6">
          {profile.bio ? (
            <p className="whitespace-pre-wrap leading-relaxed text-gray-700">
              {profile.bio}
            </p>
          ) : (
            <p className="rounded-md bg-amber-50 px-3 py-2 text-sm text-amber-800">
              No bio yet — customers see this section blank.{' '}
              <button
                onClick={onEdit}
                className="font-medium text-amber-900 underline hover:no-underline"
              >
                Add one
              </button>
              .
            </p>
          )}

          {profile.specialties.length > 0 && (
            <div>
              <h2 className="mb-2 text-xs font-semibold uppercase tracking-wider text-gray-500">
                Specialties
              </h2>
              <ul className="flex flex-wrap gap-2">
                {profile.specialties.map(s => (
                  <li
                    key={s}
                    className="rounded-full bg-indigo-50 px-3 py-1 text-sm font-medium text-indigo-700"
                  >
                    {s}
                  </li>
                ))}
              </ul>
            </div>
          )}
        </div>
      </div>

      {/* ── Portfolio section ──────────────────────────────────────────────── */}
      <section className="mt-10">
        <header className="mb-5 flex flex-wrap items-end justify-between gap-3">
          <div>
            <h2 className="bg-gradient-to-r from-indigo-600 via-violet-600 to-fuchsia-500 bg-clip-text text-2xl font-bold tracking-tight text-transparent">
              My portfolio
            </h2>
            <p className="mt-1 text-sm text-slate-500">
              This is exactly what customers see on your public profile.
            </p>
          </div>
          <Link
            to="/me/portfolio"
            className="rounded-full border border-slate-200 bg-white px-4 py-1.5 text-sm font-medium text-slate-700 shadow-sm hover:bg-slate-50 hover:text-slate-900"
          >
            Manage portfolio →
          </Link>
        </header>

        {portfolioLoading ? (
          <PortfolioSkeleton />
        ) : (
          <PortfolioGallery
            items={portfolio ?? []}
            photographerName={profile.displayName}
          />
        )}
      </section>
    </div>
  );
}

function PortfolioSkeleton() {
  const aspects = ['aspect-[4/3]', 'aspect-[9/16]', 'aspect-[4/3]', 'aspect-[4/3]', 'aspect-[9/16]', 'aspect-[4/3]'];
  return (
    <ul className="columns-1 gap-3 sm:columns-2 lg:columns-3">
      {aspects.map((a, i) => (
        <li key={i} className={`mb-4 break-inside-avoid animate-pulse rounded-2xl bg-slate-200/70 ring-1 ring-slate-200/80 ${a}`} />
      ))}
    </ul>
  );
}

function AvailabilityPill({ available }: { available: boolean }) {
  return available ? (
    <span className="inline-flex items-center gap-1 rounded-full bg-green-50 px-2 py-0.5 text-xs font-medium text-green-700 ring-1 ring-inset ring-green-200">
      <span className="h-1.5 w-1.5 rounded-full bg-green-500" /> Available
    </span>
  ) : (
    <span className="inline-flex items-center gap-1 rounded-full bg-gray-100 px-2 py-0.5 text-xs font-medium text-gray-600 ring-1 ring-inset ring-gray-200">
      <span className="h-1.5 w-1.5 rounded-full bg-gray-400" /> Paused
    </span>
  );
}

// ─────────────────────────────────────────────────────────────────────────────
// Customer editor
// ─────────────────────────────────────────────────────────────────────────────

const customerSchema = z.object({
  displayName:            z.string().min(1, 'Display name is required').max(100),
  location:               z.string().max(200).optional(),
  phoneNumber:            z.string().max(30).optional(),
  preferredContactMethod: z.enum(['EMAIL', 'PHONE', 'PLATFORM']),
});
type CustomerValues = z.infer<typeof customerSchema>;

function CustomerEditor() {
  const qc = useQueryClient();
  const [serverError, setServerError] = useState<string | null>(null);

  const { data, isLoading, error } = useQuery({
    queryKey: ['my-customer-profile'],
    queryFn: async () => (await api.get<Customer>('/customers/me')).data,
    retry: (failureCount, err) => {
      if (axios.isAxiosError(err) && err.response?.status === 404) return false;
      return failureCount < 1;
    },
  });
  const isNotFound = axios.isAxiosError(error) && error.response?.status === 404;
  const mode: 'create' | 'edit' = data ? 'edit' : 'create';

  const form = useForm<CustomerValues>({
    resolver: zodResolver(customerSchema),
    defaultValues: {
      displayName: '',
      location: '',
      phoneNumber: '',
      preferredContactMethod: 'EMAIL',
    },
  });

  useEffect(() => {
    if (data) {
      form.reset({
        displayName: data.displayName,
        location: data.location ?? '',
        phoneNumber: data.phoneNumber ?? '',
        preferredContactMethod: data.preferredContactMethod,
      });
    }
  }, [data, form]);

  const mutation = useMutation({
    mutationFn: async (values: CustomerValues) => {
      const body: CreateCustomerRequest | UpdateCustomerRequest = {
        displayName: values.displayName,
        location: values.location || undefined,
        phoneNumber: values.phoneNumber || undefined,
        preferredContactMethod: values.preferredContactMethod as ContactMethod,
      };
      const verb = mode === 'create' ? api.post : api.put;
      const res = await verb<Customer>('/customers/me', body);
      return res.data;
    },
    onSuccess: c => qc.setQueryData(['my-customer-profile'], c),
  });

  if (isLoading) return <PageLoading />;
  if (error && !isNotFound) return <PageError message={errorMessage(error)} />;

  return (
    <FormShell
      title={mode === 'create' ? 'Tell us about yourself' : 'Your account'}
      subtitle={
        mode === 'create'
          ? 'Photographers will see this when you send them an inquiry.'
          : null
      }
    >
      <form
        onSubmit={form.handleSubmit(values => {
          setServerError(null);
          mutation.mutate(values, { onError: e => setServerError(errorMessage(e)) });
        })}
        className="space-y-4"
      >
        <Field label="Display name" error={form.formState.errors.displayName?.message}>
          <Input {...form.register('displayName')} />
        </Field>

        <Field label="Location" error={form.formState.errors.location?.message} hint="Optional.">
          <Input {...form.register('location')} />
        </Field>

        <Field label="Phone number" error={form.formState.errors.phoneNumber?.message} hint="Optional.">
          <Input {...form.register('phoneNumber')} />
        </Field>

        <Field
          label="Preferred contact method"
          error={form.formState.errors.preferredContactMethod?.message}
        >
          <select
            {...form.register('preferredContactMethod')}
            className="block w-full rounded-md border border-gray-300 px-3 py-2 focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
          >
            <option value="EMAIL">Email</option>
            <option value="PHONE">Phone</option>
            <option value="PLATFORM">PhotoConnect messages</option>
          </select>
        </Field>

        {serverError && <ErrorBanner message={serverError} />}

        <button
          type="submit"
          disabled={mutation.isPending}
          className="w-full rounded-md bg-indigo-600 px-4 py-2 font-medium text-white hover:bg-indigo-700 disabled:opacity-60"
        >
          {mutation.isPending
            ? 'Saving…'
            : mode === 'create'
              ? 'Create profile'
              : 'Save changes'}
        </button>
      </form>
    </FormShell>
  );
}

// ─────────────────────────────────────────────────────────────────────────────
// Small shared building blocks
// ─────────────────────────────────────────────────────────────────────────────

function FormShell({
  title,
  subtitle,
  children,
}: {
  title: string;
  subtitle?: string | null;
  children: React.ReactNode;
}) {
  return (
    <div className="mx-auto max-w-2xl px-4 py-8">
      <h1 className="text-2xl font-semibold text-gray-900">{title}</h1>
      {subtitle && <p className="mt-1 text-gray-600">{subtitle}</p>}
      <div className="mt-6 rounded-lg border border-gray-200 bg-white p-6 shadow-sm">
        {children}
      </div>
    </div>
  );
}

function Field({
  label,
  error,
  hint,
  children,
}: {
  label: string;
  error?: string;
  hint?: string;
  children: React.ReactNode;
}) {
  return (
    <label className="block">
      <span className="mb-1 block text-sm font-medium text-gray-700">{label}</span>
      {children}
      {hint && !error && <span className="mt-1 block text-xs text-gray-500">{hint}</span>}
      {error && <span className="mt-1 block text-sm text-red-600">{error}</span>}
    </label>
  );
}

function Input(props: React.InputHTMLAttributes<HTMLInputElement>) {
  return (
    <input
      {...props}
      className="block w-full rounded-md border border-gray-300 px-3 py-2 focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
    />
  );
}

function ErrorBanner({ message }: { message: string }) {
  return (
    <p className="rounded-md bg-red-50 px-3 py-2 text-sm text-red-700">{message}</p>
  );
}

function PageLoading() {
  return <p className="mx-auto max-w-2xl px-4 py-8 text-gray-500">Loading…</p>;
}

function PageError({ message }: { message: string }) {
  return (
    <div className="mx-auto max-w-2xl px-4 py-8">
      <ErrorBanner message={message} />
    </div>
  );
}

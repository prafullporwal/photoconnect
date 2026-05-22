import { useQuery } from '@tanstack/react-query';
import { Link, useParams } from 'react-router-dom';
import { api, errorMessage } from '../lib/api';
import { useAuth } from '../lib/auth';
import { PortfolioGallery } from '../components/PortfolioGallery';
import type { PhotographerProfile, PortfolioItem } from '../lib/types';

/**
 * Customer-facing photographer detail page.
 *
 * Two parallel queries:
 *   1. The profile itself (name, bio, price, specialties).
 *   2. The portfolio gallery (images / videos / reels by category).
 *
 * Both endpoints are gated by {@code !hasRole('PHOTOGRAPHER')} on the backend,
 * so a logged-in photographer who lands here would get 403s — the SPA also
 * hides the route from them at the nav level as defence in depth.
 */
export function PhotographerDetailPage() {
  const { id } = useParams<{ id: string }>();
  const { user } = useAuth();

  const profileQuery = useQuery({
    queryKey: ['photographer', id],
    queryFn: async () => (await api.get<PhotographerProfile>(`/photographers/${id}`)).data,
    enabled: Boolean(id),
  });

  // Portfolio query runs in parallel — we don't block the page on it.
  // A 403 (e.g. photographer poking around) renders as an empty gallery.
  const portfolioQuery = useQuery({
    queryKey: ['photographer-portfolio', id],
    queryFn: async () =>
      (await api.get<PortfolioItem[]>(`/photographers/${id}/portfolio`)).data,
    enabled: Boolean(id),
    retry: false,
  });

  if (profileQuery.isLoading) {
    return <p className="mx-auto max-w-3xl px-4 py-12 text-gray-500">Loading…</p>;
  }
  if (profileQuery.error || !profileQuery.data) {
    return (
      <div className="mx-auto max-w-3xl px-4 py-12">
        <div className="rounded-md bg-red-50 px-4 py-3 text-sm text-red-700">
          {errorMessage(profileQuery.error) || 'Photographer not found.'}
        </div>
        <Link to="/" className="mt-4 inline-block text-sm text-indigo-600 hover:underline">
          ← Back to browse
        </Link>
      </div>
    );
  }
  const data = profileQuery.data;

  // Build a deterministic colour for the avatar circle from the display name.
  // Avoids needing a real image while still giving each photographer a unique
  // visual identity.
  const avatarGradient = gradientFromName(data.displayName);

  return (
    <div className="mx-auto max-w-4xl px-4 pb-12">
      <Link
        to="/"
        className="mb-4 inline-flex items-center gap-1 pt-6 text-sm font-medium text-indigo-600 hover:underline"
      >
        ← Back to browse
      </Link>

      {/* ── Hero band ────────────────────────────────────────────────────── */}
      <div className="overflow-hidden rounded-3xl bg-white shadow-sm ring-1 ring-slate-200/70">
        {/* Decorative gradient banner */}
        <div className="relative h-32 bg-gradient-to-br from-indigo-500 via-violet-500 to-fuchsia-500">
          {/* Soft noise overlay (svg pattern) for depth */}
          <div
            aria-hidden="true"
            className="absolute inset-0 opacity-20 mix-blend-overlay"
            style={{
              backgroundImage:
                "url(\"data:image/svg+xml,%3Csvg viewBox='0 0 200 200' xmlns='http://www.w3.org/2000/svg'%3E%3Cfilter id='n'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.85' numOctaves='2'/%3E%3CfeColorMatrix values='0 0 0 0 1 0 0 0 0 1 0 0 0 0 1 0 0 0 0.55 0'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23n)'/%3E%3C/svg%3E\")",
            }}
          />
        </div>

        {/* Content overlapping the banner */}
        <div className="relative px-6 pb-6">
          {/* Avatar — sits on the banner edge */}
          <div className="-mt-12 flex flex-wrap items-end justify-between gap-4">
            <div className="flex items-end gap-4">
              <div
                className={`grid h-24 w-24 place-items-center rounded-2xl bg-gradient-to-br ${avatarGradient} text-3xl font-bold text-white shadow-lg ring-4 ring-white`}
              >
                {initialsFor(data.displayName)}
              </div>
              <div className="pb-1">
                <h1 className="text-2xl font-bold tracking-tight text-slate-900 sm:text-3xl">
                  {data.displayName}
                </h1>
                <p className="mt-0.5 flex items-center gap-1.5 text-sm text-slate-500">
                  <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="h-4 w-4">
                    <path d="M20 10c0 6-8 12-8 12s-8-6-8-12a8 8 0 0 1 16 0Z"/><circle cx="12" cy="10" r="3"/>
                  </svg>
                  {data.location}
                </p>
              </div>
            </div>

            <div className="text-right">
              <p className="text-sm text-slate-500">
                {data.yearsOfExperience} years experience
              </p>
            </div>
          </div>

          {data.bio && (
            <p className="mt-6 leading-relaxed text-slate-700">{data.bio}</p>
          )}

          {data.specialties.length > 0 && (
            <div className="mt-6">
              <h2 className="mb-2 text-xs font-semibold uppercase tracking-wider text-slate-500">
                Specialties
              </h2>
              <ul className="flex flex-wrap gap-1.5">
                {data.specialties.map(s => (
                  <li
                    key={s}
                    className="rounded-full bg-slate-100 px-3 py-1 text-sm font-medium text-slate-700"
                  >
                    {s}
                  </li>
                ))}
              </ul>
            </div>
          )}

          {/* CTA row */}
          <div className="mt-6 flex flex-wrap items-center gap-3 border-t border-slate-100 pt-5">
            {user?.role === 'CUSTOMER' ? (
              <>
                <Link
                  to={`/inquiries/new?photographerId=${data.id}`}
                  className="inline-flex items-center gap-1.5 rounded-full bg-gradient-to-br from-indigo-600 to-violet-600 px-5 py-2.5 text-sm font-semibold text-white shadow-md transition hover:shadow-lg hover:brightness-110"
                >
                  Send inquiry
                  <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round" className="h-4 w-4">
                    <path d="M5 12h14M13 5l7 7-7 7"/>
                  </svg>
                </Link>
              </>
            ) : user?.role === 'PHOTOGRAPHER' ? (
              <p className="text-sm text-slate-500">
                You're logged in as a photographer — only customers can send inquiries.
              </p>
            ) : (
              <p className="text-sm text-slate-600">
                <Link to="/register" className="font-semibold text-indigo-600 hover:underline">
                  Sign up
                </Link>{' '}
                as a customer to send {data.displayName.split(' ')[0]} an inquiry.
              </p>
            )}
          </div>
        </div>
      </div>

      {/* ── Portfolio (lives in its own card so the hero stays clean) ────── */}
      <section className="mt-6 rounded-3xl bg-white p-6 shadow-sm ring-1 ring-slate-200/70">
        <h2 className="mb-4 text-xs font-semibold uppercase tracking-wider text-slate-500">
          Portfolio
        </h2>
        {portfolioQuery.isLoading ? (
          <p className="text-sm text-slate-500">Loading portfolio…</p>
        ) : (
          <PortfolioGallery
            items={portfolioQuery.data ?? []}
            photographerName={data.displayName}
          />
        )}
      </section>
    </div>
  );
}

/**
 * Two-letter initials for the avatar — first letter of the first two
 * whitespace-separated tokens, falling back to the first two chars.
 */
function initialsFor(name: string): string {
  const parts = name.trim().split(/\s+/);
  if (parts.length >= 2) return (parts[0][0] + parts[1][0]).toUpperCase();
  return name.slice(0, 2).toUpperCase();
}

/**
 * Deterministic gradient class pair derived from the photographer's name —
 * gives each photographer a unique avatar colour without needing a real image.
 */
function gradientFromName(name: string): string {
  const palettes = [
    'from-indigo-500 to-violet-500',
    'from-fuchsia-500 to-pink-500',
    'from-rose-500 to-orange-500',
    'from-amber-500 to-rose-500',
    'from-emerald-500 to-teal-500',
    'from-sky-500 to-indigo-500',
    'from-cyan-500 to-blue-500',
    'from-violet-500 to-fuchsia-500',
  ];
  // Cheap hash — sum of char codes mod palette length.
  let h = 0;
  for (let i = 0; i < name.length; i++) h = (h + name.charCodeAt(i)) | 0;
  return palettes[Math.abs(h) % palettes.length];
}

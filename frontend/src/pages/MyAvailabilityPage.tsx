import { useMemo, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { api, errorMessage } from '../lib/api';
import type {
  AddAvailabilityRequest,
  AvailabilitySlot,
} from '../lib/types';

/**
 * /me/availability — photographer's calendar editor.
 *
 * <h2>Interaction model</h2>
 * <p>The page renders the current month (with prev/next buttons). Each day
 * cell is one of:</p>
 * <ul>
 *   <li><b>Posted</b> — already marked available on the server. Clicking
 *       deletes it via {@code DELETE /me/availability/{id}} after a confirm.</li>
 *   <li><b>Pending</b> — toggled in this session but not saved yet. Clicking
 *       toggles back off in local state.</li>
 *   <li><b>Empty</b> — clicking promotes it to pending.</li>
 *   <li><b>Past</b> — disabled, can't be toggled.</li>
 * </ul>
 *
 * <p>The "Save" button posts the pending set in a single bulk call. The
 * backend is idempotent — dates already posted are silently skipped — so a
 * pending day that races against an external add still ends up in the right
 * state.</p>
 *
 * <h2>Why no drag-select / range</h2>
 * <p>Photography availability typically clusters (a few weekends, a holiday
 * week) rather than spanning long ranges. Single-day clicks are intuitive
 * and the UI stays trivial. If usage shows long ranges are common, add a
 * shift-click range select later.</p>
 */
export function MyAvailabilityPage() {
  const qc = useQueryClient();
  const today = useMemo(() => startOfDay(new Date()), []);
  const [cursor, setCursor] = useState(() => startOfMonth(today));
  const [pending, setPending] = useState<Set<string>>(new Set());
  const [serverError, setServerError] = useState<string | null>(null);

  // ── Server state ──────────────────────────────────────────────────────────
  const slotsQuery = useQuery({
    queryKey: ['my-availability'],
    queryFn: async () =>
      (await api.get<AvailabilitySlot[]>('/photographers/me/availability')).data,
  });

  // Lookup: ISO date string -> slot id, for the click handler on posted days.
  const postedById = useMemo(() => {
    const map = new Map<string, string>();
    (slotsQuery.data ?? []).forEach(s => map.set(s.availableDate, s.id));
    return map;
  }, [slotsQuery.data]);

  // ── Mutations ─────────────────────────────────────────────────────────────
  const addMutation = useMutation({
    mutationFn: async (body: AddAvailabilityRequest) => {
      const res = await api.post<AvailabilitySlot[]>(
        '/photographers/me/availability',
        body,
      );
      return res.data;
    },
    onSuccess: data => {
      qc.setQueryData(['my-availability'], data);
      setPending(new Set());
    },
  });

  const removeMutation = useMutation({
    mutationFn: async (slotId: string) => {
      await api.delete(`/photographers/me/availability/${slotId}`);
    },
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['my-availability'] });
    },
  });

  // ── Click handlers ────────────────────────────────────────────────────────
  const togglePending = (iso: string) => {
    setServerError(null);
    setPending(prev => {
      const next = new Set(prev);
      if (next.has(iso)) next.delete(iso);
      else next.add(iso);
      return next;
    });
  };

  const removePosted = (iso: string) => {
    const id = postedById.get(iso);
    if (!id) return;
    if (!confirm(`Remove ${iso} from your calendar?`)) return;
    setServerError(null);
    removeMutation.mutate(id, {
      onError: e => setServerError(errorMessage(e)),
    });
  };

  const savePending = () => {
    if (pending.size === 0) return;
    setServerError(null);
    addMutation.mutate(
      { dates: Array.from(pending) },
      { onError: e => setServerError(errorMessage(e)) },
    );
  };

  // ── Calendar grid ─────────────────────────────────────────────────────────
  const grid = buildMonthGrid(cursor);

  return (
    <div className="mx-auto max-w-3xl px-4 py-8">
      <header className="mb-6">
        <h1 className="bg-gradient-to-r from-emerald-500 via-teal-500 to-cyan-500 bg-clip-text text-3xl font-bold tracking-tight text-transparent sm:text-4xl">
          My availability
        </h1>
        <p className="mt-2 text-sm text-slate-600">
          Click days you can take bookings. Customers see these as suggested
          dates when they send you an inquiry. An empty calendar means
          "open to any date."
        </p>
      </header>

      {/* ── Month switcher ─────────────────────────────────────────────────── */}
      <div className="mb-3 flex items-center justify-between">
        <button
          type="button"
          onClick={() => setCursor(addMonths(cursor, -1))}
          disabled={isSameMonth(cursor, today)}
          aria-label="Previous month"
          className="rounded-full p-2 text-slate-500 hover:bg-slate-100 disabled:opacity-30"
        >
          <ArrowIcon dir="left" />
        </button>
        <h2 className="text-lg font-semibold text-slate-800">
          {cursor.toLocaleDateString(undefined, {
            month: 'long',
            year: 'numeric',
          })}
        </h2>
        <button
          type="button"
          onClick={() => setCursor(addMonths(cursor, 1))}
          aria-label="Next month"
          className="rounded-full p-2 text-slate-500 hover:bg-slate-100"
        >
          <ArrowIcon dir="right" />
        </button>
      </div>

      {/* ── Grid ──────────────────────────────────────────────────────────── */}
      <div className="overflow-hidden rounded-2xl bg-white shadow-sm ring-1 ring-slate-200/80">
        <div className="grid grid-cols-7 border-b border-slate-100 bg-slate-50 text-center text-[11px] font-semibold uppercase tracking-wider text-slate-500">
          {['S', 'M', 'T', 'W', 'T', 'F', 'S'].map((d, i) => (
            <div key={i} className="py-2">
              {d}
            </div>
          ))}
        </div>

        <div className="grid grid-cols-7 gap-px bg-slate-100">
          {grid.map((day, idx) => {
            if (!day) {
              return <div key={idx} className="aspect-square bg-white" />;
            }
            const iso = toIsoDate(day);
            const isPast = day < today;
            const isPosted = postedById.has(iso);
            const isPending = pending.has(iso);
            const isToday = sameDay(day, today);

            const base = 'aspect-square w-full text-sm font-medium transition focus:outline-none focus:ring-2 focus:ring-indigo-400';
            let cls: string;
            if (isPast) {
              cls = 'cursor-not-allowed bg-slate-50 text-slate-300';
            } else if (isPosted) {
              cls = 'bg-emerald-500 text-white hover:bg-emerald-600';
            } else if (isPending) {
              cls = 'bg-emerald-100 text-emerald-800 ring-1 ring-inset ring-emerald-300';
            } else {
              cls = 'bg-white text-slate-700 hover:bg-slate-50';
            }

            return (
              <button
                key={idx}
                type="button"
                disabled={isPast}
                onClick={() => (isPosted ? removePosted(iso) : togglePending(iso))}
                aria-pressed={isPosted || isPending}
                aria-label={`${day.toDateString()}${isPosted ? ', currently available — click to remove' : isPending ? ', will be added on save' : ''}`}
                className={`${base} ${cls}`}
              >
                <span
                  className={`inline-flex h-7 w-7 items-center justify-center rounded-full ${
                    isToday && !isPosted && !isPending
                      ? 'ring-1 ring-indigo-400'
                      : ''
                  }`}
                >
                  {day.getDate()}
                </span>
              </button>
            );
          })}
        </div>
      </div>

      {/* ── Legend ────────────────────────────────────────────────────────── */}
      <div className="mt-4 flex flex-wrap items-center gap-4 text-xs text-slate-600">
        <Legend swatch="bg-emerald-500" label="Available (click to remove)" />
        <Legend swatch="bg-emerald-100 ring-1 ring-emerald-300" label="Pending save" />
        <Legend swatch="bg-white ring-1 ring-slate-300" label="Open day" />
        <Legend swatch="bg-slate-100" label="Past" />
      </div>

      {/* ── Save bar ──────────────────────────────────────────────────────── */}
      {(pending.size > 0 || serverError) && (
        <div className="sticky bottom-4 mt-6 flex items-center justify-between rounded-2xl bg-slate-900 px-5 py-3 text-white shadow-2xl ring-1 ring-slate-800/50">
          <p className="text-sm">
            {pending.size > 0
              ? `${pending.size} day${pending.size === 1 ? '' : 's'} to add`
              : 'Nothing to save'}
            {serverError && (
              <span className="ml-3 inline-block rounded-full bg-rose-500/20 px-2 py-0.5 text-xs text-rose-200">
                {serverError}
              </span>
            )}
          </p>
          <div className="flex items-center gap-2">
            <button
              type="button"
              onClick={() => setPending(new Set())}
              disabled={pending.size === 0 || addMutation.isPending}
              className="rounded-full px-3 py-1.5 text-sm font-medium text-slate-300 hover:bg-white/10 disabled:opacity-40"
            >
              Clear
            </button>
            <button
              type="button"
              onClick={savePending}
              disabled={pending.size === 0 || addMutation.isPending}
              className="rounded-full bg-emerald-500 px-4 py-1.5 text-sm font-semibold text-white shadow-md transition hover:bg-emerald-400 disabled:opacity-50"
            >
              {addMutation.isPending ? 'Saving…' : 'Save'}
            </button>
          </div>
        </div>
      )}

      {slotsQuery.isLoading && (
        <p className="mt-4 text-sm text-slate-500">Loading your calendar…</p>
      )}
      {slotsQuery.error && (
        <p className="mt-4 rounded-md bg-rose-50 px-3 py-2 text-sm text-rose-700">
          {errorMessage(slotsQuery.error)}
        </p>
      )}
    </div>
  );
}

// ─────────────────────────────────────────────────────────────────────────────
// Date helpers (kept local — only this page uses them right now)
// ─────────────────────────────────────────────────────────────────────────────

function startOfDay(d: Date): Date {
  return new Date(d.getFullYear(), d.getMonth(), d.getDate());
}

function startOfMonth(d: Date): Date {
  return new Date(d.getFullYear(), d.getMonth(), 1);
}

function addMonths(d: Date, n: number): Date {
  return new Date(d.getFullYear(), d.getMonth() + n, 1);
}

function isSameMonth(a: Date, b: Date): boolean {
  return a.getFullYear() === b.getFullYear() && a.getMonth() === b.getMonth();
}

function sameDay(a: Date, b: Date): boolean {
  return (
    a.getFullYear() === b.getFullYear() &&
    a.getMonth() === b.getMonth() &&
    a.getDate() === b.getDate()
  );
}

function toIsoDate(d: Date): string {
  // YYYY-MM-DD with no time/tz drift. toISOString() converts to UTC first
  // and can roll back a day for some locales; doing it manually avoids that.
  const yyyy = d.getFullYear();
  const mm = String(d.getMonth() + 1).padStart(2, '0');
  const dd = String(d.getDate()).padStart(2, '0');
  return `${yyyy}-${mm}-${dd}`;
}

/**
 * Build a 6-row × 7-column grid for the given month. Leading and trailing
 * cells outside the month are `null` so the renderer leaves them blank.
 */
function buildMonthGrid(cursor: Date): Array<Date | null> {
  const first = startOfMonth(cursor);
  const lastDay = new Date(first.getFullYear(), first.getMonth() + 1, 0).getDate();
  const startWeekday = first.getDay(); // 0 = Sunday
  const cells: Array<Date | null> = [];
  for (let i = 0; i < startWeekday; i++) cells.push(null);
  for (let day = 1; day <= lastDay; day++) {
    cells.push(new Date(first.getFullYear(), first.getMonth(), day));
  }
  while (cells.length % 7 !== 0) cells.push(null);
  // Pad to 6 rows so the grid height doesn't jump month-to-month.
  while (cells.length < 42) cells.push(null);
  return cells;
}

// ─────────────────────────────────────────────────────────────────────────────
// Small visual helpers

function Legend({ swatch, label }: { swatch: string; label: string }) {
  return (
    <span className="inline-flex items-center gap-1.5">
      <span className={`inline-block h-3 w-3 rounded-sm ${swatch}`} />
      {label}
    </span>
  );
}

function ArrowIcon({ dir }: { dir: 'left' | 'right' }) {
  return (
    <svg
      xmlns="http://www.w3.org/2000/svg"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="2.2"
      strokeLinecap="round"
      strokeLinejoin="round"
      className="h-4 w-4"
      aria-hidden="true"
    >
      {dir === 'left' ? (
        <path d="M15 18l-6-6 6-6" />
      ) : (
        <path d="M9 18l6-6-6-6" />
      )}
    </svg>
  );
}

import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { api, errorMessage } from '../lib/api';
import type { Inquiry } from '../lib/types';
import { StatusBadge } from '../components/StatusBadge';

/**
 * One component, two URLs:
 *   /me/inquiries  → mode="outbox"  (customers see their sent inquiries)
 *   /me/inbox      → mode="inbox"   (photographers see inquiries received)
 *
 * The only thing that changes is the endpoint and the wording. Factoring this
 * out keeps the visual list logic identical for both roles.
 */
export function InquiryListPage({ mode }: { mode: 'outbox' | 'inbox' }) {
  const endpoint = mode === 'outbox' ? '/inquiries/mine' : '/inquiries/received';
  const queryKey = mode === 'outbox' ? 'my-inquiries' : 'received-inquiries';

  const { data, isLoading, error } = useQuery({
    queryKey: [queryKey],
    queryFn: async () => (await api.get<Inquiry[]>(endpoint)).data,
  });

  const title = mode === 'outbox' ? 'My inquiries' : 'Inbox';
  const empty = mode === 'outbox'
    ? "You haven't sent any inquiries yet."
    : "You haven't received any inquiries yet.";

  return (
    <div className="mx-auto max-w-4xl px-4 py-8">
      <div className="mb-6 flex items-baseline justify-between">
        <h1 className="text-2xl font-semibold text-gray-900">{title}</h1>
        {data && <span className="text-sm text-gray-500">{data.length} total</span>}
      </div>

      {isLoading && <p className="text-gray-500">Loading…</p>}
      {error && <p className="rounded-md bg-red-50 px-3 py-2 text-sm text-red-700">{errorMessage(error)}</p>}

      {data && data.length === 0 && (
        <div className="rounded-lg border border-dashed border-gray-300 bg-white p-12 text-center text-gray-500">
          {empty}
          {mode === 'outbox' && (
            <p className="mt-2 text-sm">
              <Link to="/" className="text-indigo-600 hover:underline">
                Browse photographers
              </Link>{' '}
              to send your first one.
            </p>
          )}
        </div>
      )}

      {data && data.length > 0 && (
        <ul className="divide-y divide-gray-100 rounded-lg border border-gray-200 bg-white shadow-sm">
          {data.map(inq => (
            <li key={inq.id}>
              <Link
                to={`/inquiries/${inq.id}`}
                className="block px-4 py-4 transition hover:bg-gray-50"
              >
                <div className="flex items-baseline justify-between gap-2">
                  <span className="font-medium text-gray-900">{inq.eventType}</span>
                  <StatusBadge status={inq.status} />
                </div>
                <p className="mt-1 text-sm text-gray-600 line-clamp-2">{inq.message}</p>
                <div className="mt-2 flex flex-wrap gap-x-4 gap-y-1 text-xs text-gray-500">
                  <span>Event: {inq.eventDate}</span>
                  {inq.location && <span>· {inq.location}</span>}
                  {inq.budget && <span>· ${inq.budget.toFixed(2)}</span>}
                  <span>· Sent {new Date(inq.createdAt).toLocaleDateString()}</span>
                </div>
              </Link>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}

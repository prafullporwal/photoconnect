import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Link, useParams } from 'react-router-dom';
import { api, errorMessage } from '../lib/api';
import { useAuth } from '../lib/auth';
import type { Inquiry, InquiryStatus } from '../lib/types';
import { StatusBadge } from '../components/StatusBadge';

/**
 * Single inquiry view, accessible to both participants. The status PATCH is
 * available to the side whose turn it logically is — photographers move the
 * inquiry through NEW → READ → RESPONDED, customers can CLOSE it.
 *
 * Authorization is enforced server-side (the service checks the caller is the
 * customer or photographer on the inquiry); the UI just hides options that
 * make no sense for the viewer's role.
 */
export function InquiryDetailPage() {
  const { id } = useParams<{ id: string }>();
  const { user } = useAuth();
  const qc = useQueryClient();
  const [error, setError] = useState<string | null>(null);

  const { data, isLoading, error: loadError } = useQuery({
    queryKey: ['inquiry', id],
    queryFn: async () => (await api.get<Inquiry>(`/inquiries/${id}`)).data,
    enabled: Boolean(id),
  });

  const statusMutation = useMutation({
    mutationFn: async (next: InquiryStatus) => {
      const res = await api.patch<Inquiry>(`/inquiries/${id}/status`, { status: next });
      return res.data;
    },
    onSuccess: updated => {
      qc.setQueryData(['inquiry', id], updated);
      qc.invalidateQueries({ queryKey: ['my-inquiries'] });
      qc.invalidateQueries({ queryKey: ['received-inquiries'] });
    },
    onError: e => setError(errorMessage(e)),
  });

  if (isLoading) return <p className="mx-auto max-w-3xl px-4 py-8 text-gray-500">Loading…</p>;
  if (loadError || !data) {
    return (
      <div className="mx-auto max-w-3xl px-4 py-8">
        <p className="rounded-md bg-red-50 px-3 py-2 text-sm text-red-700">
          {errorMessage(loadError) || 'Inquiry not found.'}
        </p>
      </div>
    );
  }

  const isPhotographer = user?.userId === data.photographerUserId;
  const isCustomer     = user?.userId === data.customerId;
  const back = isCustomer ? '/me/inquiries' : '/me/inbox';

  return (
    <div className="mx-auto max-w-3xl px-4 py-8">
      <Link to={back} className="mb-4 inline-block text-sm text-indigo-600 hover:underline">
        ← Back to {isCustomer ? 'my inquiries' : 'inbox'}
      </Link>

      <div className="rounded-lg border border-gray-200 bg-white p-6 shadow-sm">
        <div className="flex items-start justify-between gap-3">
          <div>
            <h1 className="text-2xl font-semibold text-gray-900">{data.eventType}</h1>
            <p className="text-sm text-gray-500">
              Sent {new Date(data.createdAt).toLocaleString()}
            </p>
          </div>
          <StatusBadge status={data.status} />
        </div>

        <dl className="mt-6 grid grid-cols-1 gap-y-3 text-sm sm:grid-cols-2">
          <DetailRow label="Event date" value={data.eventDate} />
          {data.location && <DetailRow label="Location" value={data.location} />}
          {data.budget != null && <DetailRow label="Budget" value={`$${data.budget.toFixed(2)}`} />}
          <DetailRow
            label={isPhotographer ? 'From customer' : 'To photographer'}
            value={
              isPhotographer ? (
                <span className="font-mono text-xs">{data.customerId}</span>
              ) : (
                <Link
                  to={`/photographers/${data.photographerProfileId}`}
                  className="text-indigo-600 hover:underline"
                >
                  View profile →
                </Link>
              )
            }
          />
        </dl>

        <div className="mt-6">
          <h2 className="mb-2 text-sm font-semibold uppercase tracking-wide text-gray-500">
            Message
          </h2>
          <p className="whitespace-pre-wrap rounded-md bg-gray-50 px-4 py-3 text-gray-800">
            {data.message}
          </p>
        </div>

        {/* Status actions — server enforces the participant check; UI hides what's irrelevant */}
        {data.status !== 'CLOSED' && (isPhotographer || isCustomer) && (
          <div className="mt-6 border-t border-gray-100 pt-6">
            <h2 className="mb-3 text-sm font-semibold uppercase tracking-wide text-gray-500">
              Update status
            </h2>
            <div className="flex flex-wrap gap-2">
              {availableTransitions(data.status, isPhotographer, isCustomer).map(next => (
                <button
                  key={next}
                  onClick={() => {
                    setError(null);
                    statusMutation.mutate(next);
                  }}
                  disabled={statusMutation.isPending}
                  className="rounded-md border border-gray-300 px-3 py-1.5 text-sm font-medium text-gray-700 hover:border-indigo-400 hover:bg-indigo-50 hover:text-indigo-700 disabled:opacity-60"
                >
                  Mark as {next}
                </button>
              ))}
            </div>
            {error && (
              <p className="mt-3 rounded-md bg-red-50 px-3 py-2 text-sm text-red-700">{error}</p>
            )}
          </div>
        )}
      </div>
    </div>
  );
}

/**
 * Which status buttons should we offer to the current viewer?
 *
 *   Photographer: NEW → READ, READ → RESPONDED, anything → CLOSED
 *   Customer:     anything → CLOSED
 *
 * The backend's InquiryService allows any participant to set any status, so
 * the UI is the only place this guidance lives.
 */
function availableTransitions(
  current: InquiryStatus,
  isPhotographer: boolean,
  isCustomer: boolean,
): InquiryStatus[] {
  const out: InquiryStatus[] = [];
  if (isPhotographer) {
    if (current === 'NEW')  out.push('READ');
    if (current === 'READ') out.push('RESPONDED');
  }
  if ((isPhotographer || isCustomer) && current !== 'CLOSED') {
    out.push('CLOSED');
  }
  return out;
}

function DetailRow({ label, value }: { label: string; value: React.ReactNode }) {
  return (
    <div>
      <dt className="text-xs font-medium uppercase tracking-wide text-gray-500">{label}</dt>
      <dd className="mt-0.5 text-gray-900">{value}</dd>
    </div>
  );
}

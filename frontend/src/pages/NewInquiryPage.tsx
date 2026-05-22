import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { api, errorMessage } from '../lib/api';
import type {
  AvailabilitySlot,
  CreateInquiryRequest,
  Inquiry,
  PhotographerProfile,
} from '../lib/types';

/**
 * Customer-only page that creates an inquiry against a specific photographer.
 *
 * The photographer ID arrives as a query param (`?photographerId=…`) — the
 * page fetches the photographer for display and forwards the same UUID in
 * the body of the inquiry POST.
 *
 * On the backend this single POST triggers the cross-service chain:
 *   customer-service → Feign → photographer-service
 * which resolves the photographer's userId and stores it on the inquiry row.
 */
const schema = z.object({
  eventDate: z
    .string()
    .min(1, 'Event date is required')
    .refine(
      v => new Date(v).getTime() >= new Date(new Date().toDateString()).getTime(),
      'Event date cannot be in the past',
    ),
  eventType: z.string().min(1, 'Event type is required').max(50),
  location:  z.string().max(200).optional(),
  // Budget: plain number (or undefined when blank). RHF's setValueAs on the
  // register call handles the empty-string → undefined coercion at the input.
  budget:    z.number().min(0).optional(),
  message:   z.string().min(1, 'Please write a message').max(5000),
});

type InquiryValues = z.infer<typeof schema>;

export function NewInquiryPage() {
  const [params] = useSearchParams();
  const navigate = useNavigate();
  const qc = useQueryClient();
  const photographerId = params.get('photographerId');
  const [serverError, setServerError] = useState<string | null>(null);

  // Show a preview of who the inquiry is going to.
  const { data: photographer, isLoading } = useQuery({
    queryKey: ['photographer', photographerId],
    queryFn: async () =>
      (await api.get<PhotographerProfile>(`/photographers/${photographerId}`)).data,
    enabled: Boolean(photographerId),
  });

  // Photographer's posted availability — empty list = "open to any date."
  // We hit the same public endpoint the backend uses for its pre-inquiry check,
  // so the date picker and the server stay in sync.
  const { data: availability } = useQuery({
    queryKey: ['availability', photographerId],
    queryFn: async () =>
      (
        await api.get<AvailabilitySlot[]>(
          `/photographers/${photographerId}/availability`,
        )
      ).data,
    enabled: Boolean(photographerId),
  });

  const availableDates = availability?.map(s => s.availableDate) ?? [];
  const hasPostedCalendar = availableDates.length > 0;

  const form = useForm<InquiryValues>({
    resolver: zodResolver(schema),
    defaultValues: {
      eventDate: '',
      eventType: '',
      location: '',
      budget: undefined,
      message: '',
    },
  });

  const mutation = useMutation({
    mutationFn: async (values: InquiryValues) => {
      const body: CreateInquiryRequest = {
        photographerProfileId: photographerId!,
        eventDate: values.eventDate,
        eventType: values.eventType,
        location: values.location || undefined,
        budget: values.budget && values.budget > 0 ? values.budget : undefined,
        message: values.message,
      };
      const res = await api.post<Inquiry>('/inquiries', body);
      return res.data;
    },
    onSuccess: created => {
      // Invalidate the outbox so it shows the new inquiry next time it's viewed.
      qc.invalidateQueries({ queryKey: ['my-inquiries'] });
      navigate(`/inquiries/${created.id}`, { replace: true });
    },
  });

  if (!photographerId) {
    return (
      <div className="mx-auto max-w-2xl px-4 py-8">
        <ErrorBanner message="No photographer selected." />
        <Link to="/" className="mt-4 inline-block text-sm text-indigo-600 hover:underline">
          ← Browse photographers
        </Link>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-2xl px-4 py-8">
      <Link
        to={`/photographers/${photographerId}`}
        className="mb-4 inline-block text-sm text-indigo-600 hover:underline"
      >
        ← Back to profile
      </Link>

      <h1 className="text-2xl font-semibold text-gray-900">Send an inquiry</h1>
      {isLoading && <p className="mt-2 text-gray-500">Loading photographer…</p>}
      {photographer && (
        <p className="mt-1 text-gray-600">
          To <span className="font-medium text-gray-900">{photographer.displayName}</span> —{' '}
          {photographer.location} · ${photographer.pricePerHour.toFixed(2)}/hr
        </p>
      )}

      <form
        onSubmit={form.handleSubmit(values => {
          setServerError(null);
          mutation.mutate(values, { onError: e => setServerError(errorMessage(e)) });
        })}
        className="mt-6 space-y-4 rounded-lg border border-gray-200 bg-white p-6 shadow-sm"
      >
        <div className="grid gap-4 sm:grid-cols-2">
          <Field
            label="Event date"
            error={form.formState.errors.eventDate?.message}
            hint={
              hasPostedCalendar
                ? `${availableDates.length} dates available — pick one below.`
                : "This photographer hasn't posted specific dates — any date works."
            }
          >
            {hasPostedCalendar ? (
              <select
                {...form.register('eventDate')}
                className="block w-full rounded-md border border-gray-300 bg-white px-3 py-2 focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
              >
                <option value="">Select a date…</option>
                {availableDates.map(d => (
                  <option key={d} value={d}>
                    {new Date(`${d}T00:00:00`).toLocaleDateString(undefined, {
                      weekday: 'short',
                      month: 'short',
                      day: 'numeric',
                      year: 'numeric',
                    })}
                  </option>
                ))}
              </select>
            ) : (
              <Input type="date" {...form.register('eventDate')} />
            )}
          </Field>
          <Field label="Event type" error={form.formState.errors.eventType?.message}>
            <Input placeholder="Wedding, Portrait, Corporate…" {...form.register('eventType')} />
          </Field>
        </div>

        <Field label="Location" error={form.formState.errors.location?.message} hint="Where the shoot will happen.">
          <Input {...form.register('location')} />
        </Field>

        <Field label="Budget (USD)" error={form.formState.errors.budget?.message} hint="Optional.">
          <Input
            type="number"
            step="0.01"
            min={0}
            {...form.register('budget', {
              // Convert "" → undefined and digits → number so zod sees number|undefined
              setValueAs: v => (v === '' || v == null ? undefined : Number(v)),
            })}
          />
        </Field>

        <Field label="Message" error={form.formState.errors.message?.message}>
          <textarea
            rows={6}
            {...form.register('message')}
            placeholder="Tell the photographer about your event, the vibe you're going for, etc."
            className="block w-full rounded-md border border-gray-300 px-3 py-2 focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
          />
        </Field>

        {serverError && <ErrorBanner message={serverError} />}

        <button
          type="submit"
          disabled={mutation.isPending}
          className="w-full rounded-md bg-indigo-600 px-4 py-2 font-medium text-white hover:bg-indigo-700 disabled:opacity-60"
        >
          {mutation.isPending ? 'Sending…' : 'Send inquiry'}
        </button>
      </form>
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
  return <p className="rounded-md bg-red-50 px-3 py-2 text-sm text-red-700">{message}</p>;
}

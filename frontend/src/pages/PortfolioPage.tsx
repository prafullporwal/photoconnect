import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { api, errorMessage } from '../lib/api';
import { MediaLightbox } from '../components/MediaLightbox';
import type { MediaKind, PhotographerProfile, PortfolioItem } from '../lib/types';

/**
 * Photographer's own portfolio manager.
 *
 * Two halves on the page:
 *   1. Upload form — pick a file, choose media kind + category, submit.
 *      Categories are suggested from the photographer's declared specialties.
 *   2. Grid of existing items — grouped by category, with delete buttons.
 *
 * Multipart upload goes through axios with a FormData body. The browser sets
 * Content-Type automatically (including the boundary), so we deliberately do
 * NOT set it ourselves.
 */
export function PortfolioPage() {
  const qc = useQueryClient();
  // Lightbox opens when the photographer clicks a thumbnail — videos play in
  // their natural aspect ratio there instead of being cropped to the grid tile.
  const [lightboxItem, setLightboxItem] = useState<PortfolioItem | null>(null);

  // Profile is needed for both the suggested-category list and the empty state.
  const { data: profile } = useQuery({
    queryKey: ['my-photographer-profile'],
    queryFn: async () => (await api.get<PhotographerProfile>('/photographers/me')).data,
  });

  const { data: items, isLoading, error } = useQuery({
    queryKey: ['my-portfolio'],
    queryFn: async () => (await api.get<PortfolioItem[]>('/photographers/me/portfolio')).data,
  });

  const deleteMutation = useMutation({
    mutationFn: async (id: string) => {
      await api.delete(`/photographers/me/portfolio/${id}`);
    },
    onSuccess: () => qc.invalidateQueries({ queryKey: ['my-portfolio'] }),
  });

  if (!profile) {
    return (
      <div className="mx-auto max-w-3xl px-4 py-8">
        <p className="rounded-md bg-amber-50 px-4 py-3 text-sm text-amber-800">
          You need a photographer profile before you can upload portfolio items.{' '}
          <Link to="/me/profile" className="font-medium underline">
            Create one first
          </Link>
          .
        </p>
      </div>
    );
  }

  // Group by category for the display grid.
  const byCategory = new Map<string, PortfolioItem[]>();
  (items ?? []).forEach(item => {
    const list = byCategory.get(item.category) ?? [];
    list.push(item);
    byCategory.set(item.category, list);
  });

  return (
    <div className="mx-auto max-w-5xl px-4 py-8">
      <div className="mb-6 flex items-baseline justify-between">
        <h1 className="text-2xl font-semibold text-gray-900">My portfolio</h1>
        {items && <span className="text-sm text-gray-500">{items.length} items</span>}
      </div>

      <UploadForm suggestedCategories={profile.specialties} />

      {isLoading && <p className="mt-8 text-gray-500">Loading…</p>}
      {error && (
        <p className="mt-8 rounded-md bg-red-50 px-3 py-2 text-sm text-red-700">
          {errorMessage(error)}
        </p>
      )}

      {items && items.length === 0 && (
        <div className="mt-8 rounded-lg border border-dashed border-gray-300 bg-white p-12 text-center text-gray-500">
          You haven't uploaded anything yet. Add your first sample above.
        </div>
      )}

      {byCategory.size > 0 && (
        <div className="mt-8 space-y-8">
          {Array.from(byCategory.entries()).map(([category, group]) => (
            <section key={category}>
              <h2 className="mb-3 text-sm font-semibold uppercase tracking-wide text-gray-500">
                {category}
              </h2>
              <ul className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
                {group.map(item => (
                  <li
                    key={item.id}
                    className="overflow-hidden rounded-lg border border-gray-200 bg-white shadow-sm"
                  >
                    <Media item={item} onOpen={() => setLightboxItem(item)} />
                    <div className="flex items-center justify-between border-t border-gray-100 px-3 py-2 text-xs text-gray-500">
                      <span className="font-medium text-gray-700">{item.mediaType}</span>
                      <button
                        onClick={() => deleteMutation.mutate(item.id)}
                        disabled={deleteMutation.isPending}
                        className="text-red-600 hover:underline disabled:opacity-60"
                      >
                        Delete
                      </button>
                    </div>
                  </li>
                ))}
              </ul>
            </section>
          ))}
        </div>
      )}

      {lightboxItem && (
        <MediaLightbox item={lightboxItem} onClose={() => setLightboxItem(null)} />
      )}
    </div>
  );
}

// ─────────────────────────────────────────────────────────────────────────────
// Upload form
// ─────────────────────────────────────────────────────────────────────────────

interface UploadValues {
  file: FileList;
  mediaType: MediaKind;
  category: string;
}

function UploadForm({ suggestedCategories }: { suggestedCategories: string[] }) {
  const qc = useQueryClient();
  const form = useForm<UploadValues>({
    defaultValues: { mediaType: 'IMAGE', category: suggestedCategories[0] ?? '' },
  });
  const [serverError, setServerError] = useState<string | null>(null);

  const uploadMutation = useMutation({
    mutationFn: async (values: UploadValues) => {
      const file = values.file?.[0];
      if (!file) throw new Error('Please choose a file');

      // FormData drives multipart. Crucially, we do NOT set Content-Type:
      // axios + the browser pick up the right boundary automatically.
      const body = new FormData();
      body.append('file', file);
      body.append('mediaType', values.mediaType);
      body.append('category', values.category.trim());

      const res = await api.post<PortfolioItem>('/photographers/me/portfolio', body);
      return res.data;
    },
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['my-portfolio'] });
      form.reset({ mediaType: 'IMAGE', category: suggestedCategories[0] ?? '' });
    },
  });

  return (
    <form
      onSubmit={form.handleSubmit(values => {
        setServerError(null);
        uploadMutation.mutate(values, { onError: e => setServerError(errorMessage(e)) });
      })}
      className="space-y-3 rounded-lg border border-gray-200 bg-white p-5 shadow-sm"
    >
      <h2 className="text-sm font-semibold uppercase tracking-wide text-gray-500">
        Add new sample
      </h2>

      <input
        type="file"
        accept="image/*,video/*"
        {...form.register('file', { required: true })}
        className="block w-full text-sm text-gray-700 file:mr-3 file:rounded-md file:border-0 file:bg-indigo-50 file:px-3 file:py-2 file:text-sm file:font-medium file:text-indigo-700 hover:file:bg-indigo-100"
      />

      <div className="grid gap-3 sm:grid-cols-2">
        <label className="block">
          <span className="mb-1 block text-sm font-medium text-gray-700">Media type</span>
          <select
            {...form.register('mediaType')}
            className="block w-full rounded-md border border-gray-300 px-3 py-2 focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
          >
            <option value="IMAGE">Image</option>
            <option value="VIDEO">Video</option>
            <option value="REEL">Reel (short vertical video)</option>
          </select>
        </label>

        <label className="block">
          <span className="mb-1 block text-sm font-medium text-gray-700">Category</span>
          <input
            list="suggested-categories"
            {...form.register('category', { required: true })}
            placeholder="wedding, portrait, …"
            className="block w-full rounded-md border border-gray-300 px-3 py-2 focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
          />
          <datalist id="suggested-categories">
            {suggestedCategories.map(c => (
              <option key={c} value={c} />
            ))}
          </datalist>
        </label>
      </div>

      {serverError && (
        <p className="rounded-md bg-red-50 px-3 py-2 text-sm text-red-700">{serverError}</p>
      )}

      <button
        type="submit"
        disabled={uploadMutation.isPending}
        className="rounded-md bg-indigo-600 px-4 py-2 text-sm font-medium text-white hover:bg-indigo-700 disabled:opacity-60"
      >
        {uploadMutation.isPending ? 'Uploading…' : 'Upload'}
      </button>
    </form>
  );
}

/**
 * Grid thumbnail.
 *
 * <p>Crops with {@code object-cover} so every tile shares the same aspect
 * ratio (4:3 for IMAGE/VIDEO, 9:16 for REEL). Clicking opens
 * {@link MediaLightbox} which plays the asset at its natural aspect ratio,
 * with real video controls — that's the fix for "videos play cropped".</p>
 */
function Media({
  item,
  onOpen,
}: {
  item: PortfolioItem;
  onOpen: () => void;
}) {
  const isVideo = item.mimeType.startsWith('video/');
  const wrapperClass =
    item.mediaType === 'REEL' ? 'aspect-[9/16]' : 'aspect-[4/3]';

  return (
    <button
      type="button"
      onClick={onOpen}
      className={`group relative block w-full bg-gray-100 ${wrapperClass}`}
      aria-label={`Open ${item.mediaType.toLowerCase()} preview`}
    >
      {isVideo ? (
        <video
          src={item.publicUrl}
          muted
          playsInline
          preload="metadata"
          className="h-full w-full object-cover"
        />
      ) : (
        <img
          src={item.publicUrl}
          alt={`${item.category} sample`}
          loading="lazy"
          className="h-full w-full object-cover"
        />
      )}

      {/* Play overlay on videos — hints clicking opens the full-aspect player */}
      {isVideo && (
        <span className="pointer-events-none absolute inset-0 flex items-center justify-center bg-black/0 transition group-hover:bg-black/30">
          <span className="grid h-12 w-12 place-items-center rounded-full bg-black/55 text-white opacity-90 transition group-hover:opacity-100">
            <svg
              xmlns="http://www.w3.org/2000/svg"
              viewBox="0 0 24 24"
              fill="currentColor"
              aria-hidden="true"
              className="h-6 w-6 translate-x-0.5"
            >
              <path d="M8 5v14l11-7z" />
            </svg>
          </span>
        </span>
      )}
    </button>
  );
}

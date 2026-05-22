import { useEffect, useMemo, useRef, useState } from 'react';
import { useForm } from 'react-hook-form';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Link, useNavigate } from 'react-router-dom';
import { api, errorMessage } from '../lib/api';
import { MediaLightbox } from '../components/MediaLightbox';
import type { MediaKind, PhotographerProfile, PortfolioItem } from '../lib/types';

interface StagedItem {
  id: string;
  file: File;
  mediaType: MediaKind;
  category: string;
}

type UploadPhase =
  | { kind: 'uploading'; current: number; total: number; fileName: string }
  | { kind: 'done'; total: number }
  | { kind: 'error'; message: string; uploadedCount: number };

export function PortfolioPage() {
  const qc = useQueryClient();
  const navigate = useNavigate();
  const [lightboxItem, setLightboxItem] = useState<PortfolioItem | null>(null);
  const [stagedItems, setStagedItems] = useState<StagedItem[]>([]);
  const [uploadPhase, setUploadPhase] = useState<UploadPhase | null>(null);
  const [activeKind, setActiveKind] = useState<MediaKind | 'ALL'>('ALL');
  const [query, setQuery] = useState('');

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

  async function handleConfirmUpload() {
    const toUpload = [...stagedItems];
    let uploadedCount = 0;

    for (let i = 0; i < toUpload.length; i++) {
      const staged = toUpload[i];
      setUploadPhase({
        kind: 'uploading',
        current: i + 1,
        total: toUpload.length,
        fileName: staged.file.name,
      });

      try {
        const body = new FormData();
        body.append('file', staged.file);
        body.append('mediaType', staged.mediaType);
        body.append('category', staged.category.trim());
        await api.post<PortfolioItem>('/photographers/me/portfolio', body);
        uploadedCount++;
      } catch (e) {
        // Remove already-uploaded items from the queue so the user can retry the rest
        if (uploadedCount > 0) {
          setStagedItems(prev => prev.slice(uploadedCount));
          qc.invalidateQueries({ queryKey: ['my-portfolio'] });
        }
        setUploadPhase({ kind: 'error', message: errorMessage(e), uploadedCount });
        return;
      }
    }

    setStagedItems([]);
    qc.invalidateQueries({ queryKey: ['my-portfolio'] });
    setUploadPhase({ kind: 'done', total: toUpload.length });
  }

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

  const filteredItems = useMemo(() => {
    if (!items) return [];
    const q = query.trim().toLowerCase();
    return items.filter(item => {
      if (activeKind !== 'ALL' && item.mediaType !== activeKind) return false;
      if (q && !item.category.toLowerCase().includes(q)) return false;
      return true;
    });
  }, [items, activeKind, query]);

  const byCategory = new Map<string, PortfolioItem[]>();
  filteredItems.forEach(item => {
    const list = byCategory.get(item.category) ?? [];
    list.push(item);
    byCategory.set(item.category, list);
  });

  return (
    <div className="mx-auto max-w-5xl px-4 py-8">
      <div className="mb-4 flex items-baseline justify-between">
        <h1 className="text-2xl font-semibold text-gray-900">My portfolio</h1>
        {items && <span className="text-sm text-gray-500">{items.length} items</span>}
      </div>

      {/* ── Sticky filter bar ─────────────────────────────────────────────── */}
      {items && items.length > 0 && (
        <div className="sticky top-14 z-20 -mx-4 mb-6 border-b border-slate-200/70 bg-white/70 px-4 py-3 backdrop-blur-lg supports-[backdrop-filter]:bg-white/55">
          <PortfolioSearch value={query} onChange={setQuery} />
          <div className="mt-3 flex flex-wrap items-center gap-2 text-sm">
            <FilterChip active={activeKind === 'ALL'}   onClick={() => setActiveKind('ALL')}   label="All" />
            <FilterChip active={activeKind === 'IMAGE'} onClick={() => setActiveKind('IMAGE')} label="Photos" />
            <FilterChip active={activeKind === 'VIDEO'} onClick={() => setActiveKind('VIDEO')} label="Videos" />
            <FilterChip active={activeKind === 'REEL'}  onClick={() => setActiveKind('REEL')}  label="Reels" />
            <span className="ml-auto text-xs font-medium text-slate-500">
              {filteredItems.length} {filteredItems.length === 1 ? 'item' : 'items'}
              {query.trim() && (
                <> for <span className="text-slate-900">"{query.trim()}"</span></>
              )}
            </span>
          </div>
        </div>
      )}

      <AddToQueueForm
        suggestedCategories={profile.specialties}
        onAdd={item => setStagedItems(prev => [...prev, item])}
      />

      {stagedItems.length > 0 && (
        <StagingArea
          items={stagedItems}
          onRemove={id => setStagedItems(prev => prev.filter(i => i.id !== id))}
          onConfirm={handleConfirmUpload}
        />
      )}

      {isLoading && <p className="mt-8 text-gray-500">Loading…</p>}
      {error && (
        <p className="mt-8 rounded-md bg-red-50 px-3 py-2 text-sm text-red-700">
          {errorMessage(error)}
        </p>
      )}

      {items && items.length === 0 && stagedItems.length === 0 && (
        <div className="mt-8 rounded-lg border border-dashed border-gray-300 bg-white p-12 text-center text-gray-500">
          You haven't uploaded anything yet. Add your first sample above.
        </div>
      )}

      {items && items.length > 0 && filteredItems.length === 0 && (
        <div className="mt-4 rounded-xl border border-slate-200 bg-white px-4 py-3 text-sm text-slate-600 shadow-sm">
          {query.trim() ? (
            <>
              No matches for <span className="font-semibold text-slate-900">"{query.trim()}"</span>
              {activeKind !== 'ALL' && <> in {activeKind.toLowerCase()}s</>}.{' '}
              <button
                onClick={() => { setQuery(''); setActiveKind('ALL'); }}
                className="font-semibold text-indigo-600 hover:underline"
              >
                Clear filters
              </button>
            </>
          ) : (
            <>No {activeKind.toLowerCase()} items yet. Try a different filter.</>
          )}
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

      {uploadPhase && (
        <UploadModal
          phase={uploadPhase}
          onClose={() => setUploadPhase(null)}
          onNavigateHome={() => navigate('/')}
        />
      )}
    </div>
  );
}

// ─────────────────────────────────────────────────────────────────────────────
// Add-to-queue form — collects file + metadata, does NOT call the API
// ─────────────────────────────────────────────────────────────────────────────

interface QueueValues {
  file: FileList;
  mediaType: MediaKind;
  category: string;
}

function AddToQueueForm({
  suggestedCategories,
  onAdd,
}: {
  suggestedCategories: string[];
  onAdd: (item: StagedItem) => void;
}) {
  const form = useForm<QueueValues>({
    defaultValues: { mediaType: 'IMAGE', category: suggestedCategories[0] ?? '' },
  });

  const fileInputRef = useRef<HTMLInputElement | null>(null);
  const selectedMediaType = form.watch('mediaType');
  // Narrow the file-picker to only the expected type so the OS dialog shows relevant files.
  const accept = selectedMediaType === 'IMAGE' ? 'image/*' : 'video/*';

  // When the user switches media type, re-validate the already-chosen file immediately.
  useEffect(() => {
    if (form.getValues('file')?.[0]) {
      form.trigger('file');
    }
  }, [selectedMediaType]); // eslint-disable-line react-hooks/exhaustive-deps

  function handleAdd(values: QueueValues) {
    const file = values.file?.[0];
    if (!file) return;
    onAdd({
      id: `${Date.now()}-${Math.random()}`,
      file,
      mediaType: values.mediaType,
      category: values.category.trim(),
    });
    form.reset({ mediaType: 'IMAGE', category: suggestedCategories[0] ?? '' });
    if (fileInputRef.current) fileInputRef.current.value = '';
  }

  return (
    <form
      onSubmit={form.handleSubmit(handleAdd)}
      className="space-y-3 rounded-lg border border-gray-200 bg-white p-5 shadow-sm"
    >
      <h2 className="text-sm font-semibold uppercase tracking-wide text-gray-500">
        Add new sample
      </h2>

      <div>
        <input
          type="file"
          accept={accept}
          {...(() => {
            const { ref, ...rest } = form.register('file', {
              required: 'Please choose a file.',
              validate: fileList => {
                const file = fileList?.[0];
                if (!file) return true;
                const kind = form.getValues('mediaType');
                if (kind === 'IMAGE' && !file.type.startsWith('image/')) {
                  return `"${file.name}" is a video file. Please select an image, or change the type to Video / Reel.`;
                }
                if ((kind === 'VIDEO' || kind === 'REEL') && !file.type.startsWith('video/')) {
                  return `"${file.name}" is an image file. Please select a video, or change the type to Image.`;
                }
                return true;
              },
            });
            return {
              ...rest,
              ref: (el: HTMLInputElement | null) => {
                ref(el);
                fileInputRef.current = el;
              },
            };
          })()}
          className="block w-full text-sm text-gray-700 file:mr-3 file:rounded-md file:border-0 file:bg-indigo-50 file:px-3 file:py-2 file:text-sm file:font-medium file:text-indigo-700 hover:file:bg-indigo-100"
        />
        {form.formState.errors.file && (
          <p className="mt-1.5 rounded-md bg-red-50 px-3 py-2 text-sm text-red-600">
            {form.formState.errors.file.message}
          </p>
        )}
      </div>

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

      <button
        type="submit"
        className="rounded-md bg-indigo-600 px-4 py-2 text-sm font-medium text-white hover:bg-indigo-700"
      >
        + Add to queue
      </button>
    </form>
  );
}

// ─────────────────────────────────────────────────────────────────────────────
// Staging area — review queued items, remove if needed, then confirm
// ─────────────────────────────────────────────────────────────────────────────

function StagingArea({
  items,
  onRemove,
  onConfirm,
}: {
  items: StagedItem[];
  onRemove: (id: string) => void;
  onConfirm: () => void;
}) {
  return (
    <div className="mt-4 rounded-lg border border-indigo-200 bg-indigo-50 p-5">
      <h2 className="mb-3 text-sm font-semibold text-indigo-900">
        Ready to upload &mdash; {items.length} {items.length === 1 ? 'item' : 'items'}
      </h2>

      <ul className="mb-4 divide-y divide-indigo-100 rounded-md border border-indigo-100 bg-white">
        {items.map(item => (
          <li key={item.id} className="flex items-center justify-between px-4 py-3">
            <div className="min-w-0 flex-1">
              <p className="truncate text-sm font-medium text-gray-800">{item.file.name}</p>
              <p className="text-xs text-gray-500">
                {item.mediaType} &middot; {item.category}
              </p>
            </div>
            <button
              type="button"
              onClick={() => onRemove(item.id)}
              className="ml-4 shrink-0 text-xs text-red-600 hover:underline"
            >
              Remove
            </button>
          </li>
        ))}
      </ul>

      <button
        type="button"
        onClick={onConfirm}
        className="w-full rounded-md bg-indigo-600 py-2.5 text-sm font-semibold text-white hover:bg-indigo-700 active:bg-indigo-800"
      >
        Confirm &amp; Upload {items.length} {items.length === 1 ? 'item' : 'items'}
      </button>
    </div>
  );
}

// ─────────────────────────────────────────────────────────────────────────────
// Upload progress modal
// ─────────────────────────────────────────────────────────────────────────────

function UploadModal({
  phase,
  onClose,
  onNavigateHome,
}: {
  phase: UploadPhase;
  onClose: () => void;
  onNavigateHome: () => void;
}) {
  return (
    <div
      role="dialog"
      aria-modal="true"
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm"
    >
      <div className="w-full max-w-sm rounded-2xl bg-white p-8 text-center shadow-2xl">
        {phase.kind === 'uploading' && (
          <>
            <div className="mx-auto h-14 w-14 animate-spin rounded-full border-4 border-indigo-200 border-t-indigo-600" />
            <p className="mt-5 text-lg font-semibold text-gray-800">Uploading…</p>
            <p className="mt-1 text-sm font-medium text-indigo-600">
              Item {phase.current} of {phase.total}
            </p>
            <p className="mt-1 max-w-full truncate text-sm text-gray-400">{phase.fileName}</p>
            <div className="mt-4 h-2 w-full overflow-hidden rounded-full bg-gray-200">
              <div
                className="h-full rounded-full bg-indigo-500 transition-all duration-300"
                style={{ width: `${(phase.current / phase.total) * 100}%` }}
              />
            </div>
            <p className="mt-2 text-xs text-gray-400">
              Please don't close this tab while uploading.
            </p>
          </>
        )}

        {phase.kind === 'done' && (
          <>
            <div className="mx-auto flex h-14 w-14 items-center justify-center rounded-full bg-green-100">
              <svg
                xmlns="http://www.w3.org/2000/svg"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                strokeWidth={2.5}
                className="h-8 w-8 text-green-600"
              >
                <path strokeLinecap="round" strokeLinejoin="round" d="M5 13l4 4L19 7" />
              </svg>
            </div>
            <p className="mt-5 text-lg font-semibold text-gray-800">All done!</p>
            <p className="mt-1 text-sm text-gray-500">
              {phase.total} {phase.total === 1 ? 'item' : 'items'} uploaded successfully.
            </p>
            <button
              onClick={onNavigateHome}
              className="mt-6 w-full rounded-md bg-indigo-600 py-2.5 text-sm font-semibold text-white hover:bg-indigo-700"
            >
              Go to Home
            </button>
          </>
        )}

        {phase.kind === 'error' && (
          <>
            <div className="mx-auto flex h-14 w-14 items-center justify-center rounded-full bg-red-100">
              <svg
                xmlns="http://www.w3.org/2000/svg"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                strokeWidth={2.5}
                className="h-8 w-8 text-red-600"
              >
                <path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12" />
              </svg>
            </div>
            <p className="mt-5 text-lg font-semibold text-red-700">Upload failed</p>
            <p className="mt-2 text-sm text-gray-600">{phase.message}</p>
            {phase.uploadedCount > 0 && (
              <p className="mt-1 text-xs text-gray-400">
                {phase.uploadedCount}{' '}
                {phase.uploadedCount === 1 ? 'item was' : 'items were'} uploaded before the
                error.
              </p>
            )}
            <button
              onClick={onClose}
              className="mt-6 w-full rounded-md border border-gray-300 py-2.5 text-sm font-medium text-gray-700 hover:bg-gray-50"
            >
              Close &amp; Retry
            </button>
          </>
        )}
      </div>
    </div>
  );
}

// ─────────────────────────────────────────────────────────────────────────────
// Filter bar components
// ─────────────────────────────────────────────────────────────────────────────

function FilterChip({
  active,
  onClick,
  label,
}: {
  active: boolean;
  onClick: () => void;
  label: string;
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={`rounded-full px-3.5 py-1.5 text-sm font-medium transition-all duration-200 ${
        active
          ? 'bg-slate-900 text-white shadow-md shadow-slate-900/10'
          : 'bg-white text-slate-600 ring-1 ring-slate-200 hover:bg-slate-50 hover:text-slate-900'
      }`}
    >
      {label}
    </button>
  );
}

function PortfolioSearch({
  value,
  onChange,
}: {
  value: string;
  onChange: (next: string) => void;
}) {
  return (
    <div className="relative">
      <svg
        xmlns="http://www.w3.org/2000/svg"
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        strokeWidth="2"
        strokeLinecap="round"
        strokeLinejoin="round"
        aria-hidden="true"
        className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-gray-400"
      >
        <circle cx="11" cy="11" r="7" />
        <path d="m20 20-3.5-3.5" />
      </svg>
      <input
        type="search"
        value={value}
        onChange={e => onChange(e.target.value)}
        onKeyDown={e => { if (e.key === 'Escape') onChange(''); }}
        placeholder="Filter by category…"
        autoComplete="off"
        className="block w-full rounded-full border border-slate-200 bg-white py-2.5 pl-10 pr-10 text-sm shadow-sm transition placeholder:text-slate-400 focus:border-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-500/20"
      />
      {value && (
        <button
          type="button"
          onClick={() => onChange('')}
          aria-label="Clear search"
          className="absolute right-2 top-1/2 -translate-y-1/2 rounded-md p-1 text-gray-400 hover:bg-gray-100 hover:text-gray-700"
        >
          <svg
            xmlns="http://www.w3.org/2000/svg"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="2"
            strokeLinecap="round"
            strokeLinejoin="round"
            aria-hidden="true"
            className="h-4 w-4"
          >
            <path d="M18 6 6 18M6 6l12 12" />
          </svg>
        </button>
      )}
    </div>
  );
}

// ─────────────────────────────────────────────────────────────────────────────
// Grid thumbnail
// ─────────────────────────────────────────────────────────────────────────────

function Media({
  item,
  onOpen,
}: {
  item: PortfolioItem;
  onOpen: () => void;
}) {
  const isVideo = item.mimeType.startsWith('video/');
  const wrapperClass = item.mediaType === 'REEL' ? 'aspect-[9/16]' : 'aspect-[4/3]';

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

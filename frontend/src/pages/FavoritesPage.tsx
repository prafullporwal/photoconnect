import { useMemo, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { api, errorMessage } from '../lib/api';
import { MediaLightbox } from '../components/MediaLightbox';
import { FavoriteButton } from '../components/FavoriteButton';
import type { Favorite, FeedItem, MediaKind } from '../lib/types';

/**
 * /me/favorites — saved content feed.
 *
 * Looks and behaves exactly like the browse page, but only shows portfolio
 * items the customer has hearted. Same masonry layout, same search + filter
 * chips, same lightbox. The heart on each tile removes it from this feed.
 */
export function FavoritesPage() {
  const [activeKind, setActiveKind] = useState<MediaKind | 'ALL'>('ALL');
  const [query, setQuery] = useState('');
  const [lightboxItem, setLightboxItem] = useState<FeedItem | null>(null);

  const { data, isLoading, error } = useQuery({
    queryKey: ['favorites'],
    queryFn: async () => (await api.get<Favorite[]>('/favorites')).data,
  });

  // Only items where the content still exists (non-tombstone rows).
  const liveItems = useMemo(() => (data ?? []).filter(f => f.item !== null), [data]);
  const tombstones = useMemo(() => (data ?? []).filter(f => f.item === null), [data]);

  const filtered = useMemo(() => {
    const q = query.trim().toLowerCase();
    return liveItems.filter(f => {
      const item = f.item!;
      if (activeKind !== 'ALL' && item.mediaType !== activeKind) return false;
      if (!q) return true;
      return (
        item.photographer.displayName.toLowerCase().includes(q) ||
        item.photographer.location.toLowerCase().includes(q) ||
        item.category.toLowerCase().includes(q)
      );
    });
  }, [liveItems, activeKind, query]);

  return (
    <div className="mx-auto max-w-6xl px-4 pb-12 pt-8">
      <header className="mb-6">
        <h1 className="bg-gradient-to-r from-rose-500 via-pink-500 to-fuchsia-500 bg-clip-text text-4xl font-bold tracking-tight text-transparent sm:text-5xl">
          Saved
        </h1>
        <p className="mt-2 max-w-xl text-base text-slate-600">
          Content you've hearted. Tap any tile to play it full size.
        </p>
      </header>

      {/* ── Sticky controls ───────────────────────────────────────────────── */}
      {data && liveItems.length > 0 && (
        <div className="sticky top-14 z-20 -mx-4 mb-6 border-b border-slate-200/70 bg-white/70 px-4 py-3 backdrop-blur-lg supports-[backdrop-filter]:bg-white/55">
          <SearchInput value={query} onChange={setQuery} />
          <div className="mt-3 flex flex-wrap items-center gap-2 text-sm">
            <FilterChip active={activeKind === 'ALL'}   onClick={() => setActiveKind('ALL')}   label="All" />
            <FilterChip active={activeKind === 'IMAGE'} onClick={() => setActiveKind('IMAGE')} label="Photos" />
            <FilterChip active={activeKind === 'VIDEO'} onClick={() => setActiveKind('VIDEO')} label="Videos" />
            <FilterChip active={activeKind === 'REEL'}  onClick={() => setActiveKind('REEL')}  label="Reels" />
            <span className="ml-auto text-xs font-medium text-slate-500">
              {filtered.length} {filtered.length === 1 ? 'item' : 'items'}
              {query.trim() && (
                <> for <span className="text-slate-900">"{query.trim()}"</span></>
              )}
            </span>
          </div>
        </div>
      )}

      {isLoading && <LoadingSkeleton />}

      {error && (
        <div className="rounded-xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-700">
          Couldn't load your saved content: {errorMessage(error)}
        </div>
      )}

      {/* Empty state — nothing saved yet */}
      {data && data.length === 0 && (
        <div className="rounded-2xl border border-dashed border-slate-300 bg-white/60 p-16 text-center text-slate-500">
          <p className="text-base font-medium text-slate-700">Nothing saved yet</p>
          <p className="mt-1 text-sm">
            <Link to="/" className="font-semibold text-indigo-600 hover:underline">
              Browse the marketplace
            </Link>{' '}
            and tap the heart on any photo, video, or reel you like.
          </p>
        </div>
      )}

      {/* No filter match */}
      {data && liveItems.length > 0 && filtered.length === 0 && (
        <div className="rounded-xl border border-slate-200 bg-white px-4 py-3 text-sm text-slate-600 shadow-sm">
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
            <>No {activeKind.toLowerCase()} items saved. Try a different filter.</>
          )}
        </div>
      )}

      {/* Masonry feed — same layout as browse */}
      {filtered.length > 0 && (
        <ul className="gap-3 columns-1 sm:columns-2 lg:columns-3">
          {filtered.map(fav => (
            <SavedTile
              key={fav.id}
              item={fav.item!}
              portfolioItemId={fav.portfolioItemId}
              onOpen={() => setLightboxItem(fav.item)}
            />
          ))}
        </ul>
      )}

      {/* Tombstones — content deleted after it was saved */}
      {tombstones.length > 0 && (
        <div className="mt-8">
          <p className="mb-3 text-xs font-semibold uppercase tracking-wider text-slate-400">
            Unavailable content ({tombstones.length})
          </p>
          <ul className="space-y-2">
            {tombstones.map(fav => (
              <li
                key={fav.id}
                className="flex items-center justify-between rounded-xl border border-dashed border-slate-300 bg-slate-50 px-4 py-3"
              >
                <p className="text-sm text-slate-500">
                  This content is no longer available.
                </p>
                <FavoriteButton portfolioItemId={fav.portfolioItemId} variant="overlay" />
              </li>
            ))}
          </ul>
        </div>
      )}

      {lightboxItem && (
        <MediaLightbox
          item={lightboxItem}
          onClose={() => setLightboxItem(null)}
          footer={
            <div className="flex items-center justify-between gap-3">
              <div>
                <p className="font-semibold leading-tight text-white">
                  {lightboxItem.photographer.displayName}
                </p>
                <p className="text-xs text-white/70">{lightboxItem.photographer.location}</p>
              </div>
              <Link
                to={`/photographers/${lightboxItem.photographer.id}`}
                onClick={() => setLightboxItem(null)}
                className="rounded-md bg-white px-3 py-1.5 text-sm font-medium text-gray-900 hover:bg-gray-100"
              >
                View profile →
              </Link>
            </div>
          }
        />
      )}
    </div>
  );
}

// ─────────────────────────────────────────────────────────────────────────────
// Saved content tile — same structure as FeedTile in BrowsePage
// ─────────────────────────────────────────────────────────────────────────────

function SavedTile({
  item,
  portfolioItemId,
  onOpen,
}: {
  item: FeedItem;
  portfolioItemId: string;
  onOpen: () => void;
}) {
  const isVideo = item.mimeType.startsWith('video/');
  const aspect = item.mediaType === 'REEL' ? 'aspect-[9/16]' : 'aspect-[4/3]';

  return (
    <li
      className={`group relative mb-4 break-inside-avoid overflow-hidden rounded-2xl bg-slate-100 shadow-sm ring-1 ring-slate-200/80 transition-all duration-300 hover:-translate-y-0.5 hover:shadow-xl hover:ring-rose-300/60 ${aspect}`}
    >
      <button
        type="button"
        onClick={onOpen}
        aria-label={`Open ${item.mediaType.toLowerCase()} by ${item.photographer.displayName}`}
        className="absolute inset-0 block h-full w-full"
      >
        {isVideo ? (
          <video
            src={item.publicUrl}
            muted
            playsInline
            preload="metadata"
            className="h-full w-full object-cover"
            onMouseEnter={e => e.currentTarget.play().catch(() => undefined)}
            onMouseLeave={e => { e.currentTarget.pause(); e.currentTarget.currentTime = 0; }}
          />
        ) : (
          <img
            src={item.publicUrl}
            alt={`${item.category} work by ${item.photographer.displayName}`}
            loading="lazy"
            className="h-full w-full object-cover"
          />
        )}
      </button>

      <span className="pointer-events-none absolute right-2 top-2 rounded-full bg-black/55 px-2 py-0.5 text-[10px] font-semibold uppercase tracking-wider text-white">
        {item.mediaType}
      </span>

      {/* Heart — clicking removes from saved */}
      <div className="absolute left-2 top-2">
        <FavoriteButton portfolioItemId={portfolioItemId} />
      </div>

      <Link
        to={`/photographers/${item.photographer.id}`}
        onClick={e => e.stopPropagation()}
        className="absolute inset-x-0 bottom-0 block bg-gradient-to-t from-black/75 via-black/45 to-transparent p-3 text-left text-white transition hover:from-black/90 hover:via-black/60"
      >
        <p className="text-sm font-semibold leading-tight underline-offset-2 group-hover:underline">
          {item.photographer.displayName}
        </p>
        <p className="mt-0.5 text-xs text-gray-200">
          {item.photographer.location} · {item.category}
        </p>
      </Link>
    </li>
  );
}

// ─────────────────────────────────────────────────────────────────────────────
// Filter bar — identical components to BrowsePage
// ─────────────────────────────────────────────────────────────────────────────

function FilterChip({ active, onClick, label }: { active: boolean; onClick: () => void; label: string }) {
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

function SearchInput({ value, onChange }: { value: string; onChange: (next: string) => void }) {
  return (
    <div className="relative">
      <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true" className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-gray-400">
        <circle cx="11" cy="11" r="7" /><path d="m20 20-3.5-3.5" />
      </svg>
      <input
        type="search"
        value={value}
        onChange={e => onChange(e.target.value)}
        onKeyDown={e => { if (e.key === 'Escape') onChange(''); }}
        placeholder="Search by photographer, location, or category..."
        autoComplete="off"
        className="block w-full rounded-full border border-slate-200 bg-white py-2.5 pl-10 pr-10 text-sm shadow-sm transition placeholder:text-slate-400 focus:border-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-500/20"
      />
      {value && (
        <button type="button" onClick={() => onChange('')} aria-label="Clear search" className="absolute right-2 top-1/2 -translate-y-1/2 rounded-md p-1 text-gray-400 hover:bg-gray-100 hover:text-gray-700">
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true" className="h-4 w-4">
            <path d="M18 6 6 18M6 6l12 12" />
          </svg>
        </button>
      )}
    </div>
  );
}

function LoadingSkeleton() {
  const aspects = ['aspect-[4/3]', 'aspect-[9/16]', 'aspect-[4/3]', 'aspect-[4/3]', 'aspect-[9/16]', 'aspect-[4/3]'];
  return (
    <ul className="gap-3 columns-1 sm:columns-2 lg:columns-3">
      {aspects.map((a, i) => (
        <li key={i} className={`mb-4 break-inside-avoid animate-pulse rounded-2xl bg-slate-200/70 ring-1 ring-slate-200/80 ${a}`} />
      ))}
    </ul>
  );
}

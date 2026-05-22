import { useMemo, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Link, Navigate } from 'react-router-dom';
import { api, errorMessage } from '../lib/api';
import { useAuth } from '../lib/auth';
import { MediaLightbox } from '../components/MediaLightbox';
import { FavoriteButton } from '../components/FavoriteButton';
import type { FeedItem, MediaKind } from '../lib/types';

/**
 * Marketplace browse — Pinterest/Instagram-style explore feed.
 *
 * Instead of showing photographer cards, we surface their <em>work</em>
 * directly. Each tile is a photo, video, or reel; clicking anywhere on the
 * tile takes the customer to the photographer's detail page where they can
 * see the full profile and send an inquiry.
 *
 * Tile sizing is media-type-aware:
 *   - IMAGE / VIDEO → 4:3 landscape
 *   - REEL          → 9:16 vertical (Instagram-style)
 *
 * Photographers are blocked from this page by the gateway + the redirect below;
 * a logged-in photographer who lands here gets bounced to their own profile.
 */
export function BrowsePage() {
  const { user } = useAuth();
  const [activeKind, setActiveKind] = useState<MediaKind | 'ALL'>('ALL');
  const [query, setQuery] = useState('');
  // Clicking a tile no longer navigates — it opens the media in a lightbox at
  // its natural aspect ratio. The "View profile" affordance lives inside the
  // lightbox footer so the customer can still reach the photographer when they
  // like what they see.
  const [lightboxItem, setLightboxItem] = useState<FeedItem | null>(null);

  // Defence in depth: backend gates the endpoint with @PreAuthorize, but
  // redirecting here avoids the gratuitous 403 in the network panel.
  if (user?.role === 'PHOTOGRAPHER') {
    return <Navigate to="/me/profile" replace />;
  }

  const { data, isLoading, error } = useQuery({
    queryKey: ['feed'],
    queryFn: async () => (await api.get<FeedItem[]>('/photographers/feed')).data,
  });

  /**
   * Client-side filtering — the feed returns at most ~50 items per fetch, so
   * a single pass per keystroke is cheap (no debouncing needed). The search
   * matches case-insensitively across THREE fields per tile:
   *   - photographer.displayName
   *   - photographer.location
   *   - category   (e.g. "wedding", "travel")
   *
   * Once we add pagination, this lifts to a server-side `?q=` query param.
   */
  const filtered = useMemo(() => {
    if (!data) return [];
    const q = query.trim().toLowerCase();
    return data.filter(item => {
      if (activeKind !== 'ALL' && item.mediaType !== activeKind) return false;
      if (!q) return true;
      return (
        item.photographer.displayName.toLowerCase().includes(q) ||
        item.photographer.location.toLowerCase().includes(q) ||
        item.category.toLowerCase().includes(q)
      );
    });
  }, [data, activeKind, query]);

  return (
    <div className="mx-auto max-w-6xl px-4 pb-12 pt-8">
      {/* ── Hero ─────────────────────────────────────────────────────────── */}
      <header className="mb-6">
        <h1 className="bg-gradient-to-r from-indigo-600 via-violet-600 to-fuchsia-500 bg-clip-text text-4xl font-bold tracking-tight text-transparent sm:text-5xl">
          Discover
        </h1>
        <p className="mt-2 max-w-xl text-base text-slate-600">
          Real work from working photographers. Tap any tile to play it full
          size — click the name below to view their profile.
        </p>
      </header>

      {/* ── Sticky controls (search + filters) ───────────────────────────── */}
      {/*
        Sits just below the sticky header. As the user scrolls the feed,
        the controls stay accessible. backdrop-blur + slight translucency
        keeps the page underneath legible without a solid bar.
      */}
      <div className="sticky top-14 z-20 -mx-4 mb-6 border-b border-slate-200/70 bg-white/70 px-4 py-3 backdrop-blur-lg supports-[backdrop-filter]:bg-white/55">
        <SearchInput value={query} onChange={setQuery} />

        <div className="mt-3 flex flex-wrap items-center gap-2 text-sm">
          <FilterChip active={activeKind === 'ALL'} onClick={() => setActiveKind('ALL')} label="All" />
          <FilterChip active={activeKind === 'IMAGE'} onClick={() => setActiveKind('IMAGE')} label="Photos" />
          <FilterChip active={activeKind === 'VIDEO'} onClick={() => setActiveKind('VIDEO')} label="Videos" />
          <FilterChip active={activeKind === 'REEL'} onClick={() => setActiveKind('REEL')} label="Reels" />
          {data && (
            <span className="ml-auto text-xs font-medium text-slate-500">
              {filtered.length} {filtered.length === 1 ? 'item' : 'items'}
              {query.trim() && (
                <>
                  {' '}for <span className="text-slate-900">"{query.trim()}"</span>
                </>
              )}
            </span>
          )}
        </div>
      </div>

      {isLoading && <LoadingSkeleton />}

      {error && (
        <div className="rounded-xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-700">
          Couldn't load feed: {errorMessage(error)}
        </div>
      )}

      {data && data.length === 0 && (
        <div className="rounded-2xl border border-dashed border-slate-300 bg-white/60 p-16 text-center text-slate-500">
          <p className="text-base font-medium text-slate-700">No content yet</p>
          <p className="mt-1 text-sm">Photographers haven't uploaded any samples. Check back soon.</p>
        </div>
      )}

      {filtered.length === 0 && data && data.length > 0 && (
        <div className="rounded-xl border border-slate-200 bg-white px-4 py-3 text-sm text-slate-600 shadow-sm">
          {query.trim() ? (
            <>
              No matches for <span className="font-semibold text-slate-900">"{query.trim()}"</span>
              {activeKind !== 'ALL' && <> in {activeKind.toLowerCase()}s</>}.{' '}
              <button
                onClick={() => {
                  setQuery('');
                  setActiveKind('ALL');
                }}
                className="font-semibold text-indigo-600 hover:underline"
              >
                Clear filters
              </button>
            </>
          ) : (
            <>No {activeKind.toLowerCase()} items right now. Try a different filter.</>
          )}
        </div>
      )}

      {filtered.length > 0 && (
        // Pinterest-style masonry via CSS columns. Each tile flows top-to-bottom
        // within its column and packs against its neighbours with no gaps —
        // the right pattern when mixed aspect ratios (4:3 photos and 9:16 reels)
        // would otherwise leave dead space below short tiles in a uniform grid.
        // Reading order is column-first (top of col 1, then top of col 2, …),
        // standard for explore feeds.
        <ul className="gap-3 columns-1 sm:columns-2 lg:columns-3">
          {filtered.map(item => (
            <FeedTile
              key={item.id}
              item={item}
              onOpen={() => setLightboxItem(item)}
            />
          ))}
        </ul>
      )}

      {lightboxItem && (
        <MediaLightbox
          item={lightboxItem}
          onClose={() => setLightboxItem(null)}
          // Photographer info + a clean "View profile" call-to-action. The Link
          // lives inside the lightbox so the customer can leave for the profile
          // page when they decide they like the work.
          footer={
            <div className="flex items-center justify-between gap-3">
              <div>
                <p className="font-semibold leading-tight text-white">
                  {lightboxItem.photographer.displayName}
                </p>
                <p className="text-xs text-white/70">
                  {lightboxItem.photographer.location}
                </p>
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

/**
 * Single feed tile.
 *
 * <h2>Two interactive regions, side by side</h2>
 * <p>The tile is split into two non-overlapping click targets:</p>
 * <ul>
 *   <li><b>Media area</b> (the {@code &lt;button&gt;}) — clicking opens the
 *       lightbox so the asset plays at its natural aspect ratio.</li>
 *   <li><b>Photographer overlay</b> (the {@code &lt;Link&gt;}) — clicking the
 *       name / location row navigates to the photographer's profile.</li>
 * </ul>
 *
 * <p>They're rendered as siblings inside the {@code &lt;li&gt;}, not nested
 * (nesting interactive elements is invalid HTML and breaks screen readers).
 * The Link is positioned absolutely on top of the bottom strip of the media;
 * because it sits later in DOM order, it captures clicks on the overlap.</p>
 */
function FeedTile({ item, onOpen }: { item: FeedItem; onOpen: () => void }) {
  const isVideo = item.mimeType.startsWith('video/');
  const aspect = item.mediaType === 'REEL' ? 'aspect-[9/16]' : 'aspect-[4/3]';

  return (
    <li
      // mb-3                 → vertical gap between tiles within the same column
      //                        (the `gap-3` on <ul> only controls horizontal gaps
      //                        in CSS column layout — vertical spacing comes from margin)
      // break-inside-avoid   → don't let a tile split across columns mid-content
      // ring + hover         → subtle lift + ring-on-hover for a "pinned card" feel
      className={`group relative mb-4 break-inside-avoid overflow-hidden rounded-2xl bg-slate-100 shadow-sm ring-1 ring-slate-200/80 transition-all duration-300 hover:-translate-y-0.5 hover:shadow-xl hover:ring-indigo-300/60 ${aspect}`}
    >
      {/* Media — clickable to open the lightbox */}
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
            // Silent hover preview — full playback happens in the lightbox
            onMouseEnter={e => e.currentTarget.play().catch(() => undefined)}
            onMouseLeave={e => {
              e.currentTarget.pause();
              e.currentTarget.currentTime = 0;
            }}
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

      {/* Top-right media-type pill — decorative, no click target */}
      <span className="pointer-events-none absolute right-2 top-2 rounded-full bg-black/55 px-2 py-0.5 text-[10px] font-semibold uppercase tracking-wider text-white">
        {item.mediaType}
      </span>

      {/* Top-left heart toggle — only renders for logged-in customers.
          Lives as a sibling of the media button so its click doesn't bubble
          into "open lightbox" (FavoriteButton calls stopPropagation). */}
      <div className="absolute left-2 top-2">
        <FavoriteButton portfolioItemId={item.id} />
      </div>

      {/* Bottom overlay — clickable Link to the photographer's profile.
          Stop-propagation isn't strictly needed (siblings, not nested) but
          it makes intent obvious and is cheap insurance. */}
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

/** Skeleton tiles shown while the feed is loading — a hint of layout so the
 *  page doesn't feel empty during the request. */
function LoadingSkeleton() {
  // Vary aspect ratios so the skeleton matches the masonry the real feed produces.
  const aspects = ['aspect-[4/3]', 'aspect-[9/16]', 'aspect-[4/3]', 'aspect-[4/3]', 'aspect-[9/16]', 'aspect-[4/3]'];
  return (
    <ul className="gap-3 columns-1 sm:columns-2 lg:columns-3">
      {aspects.map((a, i) => (
        <li
          key={i}
          className={`mb-4 break-inside-avoid animate-pulse rounded-2xl bg-slate-200/70 ring-1 ring-slate-200/80 ${a}`}
        />
      ))}
    </ul>
  );
}

/**
 * Search box that filters the feed by photographer name, location, or category.
 *
 * Controlled input (parent owns the state) so the result count and "no matches"
 * messaging can react to the same value. Escape clears the input — common
 * pattern for marketplace explore pages.
 */
function SearchInput({
  value,
  onChange,
}: {
  value: string;
  onChange: (next: string) => void;
}) {
  return (
    <div className="relative">
      {/* Inline SVG to keep the dependency footprint at zero — no icon library */}
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
        onKeyDown={e => {
          if (e.key === 'Escape') onChange('');
        }}
        placeholder="Search by photographer, location, or category..."
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

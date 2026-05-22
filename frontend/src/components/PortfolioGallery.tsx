import { useMemo, useState } from 'react';
import type { MediaKind, PortfolioItem } from '../lib/types';
import { MediaLightbox } from './MediaLightbox';

export function PortfolioGallery({
  items,
  photographerName,
}: {
  items: PortfolioItem[];
  photographerName: string;
}) {
  const [activeCategory, setActiveCategory] = useState<string | null>(null);
  const [activeKind, setActiveKind] = useState<MediaKind | 'ALL'>('ALL');
  const [lightboxItem, setLightboxItem] = useState<PortfolioItem | null>(null);

  const categories = useMemo(
    () => Array.from(new Set(items.map(i => i.category))).sort(),
    [items],
  );

  const filtered = items.filter(
    i =>
      (activeCategory === null || i.category === activeCategory) &&
      (activeKind === 'ALL' || i.mediaType === activeKind),
  );

  if (items.length === 0) {
    return (
      <div className="rounded-2xl border border-dashed border-slate-300 bg-white/60 p-12 text-center text-slate-500">
        <p className="text-base font-medium text-slate-700">No portfolio items yet</p>
        <p className="mt-1 text-sm">{photographerName} hasn't uploaded any samples.</p>
      </div>
    );
  }

  return (
    <div>
      {/* ── Sticky filter bar ─────────────────────────────────────────────── */}
      <div className="sticky top-14 z-10 -mx-4 mb-6 border-b border-slate-200/70 bg-white/70 px-4 py-3 backdrop-blur-lg supports-[backdrop-filter]:bg-white/55">
        <div className="flex flex-wrap items-center gap-2">
          {/* Media-type chips */}
          <FilterChip active={activeKind === 'ALL'}   onClick={() => setActiveKind('ALL')}   label="All"    />
          <FilterChip active={activeKind === 'IMAGE'} onClick={() => setActiveKind('IMAGE')} label="Photos" />
          <FilterChip active={activeKind === 'VIDEO'} onClick={() => setActiveKind('VIDEO')} label="Videos" />
          <FilterChip active={activeKind === 'REEL'}  onClick={() => setActiveKind('REEL')}  label="Reels"  />

          {/* Category chips — only shown when there is more than one category */}
          {categories.length > 1 && (
            <>
              <span className="mx-1 text-slate-300" aria-hidden>|</span>
              <FilterChip
                active={activeCategory === null}
                onClick={() => setActiveCategory(null)}
                label="All categories"
              />
              {categories.map(cat => (
                <FilterChip
                  key={cat}
                  active={activeCategory === cat}
                  onClick={() => setActiveCategory(cat)}
                  label={cat}
                />
              ))}
            </>
          )}

          <span className="ml-auto text-xs font-medium text-slate-500">
            {filtered.length} {filtered.length === 1 ? 'item' : 'items'}
          </span>
        </div>
      </div>

      {/* ── Empty-filter state ────────────────────────────────────────────── */}
      {filtered.length === 0 ? (
        <div className="rounded-xl border border-slate-200 bg-white px-4 py-3 text-sm text-slate-600 shadow-sm">
          No items match that filter.{' '}
          <button
            type="button"
            onClick={() => { setActiveKind('ALL'); setActiveCategory(null); }}
            className="font-semibold text-indigo-600 hover:underline"
          >
            Clear filters
          </button>
        </div>
      ) : (
        /* ── Masonry grid (same columns as Browse) ──────────────────────── */
        <ul className="columns-1 gap-3 sm:columns-2 lg:columns-3">
          {filtered.map(item => (
            <GalleryTile key={item.id} item={item} onOpen={() => setLightboxItem(item)} />
          ))}
        </ul>
      )}

      {lightboxItem && (
        <MediaLightbox item={lightboxItem} onClose={() => setLightboxItem(null)} />
      )}
    </div>
  );
}

// ─────────────────────────────────────────────────────────────────────────────
// Single masonry tile — mirrors FeedTile in BrowsePage
// ─────────────────────────────────────────────────────────────────────────────

function GalleryTile({ item, onOpen }: { item: PortfolioItem; onOpen: () => void }) {
  const isVideo = item.mimeType.startsWith('video/');
  const aspect = item.mediaType === 'REEL' ? 'aspect-[9/16]' : 'aspect-[4/3]';

  return (
    <li
      className={`group relative mb-4 break-inside-avoid overflow-hidden rounded-2xl bg-slate-100 shadow-sm ring-1 ring-slate-200/80 transition-all duration-300 hover:-translate-y-0.5 hover:shadow-xl hover:ring-indigo-300/60 ${aspect}`}
    >
      {/* Full-tile click target for the lightbox */}
      <button
        type="button"
        onClick={onOpen}
        aria-label={`Open ${item.mediaType.toLowerCase()} preview`}
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
            alt={`${item.category} sample`}
            loading="lazy"
            className="h-full w-full object-cover"
          />
        )}
      </button>

      {/* Top-right media-type badge */}
      <span className="pointer-events-none absolute right-2 top-2 rounded-full bg-black/55 px-2 py-0.5 text-[10px] font-semibold uppercase tracking-wider text-white">
        {item.mediaType}
      </span>

      {/* Bottom gradient overlay — category label */}
      <div className="pointer-events-none absolute inset-x-0 bottom-0 bg-gradient-to-t from-black/70 via-black/40 to-transparent p-3">
        <p className="text-xs text-white/80">{item.category}</p>
      </div>
    </li>
  );
}

// ─────────────────────────────────────────────────────────────────────────────
// Filter chip — same style as BrowsePage's FilterChip
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

import { useMemo, useState } from 'react';
import type { MediaKind, PortfolioItem } from '../lib/types';
import { MediaLightbox } from './MediaLightbox';

/**
 * Customer-facing gallery — renders a photographer's portfolio with optional
 * filtering by category and media kind.
 *
 * <h2>Tile vs lightbox</h2>
 * <p>Tiles are uniform 4:3 (or 9:16 for reels) and cropped with
 * {@code object-cover} so the grid looks tidy. <em>Clicking</em> a tile opens
 * a {@link MediaLightbox} that shows the asset at its <strong>natural aspect
 * ratio</strong> with full video controls — that's where the customer
 * actually watches the reel or zooms into a photo.</p>
 *
 * <p>Filter chips are derived from the items themselves so the photographer's
 * own vocabulary drives the list (no hard-coded category enum).</p>
 */
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

  // Unique categories sorted alphabetically — drives the filter chip row.
  const categories = useMemo(() => {
    return Array.from(new Set(items.map(i => i.category))).sort();
  }, [items]);

  const filtered = items.filter(
    i =>
      (activeCategory === null || i.category === activeCategory) &&
      (activeKind === 'ALL' || i.mediaType === activeKind),
  );

  if (items.length === 0) {
    return (
      <p className="text-sm text-gray-500">
        {photographerName} hasn't uploaded any samples yet.
      </p>
    );
  }

  return (
    <div>
      {/* Filter row */}
      <div className="mb-4 flex flex-wrap items-center gap-x-3 gap-y-2 text-sm">
        <span className="font-medium text-gray-600">Filter:</span>

        <FilterPill
          active={activeKind === 'ALL'}
          onClick={() => setActiveKind('ALL')}
          label="All media"
        />
        {(['IMAGE', 'VIDEO', 'REEL'] as MediaKind[]).map(k => (
          <FilterPill
            key={k}
            active={activeKind === k}
            onClick={() => setActiveKind(k)}
            label={kindLabel(k)}
          />
        ))}

        {categories.length > 0 && (
          <>
            <span className="ml-2 hidden text-gray-300 sm:inline">·</span>
            <FilterPill
              active={activeCategory === null}
              onClick={() => setActiveCategory(null)}
              label="All categories"
            />
            {categories.map(cat => (
              <FilterPill
                key={cat}
                active={activeCategory === cat}
                onClick={() => setActiveCategory(cat)}
                label={cat}
              />
            ))}
          </>
        )}
      </div>

      {/* Grid */}
      {filtered.length === 0 ? (
        <p className="text-sm text-gray-500">No items match that filter.</p>
      ) : (
        <ul className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
          {filtered.map(item => (
            <li
              key={item.id}
              className="overflow-hidden rounded-lg border border-gray-200 bg-white shadow-sm"
            >
              <Thumbnail item={item} onOpen={() => setLightboxItem(item)} />
              <div className="flex items-center justify-between px-3 py-2 text-xs text-gray-500">
                <span className="font-medium text-gray-700">{kindLabel(item.mediaType)}</span>
                <span>{item.category}</span>
              </div>
            </li>
          ))}
        </ul>
      )}

      {lightboxItem && (
        <MediaLightbox item={lightboxItem} onClose={() => setLightboxItem(null)} />
      )}
    </div>
  );
}

function FilterPill({
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
      className={`rounded-full px-2.5 py-0.5 text-xs font-medium transition ${
        active
          ? 'bg-indigo-600 text-white'
          : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
      }`}
    >
      {label}
    </button>
  );
}

/**
 * Grid thumbnail. Crops to the tile aspect for uniform layout. For videos we
 * intentionally omit the {@code controls} attribute — the in-grid view is a
 * preview, NOT the player. A play-button overlay hints that clicking opens
 * the lightbox for proper playback at natural aspect ratio.
 */
function Thumbnail({
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
      aria-label={`Open ${kindLabel(item.mediaType).toLowerCase()} preview`}
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

      {/* Play-button overlay for videos — hints "click to watch in full" */}
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

function kindLabel(k: MediaKind): string {
  return k === 'REEL' ? 'Reels' : k === 'VIDEO' ? 'Videos' : 'Photos';
}

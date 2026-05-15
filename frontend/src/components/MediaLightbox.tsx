import { useEffect, type ReactNode } from 'react';
import type { MediaKind } from '../lib/types';

/**
 * The minimum a lightbox needs to render a tile's full-resolution view.
 *
 * <p>Both {@code PortfolioItem} (used by the photographer's profile page) and
 * {@code FeedItem} (used by the marketplace browse feed) satisfy this shape
 * structurally — so the same component renders for both call sites.</p>
 */
export interface MediaAsset {
  publicUrl: string;
  mimeType: string;
  mediaType: MediaKind;
  category: string;
}

/**
 * Full-screen overlay that shows a single portfolio item at its <em>natural</em>
 * aspect ratio. Grid tiles can use {@code aspect-[4/3]} + {@code object-cover}
 * for a uniform layout; clicking a tile opens this lightbox so the customer
 * sees the actual asset (no cropping, no letterboxing of vertical content).
 *
 * <h2>Interaction</h2>
 * <ul>
 *   <li>Click outside the media (on the backdrop) → close</li>
 *   <li>Escape key → close</li>
 *   <li>The body scroll is locked while the lightbox is open so the page
 *       behind doesn't drift around.</li>
 *   <li>Click on the media itself does NOT close — so a user can drag a
 *       scrub-bar or tap play without dismissing.</li>
 * </ul>
 *
 * <h2>Why a separate component?</h2>
 * <p>Both the customer-facing {@code PortfolioGallery} and the photographer's
 * own {@code PortfolioPage} reuse this — one accessibility model, one set of
 * keyboard handlers.</p>
 */
export function MediaLightbox({
  item,
  onClose,
  footer,
}: {
  item: MediaAsset;
  onClose: () => void;
  /** Optional content rendered under the caption (e.g. a "View profile" link). */
  footer?: ReactNode;
}) {
  // Lock body scroll + listen for Escape while the lightbox is open.
  useEffect(() => {
    const prevOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';

    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose();
    };
    window.addEventListener('keydown', onKey);
    return () => {
      document.body.style.overflow = prevOverflow;
      window.removeEventListener('keydown', onKey);
    };
  }, [onClose]);

  const isVideo = item.mimeType.startsWith('video/');

  return (
    <div
      // Backdrop — clicking here closes
      role="dialog"
      aria-modal="true"
      aria-label="Media preview"
      onClick={onClose}
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/80 p-4 backdrop-blur-sm"
    >
      {/* Close button (top-right of the viewport, not the media) */}
      <button
        type="button"
        onClick={onClose}
        aria-label="Close"
        className="absolute right-4 top-4 rounded-full bg-white/10 p-2 text-white transition hover:bg-white/20"
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
          className="h-5 w-5"
        >
          <path d="M18 6 6 18M6 6l12 12" />
        </svg>
      </button>

      {/* Media stage — stops propagation so clicks on the media don't close */}
      <div
        onClick={e => e.stopPropagation()}
        className="relative flex max-h-[90vh] max-w-[95vw] flex-col items-center justify-center"
      >
        {isVideo ? (
          <video
            src={item.publicUrl}
            controls
            autoPlay
            playsInline
            preload="metadata"
            // max-h/max-w let the browser size the element at the natural aspect
            // (no object-cover/contain on the element itself — the intrinsic
            // video dimensions drive the box).
            className="max-h-[85vh] max-w-full rounded-md bg-black shadow-2xl"
          />
        ) : (
          <img
            src={item.publicUrl}
            alt={`${item.category} sample`}
            className="max-h-[85vh] max-w-full rounded-md bg-black shadow-2xl"
          />
        )}

        {/* Caption strip */}
        <div className="mt-3 flex w-full items-center justify-between gap-2 text-sm text-white/90">
          <span className="rounded-full bg-white/10 px-2.5 py-0.5 text-xs font-medium uppercase tracking-wider">
            {item.mediaType}
          </span>
          <span className="text-white/80">{item.category}</span>
        </div>

        {/* Caller-supplied footer — e.g. "View Alice's profile" from the feed */}
        {footer && (
          <div className="mt-2 w-full text-sm text-white/90">{footer}</div>
        )}
      </div>
    </div>
  );
}

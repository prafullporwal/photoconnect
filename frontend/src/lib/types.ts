// =============================================================================
// PhotoConnect API types
// =============================================================================
// Each interface mirrors a backend DTO. We're deliberately structural-only —
// no class instances, no validation here. Validation belongs in the form layer
// (via zod schemas) and in the backend (which is the source of truth).
//
// When the backend adds fields we don't yet care about, our tolerant readers
// will just ignore them. When the backend removes a field, TypeScript will
// surface usage sites that need updating — that's the win.
// =============================================================================

// ── Auth ─────────────────────────────────────────────────────────────────────

export type Role = 'PHOTOGRAPHER' | 'CUSTOMER';

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  email: string;
  password: string;
  role: Role;
}

/** Response from POST /auth/register or /auth/login. */
export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  accessTokenExpiresAt: string;   // ISO-8601 Instant
  userId: string;                  // UUID
  email: string;
  role: Role;
}

export interface RefreshRequest {
  refreshToken: string;
}

// ── Photographer ─────────────────────────────────────────────────────────────

export interface PhotographerProfile {
  id: string;
  userId: string;
  displayName: string;
  bio: string | null;
  location: string;
  yearsOfExperience: number;
  pricePerHour: number;
  available: boolean;
  specialties: string[];
  createdAt: string;
  updatedAt: string;
}

export interface CreatePhotographerProfileRequest {
  displayName: string;
  bio?: string;
  location: string;
  yearsOfExperience: number;
  pricePerHour: number;
  specialties: string[];
}

export interface UpdatePhotographerProfileRequest extends CreatePhotographerProfileRequest {
  available: boolean;
}

// ── Marketplace feed ─────────────────────────────────────────────────────────

/**
 * One tile on the marketplace browse feed. The backend joins PortfolioItem
 * with PhotographerProfile so a single response carries everything the SPA
 * needs to render a tile + photographer overlay + click-through.
 */
export interface FeedItem {
  id: string;
  mediaType: MediaKind;
  category: string;
  mimeType: string;
  publicUrl: string;
  uploadedAt: string;
  photographer: {
    id: string;
    displayName: string;
    location: string;
  };
}

// ── Portfolio (sample media) ─────────────────────────────────────────────────

/**
 * Editorial bucket for portfolio items. The split between VIDEO and REEL is
 * about presentation (landscape vs vertical), not encoding.
 */
export type MediaKind = 'IMAGE' | 'VIDEO' | 'REEL';

export interface PortfolioItem {
  id: string;
  photographerProfileId: string;
  mediaType: MediaKind;
  category: string;
  mimeType: string;
  sizeBytes: number;
  /** Direct-fetch URL into the bucket — render via <img> or <video> tags. */
  publicUrl: string;
  displayOrder: number;
  uploadedAt: string;
}

// ── Customer ─────────────────────────────────────────────────────────────────

export type ContactMethod = 'EMAIL' | 'PHONE' | 'PLATFORM';

export interface Customer {
  id: string;
  userId: string;
  displayName: string;
  location: string | null;
  phoneNumber: string | null;
  preferredContactMethod: ContactMethod;
  createdAt: string;
  updatedAt: string;
}

export interface CreateCustomerRequest {
  displayName: string;
  location?: string;
  phoneNumber?: string;
  preferredContactMethod: ContactMethod;
}

export type UpdateCustomerRequest = CreateCustomerRequest;

// ── Inquiry ──────────────────────────────────────────────────────────────────

export type InquiryStatus = 'NEW' | 'READ' | 'RESPONDED' | 'CLOSED';

export interface Inquiry {
  id: string;
  customerId: string;
  photographerProfileId: string;
  photographerUserId: string;
  eventDate: string;       // ISO LocalDate (YYYY-MM-DD)
  eventType: string;
  location: string | null;
  budget: number | null;
  message: string;
  status: InquiryStatus;
  createdAt: string;
  updatedAt: string;
}

export interface CreateInquiryRequest {
  photographerProfileId: string;
  eventDate: string;
  eventType: string;
  location?: string;
  budget?: number;
  message: string;
}

export interface UpdateInquiryStatusRequest {
  status: InquiryStatus;
}

// ── Favorites ────────────────────────────────────────────────────────────────

/**
 * A saved content item. `item` is null when the portfolio item was deleted
 * after the customer saved it — render a tombstone in that case.
 */
export interface Favorite {
  id: string;
  portfolioItemId: string;
  item: FeedItem | null;
  createdAt: string;
}

export interface FavoriteStatus {
  favorited: boolean;
}

// ── Availability calendar ────────────────────────────────────────────────────

/**
 * One day on a photographer's posted availability calendar.
 *
 * <p>{@code availableDate} is an ISO-8601 LocalDate string (YYYY-MM-DD);
 * granularity is intentionally per-day — fine-grained slots are negotiated
 * inside the inquiry conversation rather than in the calendar.</p>
 */
export interface AvailabilitySlot {
  id: string;
  photographerProfileId: string;
  availableDate: string;
  note: string | null;
  createdAt: string;
}

/** Bulk-add payload — the editor sends every newly-toggled date in one call. */
export interface AddAvailabilityRequest {
  dates: string[];
  note?: string;
}

// ── Error envelope ───────────────────────────────────────────────────────────

/**
 * Shape returned by GlobalExceptionHandler on all four backend services.
 * The `correlationId` is the operator-side handle — shown in error UIs so
 * users can quote it in support tickets.
 */
export interface ApiErrorResponse {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
  correlationId: string;
  fieldErrors: Record<string, string> | null;
}

package com.photoconnect.customer.dto;

/**
 * Tiny response for {@code GET /favorites/{photographerProfileId}/status}.
 *
 * <p>Used by the heart-button on the photographer detail page to render the
 * filled-vs-outlined state on initial load. Returning a single boolean inside
 * a JSON object (rather than a bare {@code true}/{@code false}) leaves room
 * for adding fields later — e.g. {@code savedAt} — without breaking the
 * contract.</p>
 */
public record FavoriteStatusResponse(boolean favorited) {}

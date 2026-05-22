package com.photoconnect.customer.domain;

/**
 * Inquiry lifecycle states.
 * <pre>
 *                                                              ┌──▶ CLOSED     (no booking)
 *   NEW ──photographer reads──▶ READ ──photographer responds──▶ RESPONDED ──┤
 *                                                              └──▶ COMPLETED (shoot happened)
 * </pre>
 *
 * <p>{@link #COMPLETED} is the precondition reviews-service checks before
 * allowing a customer to leave a review. It is intentionally distinct from
 * {@link #CLOSED}, which means "we talked and decided not to proceed". In
 * Phase 2 the inquiry-as-booking-proxy goes away and a dedicated {@code
 * booking-service} owns this terminal state.</p>
 */
public enum InquiryStatus {
    /** Just created by the customer; photographer has not viewed it yet. */
    NEW,
    /** Photographer has opened the inquiry. */
    READ,
    /** Photographer has replied (out of band — Phase 2 builds a messaging thread). */
    RESPONDED,
    /** Conversation finished — either party can close. */
    CLOSED,
    /** Shoot happened. Unlocks the right to leave a review. */
    COMPLETED
}

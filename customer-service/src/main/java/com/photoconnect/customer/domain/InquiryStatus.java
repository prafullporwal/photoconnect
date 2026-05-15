package com.photoconnect.customer.domain;

/**
 * Inquiry lifecycle states.
 * <pre>
 *   NEW ──photographer reads──▶ READ ──photographer responds──▶ RESPONDED ──either party──▶ CLOSED
 * </pre>
 */
public enum InquiryStatus {
    /** Just created by the customer; photographer has not viewed it yet. */
    NEW,
    /** Photographer has opened the inquiry. */
    READ,
    /** Photographer has replied (out of band — Phase 2 builds a messaging thread). */
    RESPONDED,
    /** Conversation finished — either party can close. */
    CLOSED
}

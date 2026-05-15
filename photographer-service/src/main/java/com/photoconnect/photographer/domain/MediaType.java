package com.photoconnect.photographer.domain;

/**
 * What kind of media a portfolio item is.
 *
 * <ul>
 *   <li>{@link #IMAGE} — still photo (JPEG, PNG, WebP, etc.)</li>
 *   <li>{@link #VIDEO} — longer-form video (behind-the-scenes, full event clip)</li>
 *   <li>{@link #REEL}  — short vertical video meant for social-style preview</li>
 * </ul>
 *
 * <p>The split between {@code VIDEO} and {@code REEL} is editorial, not
 * technical — the photographer tells us which it is. The SPA renders them in
 * different layouts (landscape grid vs vertical reel strip).</p>
 */
public enum MediaType {
    IMAGE,
    VIDEO,
    REEL
}

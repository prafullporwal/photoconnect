package com.photoconnect.photographer.service;

import com.photoconnect.photographer.domain.AvailabilitySlot;
import com.photoconnect.photographer.domain.PhotographerProfile;
import com.photoconnect.photographer.dto.AddAvailabilityRequest;
import com.photoconnect.photographer.dto.AvailabilitySlotResponse;
import com.photoconnect.photographer.exception.AvailabilitySlotNotFoundException;
import com.photoconnect.photographer.exception.ProfileNotFoundException;
import com.photoconnect.photographer.repository.AvailabilitySlotRepository;
import com.photoconnect.photographer.repository.PhotographerProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Manages a photographer's day-level availability calendar.
 *
 * <h2>Allowlist model</h2>
 * <p>An empty calendar means "no available dates posted" — the photographer
 * still receives inquiries via the public profile, but they're not encouraged
 * with a specific date suggestion. Photographers explicitly add the days they
 * have capacity for; nothing is implicit.</p>
 *
 * <h2>Bulk add semantics</h2>
 * <p>{@link #addBulk} is an idempotent set-union: dates already in the
 * calendar are silently skipped, past dates are filtered out, and any
 * surviving new dates are inserted. A unique-constraint race (two requests
 * adding the same date concurrently) is caught and treated as "already there."</p>
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class AvailabilityService {

    private final AvailabilitySlotRepository slotRepo;
    private final PhotographerProfileRepository profileRepo;

    // ── Owner endpoints (PHOTOGRAPHER role) ───────────────────────────────────

    /** List the calling photographer's own calendar. */
    @Transactional(readOnly = true)
    public List<AvailabilitySlotResponse> listMine(UUID userId) {
        PhotographerProfile profile = resolveProfile(userId);
        return slotRepo
                .findByPhotographerProfileIdOrderByAvailableDateAsc(profile.getId())
                .stream()
                .map(AvailabilityService::toResponse)
                .toList();
    }

    /**
     * Add one or more available dates to the calling photographer's calendar.
     * Returns the updated full calendar so the SPA can refresh in one round-trip.
     */
    public List<AvailabilitySlotResponse> addBulk(UUID userId, AddAvailabilityRequest request) {
        PhotographerProfile profile = resolveProfile(userId);
        LocalDate today = LocalDate.now();
        List<AvailabilitySlot> inserted = new ArrayList<>();

        // Filter past dates server-side rather than rejecting the whole request —
        // calendar UIs can drift across day boundaries.
        for (LocalDate date : request.dates().stream().distinct().toList()) {
            if (date.isBefore(today)) {
                log.debug("Skipping past date {} for profile {}", date, profile.getId());
                continue;
            }
            if (slotRepo.existsByPhotographerProfileIdAndAvailableDate(profile.getId(), date)) {
                continue;
            }
            AvailabilitySlot slot = new AvailabilitySlot();
            slot.setPhotographerProfileId(profile.getId());
            slot.setAvailableDate(date);
            slot.setNote(request.note());
            try {
                inserted.add(slotRepo.saveAndFlush(slot));
            } catch (DataIntegrityViolationException race) {
                // Concurrent add for the same (profile, date). The winner's row
                // is already there; skip this one and move on.
                log.debug("Concurrent add for ({}, {}) — skipping", profile.getId(), date);
            }
        }

        if (!inserted.isEmpty()) {
            log.info("Photographer {} added {} new availability slots",
                    profile.getId(), inserted.size());
        }
        return slotRepo
                .findByPhotographerProfileIdOrderByAvailableDateAsc(profile.getId())
                .stream()
                .map(AvailabilityService::toResponse)
                .toList();
    }

    /** Delete a single slot owned by the caller. */
    public void delete(UUID userId, UUID slotId) {
        PhotographerProfile profile = resolveProfile(userId);
        int removed = slotRepo.deleteByOwnedId(profile.getId(), slotId);
        if (removed == 0) {
            // Either the slot doesn't exist, or it belongs to someone else.
            // We don't distinguish — that would leak info about other photographers.
            throw new AvailabilitySlotNotFoundException(slotId);
        }
        log.info("Photographer {} removed availability slot {}", profile.getId(), slotId);
    }

    /** Clear the caller's whole calendar. Used when wiping & re-importing dates. */
    public void clearAll(UUID userId) {
        PhotographerProfile profile = resolveProfile(userId);
        int removed = slotRepo.deleteAllForProfile(profile.getId());
        log.info("Photographer {} cleared {} availability slots", profile.getId(), removed);
    }

    // ── Public / cross-service reads ──────────────────────────────────────────

    /**
     * Public-facing list of a photographer's available dates. Used by the SPA
     * date picker AND by customer-service's pre-inquiry availability check
     * (via Feign). The endpoint stays public — there's no PII here, only
     * scheduling info the customer is about to act on anyway.
     */
    @Transactional(readOnly = true)
    public List<AvailabilitySlotResponse> listForProfile(UUID photographerProfileId) {
        return slotRepo
                .findByPhotographerProfileIdOrderByAvailableDateAsc(photographerProfileId)
                .stream()
                .map(AvailabilityService::toResponse)
                .toList();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private PhotographerProfile resolveProfile(UUID userId) {
        return profileRepo.findByUserId(userId)
                .orElseThrow(() -> new ProfileNotFoundException(userId));
    }

    private static AvailabilitySlotResponse toResponse(AvailabilitySlot s) {
        return new AvailabilitySlotResponse(
                s.getId(),
                s.getPhotographerProfileId(),
                s.getAvailableDate(),
                s.getNote(),
                s.getCreatedAt());
    }
}

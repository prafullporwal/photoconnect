package com.photoconnect.photographer.service;

import com.photoconnect.photographer.domain.PhotographerProfile;
import com.photoconnect.photographer.dto.CreateProfileRequest;
import com.photoconnect.photographer.dto.PhotographerProfileResponse;
import com.photoconnect.photographer.dto.UpdateProfileRequest;
import com.photoconnect.photographer.exception.ProfileAlreadyExistsException;
import com.photoconnect.photographer.exception.ProfileNotFoundException;
import com.photoconnect.photographer.mapper.PhotographerMapper;
import com.photoconnect.photographer.repository.PhotographerProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Core business logic for photographer profile management.
 *
 * <h2>Transaction policy</h2>
 * <p>{@code @Transactional} on the class sets the default for all methods.
 * Read methods override with {@code readOnly = true} — this tells Hibernate
 * to skip dirty checking on flush (minor perf gain) and hints to the database
 * driver to use a read replica if one is configured.</p>
 *
 * <h2>Profile lifecycle</h2>
 * <ol>
 *   <li><b>Create</b> — one profile per user; duplicate attempt → 409.</li>
 *   <li><b>Read own</b> — found by {@code userId} (from auth header), not PK.</li>
 *   <li><b>Read by ID</b> — public browse; found by profile PK.</li>
 *   <li><b>List</b> — only {@code available = true} profiles appear publicly.</li>
 *   <li><b>Update</b> — full replacement (PUT). MapStruct {@code @MappingTarget}
 *       mutates the existing entity in-place so identity fields are never touched.</li>
 *   <li><b>Delete</b> — hard delete. Phase 2: soft-delete with {@code deleted_at}.</li>
 * </ol>
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class PhotographerService {

    private final PhotographerProfileRepository repository;
    private final PhotographerMapper mapper;

    // ── Write operations (default: @Transactional, read-write) ───────────────

    /**
     * Create a new photographer profile.
     *
     * @throws ProfileAlreadyExistsException if this user already has a profile
     */
    public PhotographerProfileResponse createProfile(UUID userId, CreateProfileRequest request) {
        if (repository.existsByUserId(userId)) {
            throw new ProfileAlreadyExistsException(userId);
        }
        PhotographerProfile profile = mapper.toEntity(request);
        profile.setUserId(userId);
        PhotographerProfile saved = repository.save(profile);
        log.info("Created photographer profile {} for user {}", saved.getId(), userId);
        return mapper.toResponse(saved);
    }

    /**
     * Full profile replacement (PUT semantics).
     *
     * <p>MapStruct's {@code updateEntity} mutates the existing entity in-place.
     * Because the entity is managed (loaded in this transaction), Hibernate will
     * detect the field changes and issue an UPDATE without an explicit
     * {@code save()} call. We call {@code save()} anyway for clarity.</p>
     *
     * @throws ProfileNotFoundException if this user doesn't have a profile yet
     */
    public PhotographerProfileResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        PhotographerProfile profile = findByUserIdOrThrow(userId);
        mapper.updateEntity(request, profile);
        return mapper.toResponse(repository.save(profile));
    }

    /**
     * Hard-delete the profile.
     *
     * @throws ProfileNotFoundException if this user doesn't have a profile yet
     */
    public void deleteProfile(UUID userId) {
        PhotographerProfile profile = findByUserIdOrThrow(userId);
        repository.delete(profile);
        log.info("Deleted photographer profile for user {}", userId);
    }

    // ── Read operations ───────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public PhotographerProfileResponse getOwnProfile(UUID userId) {
        return mapper.toResponse(findByUserIdOrThrow(userId));
    }

    @Transactional(readOnly = true)
    public PhotographerProfileResponse getProfileById(UUID profileId) {
        return mapper.toResponse(
                repository.findById(profileId)
                        .orElseThrow(() -> new ProfileNotFoundException(profileId)));
    }

    @Transactional(readOnly = true)
    public List<PhotographerProfileResponse> listAvailableProfiles() {
        return repository.findAllByAvailableTrue()
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private PhotographerProfile findByUserIdOrThrow(UUID userId) {
        return repository.findByUserId(userId)
                .orElseThrow(() -> new ProfileNotFoundException(userId));
    }
}

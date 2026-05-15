package com.photoconnect.photographer.service;

import com.photoconnect.photographer.domain.PhotographerProfile;
import com.photoconnect.photographer.dto.CreateProfileRequest;
import com.photoconnect.photographer.dto.PhotographerProfileResponse;
import com.photoconnect.photographer.dto.UpdateProfileRequest;
import com.photoconnect.photographer.exception.ProfileAlreadyExistsException;
import com.photoconnect.photographer.exception.ProfileNotFoundException;
import com.photoconnect.photographer.mapper.PhotographerMapper;
import com.photoconnect.photographer.repository.PhotographerProfileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link PhotographerService}.
 *
 * <p>Pure unit test: Mockito only, no Spring context, no database.</p>
 */
@ExtendWith(MockitoExtension.class)
class PhotographerServiceTest {

    @Mock PhotographerProfileRepository repository;
    @Mock PhotographerMapper mapper;

    @InjectMocks PhotographerService service;

    // ── createProfile ─────────────────────────────────────────────────────────

    @Test
    void createProfile_savesAndReturnsResponse() {
        UUID userId = UUID.randomUUID();
        CreateProfileRequest request = sampleCreateRequest();
        PhotographerProfile entity  = sampleEntity(userId);
        PhotographerProfileResponse response = sampleResponse(entity);

        when(repository.existsByUserId(userId)).thenReturn(false);
        when(mapper.toEntity(request)).thenReturn(entity);
        when(repository.save(entity)).thenReturn(entity);
        when(mapper.toResponse(entity)).thenReturn(response);

        PhotographerProfileResponse result = service.createProfile(userId, request);

        assertThat(result).isSameAs(response);
        verify(repository).save(entity);
    }

    @Test
    void createProfile_duplicateUser_throwsConflict() {
        UUID userId = UUID.randomUUID();
        when(repository.existsByUserId(userId)).thenReturn(true);

        assertThatThrownBy(() -> service.createProfile(userId, sampleCreateRequest()))
                .isInstanceOf(ProfileAlreadyExistsException.class);

        verify(repository, never()).save(any());
    }

    // ── getOwnProfile ─────────────────────────────────────────────────────────

    @Test
    void getOwnProfile_found_returnsResponse() {
        UUID userId = UUID.randomUUID();
        PhotographerProfile entity   = sampleEntity(userId);
        PhotographerProfileResponse response = sampleResponse(entity);

        when(repository.findByUserId(userId)).thenReturn(Optional.of(entity));
        when(mapper.toResponse(entity)).thenReturn(response);

        assertThat(service.getOwnProfile(userId)).isSameAs(response);
    }

    @Test
    void getOwnProfile_notFound_throwsNotFound() {
        UUID userId = UUID.randomUUID();
        when(repository.findByUserId(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getOwnProfile(userId))
                .isInstanceOf(ProfileNotFoundException.class);
    }

    // ── updateProfile ─────────────────────────────────────────────────────────

    @Test
    void updateProfile_mutatesEntityAndReturnsResponse() {
        UUID userId = UUID.randomUUID();
        PhotographerProfile entity   = sampleEntity(userId);
        UpdateProfileRequest request = sampleUpdateRequest();
        PhotographerProfileResponse response = sampleResponse(entity);

        when(repository.findByUserId(userId)).thenReturn(Optional.of(entity));
        doNothing().when(mapper).updateEntity(request, entity);
        when(repository.save(entity)).thenReturn(entity);
        when(mapper.toResponse(entity)).thenReturn(response);

        PhotographerProfileResponse result = service.updateProfile(userId, request);

        assertThat(result).isSameAs(response);
        // Verify the in-place mutation was called, not a new toEntity()
        verify(mapper).updateEntity(request, entity);
        verify(mapper, never()).toEntity(any(CreateProfileRequest.class));
    }

    @Test
    void updateProfile_notFound_throwsNotFound() {
        UUID userId = UUID.randomUUID();
        when(repository.findByUserId(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateProfile(userId, sampleUpdateRequest()))
                .isInstanceOf(ProfileNotFoundException.class);
    }

    // ── deleteProfile ─────────────────────────────────────────────────────────

    @Test
    void deleteProfile_callsRepositoryDelete() {
        UUID userId = UUID.randomUUID();
        PhotographerProfile entity = sampleEntity(userId);
        when(repository.findByUserId(userId)).thenReturn(Optional.of(entity));

        service.deleteProfile(userId);

        verify(repository).delete(entity);
    }

    // ── Test data helpers ─────────────────────────────────────────────────────

    private static CreateProfileRequest sampleCreateRequest() {
        return new CreateProfileRequest(
                "Alice Photography",
                "Capturing moments since 2010.",
                "New York, NY",
                14,
                new BigDecimal("250.00"),
                List.of("wedding", "portrait"));
    }

    private static UpdateProfileRequest sampleUpdateRequest() {
        return new UpdateProfileRequest(
                "Alice Photography Updated",
                "Bio updated.",
                "Brooklyn, NY",
                15,
                new BigDecimal("275.00"),
                List.of("wedding", "portrait", "commercial"),
                false);
    }

    private static PhotographerProfile sampleEntity(UUID userId) {
        PhotographerProfile p = new PhotographerProfile();
        p.setId(UUID.randomUUID());
        p.setUserId(userId);
        p.setDisplayName("Alice Photography");
        p.setLocation("New York, NY");
        p.setYearsOfExperience(14);
        p.setPricePerHour(new BigDecimal("250.00"));
        p.setAvailable(true);
        p.setSpecialties(List.of("wedding", "portrait"));
        p.setCreatedAt(Instant.now());
        p.setUpdatedAt(Instant.now());
        return p;
    }

    private static PhotographerProfileResponse sampleResponse(PhotographerProfile p) {
        return new PhotographerProfileResponse(
                p.getId(), p.getUserId(), p.getDisplayName(), p.getBio(),
                p.getLocation(), p.getYearsOfExperience(), p.getPricePerHour(),
                p.isAvailable(), p.getSpecialties(), p.getCreatedAt(), p.getUpdatedAt());
    }
}

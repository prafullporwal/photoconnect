package com.photoconnect.photographer.mapper;

import com.photoconnect.photographer.domain.PhotographerProfile;
import com.photoconnect.photographer.dto.CreateProfileRequest;
import com.photoconnect.photographer.dto.PhotographerProfileResponse;
import com.photoconnect.photographer.dto.UpdateProfileRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

/**
 * MapStruct mapper for {@link PhotographerProfile}.
 *
 * <h2>Three operations</h2>
 * <ol>
 *   <li>{@link #toEntity(CreateProfileRequest)} — DTO → new entity on create.
 *       Fields the DTO doesn't supply ({@code id}, {@code userId},
 *       {@code createdAt}, {@code updatedAt}, {@code version}) are ignored here;
 *       the service sets {@code userId} explicitly and JPA / {@code @PrePersist}
 *       handle the rest.</li>
 *   <li>{@link #toResponse(PhotographerProfile)} — entity → response DTO.
 *       MapStruct generates a straightforward field-by-field copy.</li>
 *   <li>{@link #updateEntity(UpdateProfileRequest, PhotographerProfile)} — DTO →
 *       mutate existing entity on update. The {@code @MappingTarget} annotation
 *       is the key: MapStruct generates a void method that applies the DTO's
 *       fields onto the given entity <em>in-place</em>, so {@code id}, {@code userId},
 *       and {@code createdAt} are never accidentally overwritten.</li>
 * </ol>
 */
@Mapper(componentModel = "spring")
public interface PhotographerMapper {

    /**
     * Create a new entity from a create request.
     * The caller must still set {@code userId} before persisting.
     */
    @Mapping(target = "id",                ignore = true)
    @Mapping(target = "userId",            ignore = true)
    @Mapping(target = "available",         constant = "true")
    @Mapping(target = "createdAt",         ignore = true)
    @Mapping(target = "updatedAt",         ignore = true)
    @Mapping(target = "version",           ignore = true)
    PhotographerProfile toEntity(CreateProfileRequest request);

    /** Map entity fields to the API response record. */
    PhotographerProfileResponse toResponse(PhotographerProfile profile);

    /**
     * Apply an update request to an existing entity.
     *
     * <p>{@code @MappingTarget} tells MapStruct to generate an in-place mutator
     * rather than a factory method. Identity fields ({@code id}, {@code userId},
     * {@code createdAt}) and the JPA {@code version} counter are never touched —
     * they're ignored here and managed by Hibernate / {@code @PreUpdate}.</p>
     */
    @Mapping(target = "id",        ignore = true)
    @Mapping(target = "userId",    ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version",   ignore = true)
    void updateEntity(UpdateProfileRequest request, @MappingTarget PhotographerProfile profile);
}

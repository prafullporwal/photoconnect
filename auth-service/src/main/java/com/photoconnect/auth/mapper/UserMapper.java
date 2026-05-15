package com.photoconnect.auth.mapper;

import com.photoconnect.auth.domain.User;
import com.photoconnect.auth.dto.UserDto;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

/**
 * MapStruct compile-time mapper from {@link User} to {@link UserDto}.
 *
 * <p>{@code componentModel = "spring"} makes MapStruct generate a
 * Spring-managed bean; inject it like any other.</p>
 *
 * <p>Why MapStruct over manual mapping or ModelMapper?</p>
 * <ul>
 *   <li>Zero reflection — code is generated at compile time, so it's as
 *       fast as hand-written code.</li>
 *   <li>Compile-time safety — if a field doesn't exist, your build breaks
 *       loudly instead of a silent runtime null.</li>
 *   <li>The annotation processor lives on the parent POM's
 *       compiler-plugin config, so no per-module setup.</li>
 * </ul>
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserMapper {

    UserDto toDto(User user);
}

package com.photoconnect.photographer.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.photoconnect.photographer.domain.Role;
import com.photoconnect.photographer.dto.CreateProfileRequest;
import com.photoconnect.photographer.dto.PhotographerProfileResponse;
import com.photoconnect.photographer.dto.UpdateProfileRequest;
import com.photoconnect.photographer.exception.GlobalExceptionHandler;
import com.photoconnect.photographer.exception.ProfileAlreadyExistsException;
import com.photoconnect.photographer.exception.ProfileNotFoundException;
import com.photoconnect.photographer.security.GatewayAuthenticationFilter;
import com.photoconnect.photographer.security.GatewayPrincipal;
import com.photoconnect.photographer.security.CorrelationIdServletFilter;
import com.photoconnect.photographer.security.ServiceTokenAuthenticationFilter;
import com.photoconnect.photographer.service.PhotographerService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.boot.test.context.TestConfiguration;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Slice test for {@link PhotographerController}.
 *
 * <h2>Security setup</h2>
 * <p>Spring Security autoconfig is excluded so the test focuses on controller
 * logic, not the security filter chain. To make {@code @AuthenticationPrincipal}
 * work without the full Security setup, we register
 * {@link AuthenticationPrincipalArgumentResolver} via a {@link TestConfiguration}
 * implementing {@link WebMvcConfigurer}.</p>
 *
 * <p>For tests that need an authenticated principal, we set
 * {@link SecurityContextHolder} directly in the test method. The resolver
 * reads from there. We clear it in {@link #clearSecurityContext()} so test
 * isolation is guaranteed.</p>
 *
 * <h2>Filter exclusions</h2>
 * <p>Both {@link GatewayAuthenticationFilter} and {@link CorrelationIdServletFilter}
 * are excluded from the slice component scan. If mocked instead of excluded,
 * Mockito's void-by-default behaviour would prevent {@code chain.doFilter()}
 * from running — the controller would never execute.</p>
 */
@WebMvcTest(
        controllers = PhotographerController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class},
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {GatewayAuthenticationFilter.class,
                           CorrelationIdServletFilter.class,
                           ServiceTokenAuthenticationFilter.class}))
@Import({GlobalExceptionHandler.class, PhotographerControllerTest.MvcConfig.class})
class PhotographerControllerTest {

    /**
     * Registers {@link AuthenticationPrincipalArgumentResolver} so that
     * {@code @AuthenticationPrincipal GatewayPrincipal} injection works in the
     * controller even though Spring Security autoconfig is excluded.
     */
    @TestConfiguration
    static class MvcConfig implements WebMvcConfigurer {
        @Override
        public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
            resolvers.add(new AuthenticationPrincipalArgumentResolver());
        }
    }

    @Autowired MockMvc        mockMvc;
    @Autowired ObjectMapper   objectMapper;
    @MockitoBean PhotographerService service;

    // Reusable test data
    private static final UUID     USER_ID   = UUID.randomUUID();
    private static final UUID     PROFILE_ID = UUID.randomUUID();
    private static final GatewayPrincipal PHOTOGRAPHER =
            new GatewayPrincipal(USER_ID, Role.PHOTOGRAPHER);

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    /** Set a GatewayPrincipal as the authenticated caller for this test thread. */
    private void authenticateAs(GatewayPrincipal principal) {
        var auth = UsernamePasswordAuthenticationToken.authenticated(
                principal, null,
                List.of(new SimpleGrantedAuthority("ROLE_" + principal.role().name())));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    // ── Public endpoints ──────────────────────────────────────────────────────

    @Test
    void listPhotographers_returns200() throws Exception {
        when(service.listAvailableProfiles()).thenReturn(List.of(sampleResponse()));

        mockMvc.perform(get("/api/v1/photographers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].displayName").value("Alice Photography"));
    }

    @Test
    void getPhotographerById_found_returns200() throws Exception {
        when(service.getProfileById(PROFILE_ID)).thenReturn(sampleResponse());

        mockMvc.perform(get("/api/v1/photographers/{id}", PROFILE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(PROFILE_ID.toString()));
    }

    @Test
    void getPhotographerById_notFound_returns404() throws Exception {
        when(service.getProfileById(PROFILE_ID))
                .thenThrow(new ProfileNotFoundException(PROFILE_ID));

        mockMvc.perform(get("/api/v1/photographers/{id}", PROFILE_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    // ── Authenticated /me endpoints ───────────────────────────────────────────

    @Test
    void getOwnProfile_authenticated_returns200() throws Exception {
        authenticateAs(PHOTOGRAPHER);
        when(service.getOwnProfile(USER_ID)).thenReturn(sampleResponse());

        mockMvc.perform(get("/api/v1/photographers/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(USER_ID.toString()));
    }

    @Test
    void createProfile_validRequest_returns201() throws Exception {
        authenticateAs(PHOTOGRAPHER);
        CreateProfileRequest body = sampleCreateRequest();
        when(service.createProfile(eq(USER_ID), any())).thenReturn(sampleResponse());

        mockMvc.perform(post("/api/v1/photographers/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.displayName").value("Alice Photography"));
    }

    @Test
    void createProfile_missingDisplayName_returns400() throws Exception {
        authenticateAs(PHOTOGRAPHER);
        // null displayName fails @NotBlank
        CreateProfileRequest body = new CreateProfileRequest(
                null, "bio", "NYC", 5, new BigDecimal("100"), List.of());

        mockMvc.perform(post("/api/v1/photographers/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.displayName").exists());
    }

    @Test
    void createProfile_duplicate_returns409() throws Exception {
        authenticateAs(PHOTOGRAPHER);
        when(service.createProfile(eq(USER_ID), any()))
                .thenThrow(new ProfileAlreadyExistsException(USER_ID));

        mockMvc.perform(post("/api/v1/photographers/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleCreateRequest())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void updateProfile_validRequest_returns200() throws Exception {
        authenticateAs(PHOTOGRAPHER);
        UpdateProfileRequest body = sampleUpdateRequest();
        when(service.updateProfile(eq(USER_ID), any())).thenReturn(sampleResponse());

        mockMvc.perform(put("/api/v1/photographers/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());
    }

    @Test
    void deleteProfile_returns204() throws Exception {
        authenticateAs(PHOTOGRAPHER);
        doNothing().when(service).deleteProfile(USER_ID);

        mockMvc.perform(delete("/api/v1/photographers/me"))
                .andExpect(status().isNoContent());
    }

    // ── Test data helpers ─────────────────────────────────────────────────────

    private static PhotographerProfileResponse sampleResponse() {
        return new PhotographerProfileResponse(
                PROFILE_ID, USER_ID, "Alice Photography", "Bio here.",
                "New York, NY", 14, new BigDecimal("250.00"),
                true, List.of("wedding", "portrait"),
                Instant.now(), Instant.now());
    }

    private static CreateProfileRequest sampleCreateRequest() {
        return new CreateProfileRequest(
                "Alice Photography", "Bio here.", "New York, NY",
                14, new BigDecimal("250.00"), List.of("wedding", "portrait"));
    }

    private static UpdateProfileRequest sampleUpdateRequest() {
        return new UpdateProfileRequest(
                "Alice Photography", "Updated bio.", "Brooklyn, NY",
                15, new BigDecimal("275.00"), List.of("wedding", "portrait", "commercial"),
                true);
    }
}

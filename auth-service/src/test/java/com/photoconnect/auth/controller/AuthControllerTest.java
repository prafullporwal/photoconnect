package com.photoconnect.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.photoconnect.auth.config.OtpProperties;
import com.photoconnect.auth.domain.Role;
import com.photoconnect.auth.dto.AuthResponse;
import com.photoconnect.auth.dto.LoginRequest;
import com.photoconnect.auth.dto.RegisterRequest;
import com.photoconnect.auth.dto.SendOtpRequest;
import com.photoconnect.auth.dto.VerifyOtpRequest;
import com.photoconnect.auth.exception.EmailAlreadyExistsException;
import com.photoconnect.auth.exception.GlobalExceptionHandler;
import com.photoconnect.auth.exception.InvalidCredentialsException;
import com.photoconnect.auth.exception.InvalidOtpException;
import com.photoconnect.auth.mapper.UserMapper;
import com.photoconnect.auth.repository.UserRepository;
import com.photoconnect.auth.security.CorrelationIdServletFilter;
import com.photoconnect.auth.security.JwtAuthenticationFilter;
import com.photoconnect.auth.security.JwtService;
import com.photoconnect.auth.service.AuthService;
import com.photoconnect.auth.service.OtpService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Slice test for {@link AuthController}: verifies request validation, status
 * codes, and the error envelope shape — without booting the full app.
 *
 * <p>We deliberately:</p>
 * <ul>
 *   <li>exclude Spring Security autoconfig (full filter chain is covered by
 *       the {@code @SpringBootTest} integration test against real Postgres);</li>
 *   <li>exclude our custom {@code Filter} {@code @Component}s from the slice —
 *       a mocked {@code void doFilter(...)} would short-circuit every request
 *       because the default Mockito behaviour is "do nothing", so
 *       {@code chain.doFilter} never runs and the controller never executes.</li>
 * </ul>
 */
@WebMvcTest(
        controllers = AuthController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class},
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {JwtAuthenticationFilter.class, CorrelationIdServletFilter.class}))
@Import({GlobalExceptionHandler.class, AuthControllerTest.OtpPropsConfig.class})
class AuthControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockitoBean AuthService authService;
    @MockitoBean JwtService jwtService;
    @MockitoBean UserRepository userRepository;
    @MockitoBean UserMapper userMapper;
    @MockitoBean OtpService otpService;

    // Constants in the slice context; controller reads only OtpProperties.devMode()
    @org.springframework.boot.test.context.TestConfiguration
    static class OtpPropsConfig {
        @org.springframework.context.annotation.Bean
        OtpProperties otpProperties() {
            return new OtpProperties(
                    OtpProperties.Provider.DEV, true, 6, Duration.ofMinutes(5), Duration.ofSeconds(30), 5);
        }
    }

    @Test
    void register_returns201AndTokens() throws Exception {
        UUID userId = UUID.randomUUID();
        when(authService.register(any())).thenReturn(new AuthResponse(
                "access.jwt.here", "refresh.jwt.here", Instant.now().plusSeconds(900),
                userId, "alice@example.com", Role.PHOTOGRAPHER));

        RegisterRequest body = new RegisterRequest("alice@example.com", "secretsecret", Role.PHOTOGRAPHER);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").value("access.jwt.here"))
                .andExpect(jsonPath("$.refreshToken").value("refresh.jwt.here"))
                .andExpect(jsonPath("$.role").value("PHOTOGRAPHER"));
    }

    @Test
    void register_invalidEmail_returns400() throws Exception {
        RegisterRequest body = new RegisterRequest("not-an-email", "secretsecret", Role.PHOTOGRAPHER);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors.email").exists());
    }

    @Test
    void register_emailExists_returns409() throws Exception {
        when(authService.register(any())).thenThrow(new EmailAlreadyExistsException("alice@example.com"));

        RegisterRequest body = new RegisterRequest("alice@example.com", "secretsecret", Role.CUSTOMER);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void login_invalidCredentials_returns401() throws Exception {
        when(authService.login(any())).thenThrow(new InvalidCredentialsException());

        LoginRequest body = new LoginRequest("alice@example.com", "wrong");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void sendOtp_devMode_returnsCodeInResponse() throws Exception {
        when(otpService.sendOtp("+919876543210"))
                .thenReturn(new OtpService.Issued("123456", Instant.now().plusSeconds(300)));

        SendOtpRequest body = new SendOtpRequest("+919876543210");

        mockMvc.perform(post("/api/v1/auth/otp/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phone").value("+919876543210"))
                .andExpect(jsonPath("$.devCode").value("123456"));
    }

    @Test
    void sendOtp_invalidPhone_returns400() throws Exception {
        // US-formatted number is rejected by the +91 regex
        SendOtpRequest body = new SendOtpRequest("+14155550123");

        mockMvc.perform(post("/api/v1/auth/otp/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.phone").exists());
    }

    @Test
    void verifyOtp_correctCode_returnsTokens() throws Exception {
        UUID userId = UUID.randomUUID();
        when(otpService.verify(eq("+919876543210"), eq("123456"))).thenReturn(true);
        when(authService.registerOrLoginViaOtp(eq("+919876543210"), any(), eq(Role.CUSTOMER)))
                .thenReturn(new AuthResponse(
                        "access.jwt", "refresh.jwt", Instant.now().plusSeconds(900),
                        userId, null, Role.CUSTOMER));

        VerifyOtpRequest body = new VerifyOtpRequest(
                "+919876543210", "123456", Role.CUSTOMER, null);

        mockMvc.perform(post("/api/v1/auth/otp/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access.jwt"))
                .andExpect(jsonPath("$.role").value("CUSTOMER"));
    }

    @Test
    void verifyOtp_wrongCode_returns401() throws Exception {
        when(otpService.verify(any(), any()))
                .thenThrow(new InvalidOtpException("incorrect code; 4 attempt(s) left"));

        VerifyOtpRequest body = new VerifyOtpRequest(
                "+919876543210", "000000", Role.CUSTOMER, null);

        mockMvc.perform(post("/api/v1/auth/otp/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }
}

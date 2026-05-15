package com.photoconnect.auth.controller;

import com.photoconnect.auth.domain.User;
import com.photoconnect.auth.dto.AuthResponse;
import com.photoconnect.auth.dto.LoginRequest;
import com.photoconnect.auth.dto.RefreshRequest;
import com.photoconnect.auth.dto.RegisterRequest;
import com.photoconnect.auth.dto.UserDto;
import com.photoconnect.auth.exception.InvalidTokenException;
import com.photoconnect.auth.mapper.UserMapper;
import com.photoconnect.auth.repository.UserRepository;
import com.photoconnect.auth.security.JwtService;
import com.photoconnect.auth.security.UserPrincipal;
import com.photoconnect.auth.service.AuthService;
import io.jsonwebtoken.Claims;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@Tag(name = "auth", description = "Registration, login, token refresh, logout")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Operation(summary = "Register a new photographer or customer")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "User created; tokens returned"),
            @ApiResponse(responseCode = "400", description = "Validation failure"),
            @ApiResponse(responseCode = "409", description = "Email already registered")
    })
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest body) {
        return authService.register(body);
    }

    @Operation(summary = "Log in with email and password")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Authenticated; tokens returned"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials")
    })
    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest body) {
        return authService.login(body);
    }

    @Operation(summary = "Exchange a refresh token for a new access+refresh pair (rotation)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "New token pair issued"),
            @ApiResponse(responseCode = "401", description = "Refresh token invalid, expired, or already used")
    })
    @PostMapping("/refresh")
    public AuthResponse refresh(@Valid @RequestBody RefreshRequest body) {
        return authService.refresh(body);
    }

    @Operation(
            summary = "Revoke the current session (refresh token + access-token jti)",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponse(responseCode = "204", description = "Logged out")
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@AuthenticationPrincipal UserPrincipal principal,
                       HttpServletRequest request) {
        // We need the access token's expiry to size the Redis TTL — pull it
        // from the same Authorization header we already validated.
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith("Bearer ")) {
            throw new InvalidTokenException("missing bearer token");
        }
        Claims claims = jwtService.parseAndVerify(header.substring("Bearer ".length())).getPayload();
        Instant expiresAt = claims.getExpiration().toInstant();
        authService.logout(principal.userId(), principal.jti(), expiresAt);
    }

    @Operation(
            summary = "Return the current authenticated user's profile",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token")
    })
    @GetMapping("/me")
    public ResponseEntity<UserDto> me(@AuthenticationPrincipal UserPrincipal principal) {
        User user = userRepository.findById(principal.userId())
                .orElseThrow(() -> new InvalidTokenException("user no longer exists"));
        return ResponseEntity.ok(userMapper.toDto(user));
    }
}

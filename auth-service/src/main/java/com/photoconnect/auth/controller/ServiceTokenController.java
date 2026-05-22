package com.photoconnect.auth.controller;

import com.photoconnect.auth.dto.ServiceTokenRequest;
import com.photoconnect.auth.dto.ServiceTokenResponse;
import com.photoconnect.auth.service.ServiceTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint for service-to-service authentication. NOT meant to be reachable
 * through the api-gateway — internal callers hit auth-service directly via
 * Eureka. The gateway therefore does not need to (and should not) expose this
 * path to the public.
 *
 * <p>Request shape mirrors the OAuth2 client-credentials grant:</p>
 * <pre>{@code
 *   POST /api/v1/auth/service-token
 *   {
 *     "clientId":     "customer-service",
 *     "clientSecret": "..."
 *   }
 *
 *   200 OK
 *   {
 *     "accessToken": "eyJhbGciOi...",
 *     "expiresAt":   "2026-05-21T12:34:56Z",
 *     "scope":       "photographer-read"
 *   }
 * }</pre>
 */
@Tag(name = "service-auth", description = "Service-to-service token issuance (internal)")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class ServiceTokenController {

    private final ServiceTokenService serviceTokenService;

    @Operation(summary = "Mint a short-lived service-to-service JWT")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Service token issued"),
            @ApiResponse(responseCode = "401", description = "Invalid client credentials")
    })
    @PostMapping("/service-token")
    public ServiceTokenResponse issue(@Valid @RequestBody ServiceTokenRequest body) {
        return serviceTokenService.issue(body);
    }
}

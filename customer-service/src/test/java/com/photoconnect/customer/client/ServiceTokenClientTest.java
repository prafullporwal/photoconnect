package com.photoconnect.customer.client;

import com.photoconnect.customer.config.ServiceClientProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit test for {@link ServiceTokenClient}.
 *
 * <p>Spins up an in-process {@link MockWebServer} that pretends to be
 * auth-service and asserts:</p>
 * <ul>
 *   <li>the client mints a token, caches it, and reuses the cached value;</li>
 *   <li>when the cached token's refresh-skew window has elapsed, it mints again.</li>
 * </ul>
 */
class ServiceTokenClientTest {

    MockWebServer server;
    ServiceTokenClient client;

    @BeforeEach
    void start() throws IOException {
        server = new MockWebServer();
        server.start();
        ServiceClientProperties props = new ServiceClientProperties(
                "customer-service",
                "secret",
                server.url("/").toString(),
                Duration.ofSeconds(10));
        client = new ServiceTokenClient(props, RestClient.builder());
    }

    @AfterEach
    void stop() throws IOException {
        server.shutdown();
    }

    @Test
    void cachesTokenWithinRefreshWindow() throws Exception {
        Instant futureExpiry = Instant.now().plus(60, ChronoUnit.SECONDS);
        server.enqueue(jsonResponse(
                "{\"accessToken\":\"first-token\",\"expiresAt\":\"" + futureExpiry + "\",\"scope\":\"photographer-read\"}"));

        // Two calls should hit auth-service once.
        assertThat(client.getToken()).isEqualTo("first-token");
        assertThat(client.getToken()).isEqualTo("first-token");
        assertThat(server.getRequestCount()).isEqualTo(1);
    }

    @Test
    void refreshesWhenCachedTokenIsCloseToExpiry() throws Exception {
        // expiresAt is BEFORE now + refreshSkew → already in the refresh window → mint again on next call.
        Instant nearExpiry = Instant.now().plus(5, ChronoUnit.SECONDS); // skew is 10s
        server.enqueue(jsonResponse(
                "{\"accessToken\":\"first-token\",\"expiresAt\":\"" + nearExpiry + "\",\"scope\":\"photographer-read\"}"));
        Instant farExpiry = Instant.now().plus(60, ChronoUnit.SECONDS);
        server.enqueue(jsonResponse(
                "{\"accessToken\":\"second-token\",\"expiresAt\":\"" + farExpiry + "\",\"scope\":\"photographer-read\"}"));

        assertThat(client.getToken()).isEqualTo("first-token");
        // Second call: cached token's refreshAt has already passed → mint again.
        assertThat(client.getToken()).isEqualTo("second-token");
        assertThat(server.getRequestCount()).isEqualTo(2);
    }

    private MockResponse jsonResponse(String body) {
        return new MockResponse()
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(body);
    }
}

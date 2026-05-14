package com.photoconnect.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke + functional tests for the Config Server.
 *
 * <ul>
 *   <li>{@link #contextLoads()} confirms autoconfiguration is healthy.</li>
 *   <li>{@link #servesApplicationDefaults()} actually hits the Config Server's
 *       REST API and asserts that the shared {@code application.yml} from
 *       {@code config-repo/} is being served. This is the test that proves
 *       the native filesystem backend is wired correctly.</li>
 * </ul>
 *
 * <p>Note: We bind to a RANDOM port so the test can run alongside a real
 * Config Server already listening on 8888.</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("local")
class ConfigServiceApplicationTests {

    @LocalServerPort
    int port;

    @Test
    void contextLoads() {
        // Booting with @SpringBootTest is itself the assertion.
    }

    @Test
    void servesApplicationDefaults() {
        RestTemplate rest = new RestTemplateBuilder().build();
        // The "application" pseudo-name returns global defaults
        // (config-repo/application.yml). "default" is the requested profile.
        @SuppressWarnings("unchecked")
        Map<String, Object> body = rest.getForObject(
                "http://localhost:" + port + "/application/default", Map.class);

        assertThat(body).isNotNull();
        assertThat(body).containsKey("name");
        assertThat(body.get("name")).isEqualTo("application");
        // propertySources is the array of resolved sources Config Server
        // assembled for this request. There must be at least one — our
        // config-repo/application.yml.
        assertThat(body).containsKey("propertySources");
        assertThat((Iterable<?>) body.get("propertySources")).isNotEmpty();
    }
}

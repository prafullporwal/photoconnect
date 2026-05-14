package com.photoconnect.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;

/**
 * PhotoConnect — Spring Cloud Config Server entry point.
 *
 * <p>Acts as the single source of truth for configuration. Other services
 * fetch their config from this endpoint at startup (and can hot-refresh later
 * by POSTing to {@code /actuator/refresh}).</p>
 *
 * <p>The {@code @EnableConfigServer} annotation activates the Config Server
 * machinery: it adds REST endpoints under {@code /{application}/{profile}}
 * that return resolved configuration from the configured backend.</p>
 *
 * <p>Backends are selected by Spring profile:</p>
 * <ul>
 *   <li><b>local</b> — {@code native} backend pointing at the local
 *       {@code config-repo/} folder. Edit a YAML, hit refresh, and clients
 *       see the change.</li>
 *   <li><b>aws</b> — {@code git} backend (placeholder); Phase 2 will fill
 *       in the Git URL and AWS Secrets Manager integration.</li>
 * </ul>
 *
 * <p>Why no Eureka registration? Config Server is a <em>bootstrap dependency</em>
 * — clients need it before they're fully wired. Asking them to discover it
 * via Eureka means Eureka must be up first, which introduces a coupling we
 * don't need. A hardcoded URL keeps the boot order simple:
 * config-service → discovery-service → everything else.</p>
 */
@SpringBootApplication
@EnableConfigServer
public class ConfigServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConfigServiceApplication.class, args);
    }
}

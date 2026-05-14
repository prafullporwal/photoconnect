package com.photoconnect.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * PhotoConnect — API Gateway entry point.
 *
 * <p>This service is the single external entry point for all PhotoConnect
 * API traffic. It does four cross-cutting jobs that don't belong in any
 * specific backend service:</p>
 * <ol>
 *   <li><b>Routing</b> — match incoming paths and forward to the correct
 *       backend (load-balanced via Eureka with {@code lb://SERVICE-NAME}).</li>
 *   <li><b>Edge filters</b> — correlation IDs, CORS, JWT validation
 *       (JWT lands in Step 4), rate limiting (Phase 2).</li>
 *   <li><b>Observability</b> — every request becomes a traced span and is
 *       logged with a stable correlation ID.</li>
 *   <li><b>API aggregation</b> — single Swagger UI showing every downstream
 *       service's OpenAPI spec, fetched via the gateway itself.</li>
 * </ol>
 *
 * <p>Note: <em>no</em> {@code @EnableXxx} annotations here. Spring Cloud
 * Gateway, Config client, and Eureka client are all picked up by
 * auto-configuration as soon as their starters are on the classpath.</p>
 */
@SpringBootApplication
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}

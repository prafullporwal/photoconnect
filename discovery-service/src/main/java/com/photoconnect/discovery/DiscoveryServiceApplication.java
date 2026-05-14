package com.photoconnect.discovery;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

/**
 * PhotoConnect — Eureka Server entry point.
 *
 * <p>The {@code @EnableEurekaServer} annotation flips this Boot app from a
 * regular web service into a service registry. Once running, it accepts
 * registration requests from Eureka clients (every other PhotoConnect service)
 * and serves a dashboard at <a href="http://localhost:8761">http://localhost:8761</a>.</p>
 *
 * <p>Architectural notes:</p>
 * <ul>
 *   <li>The server itself does <em>not</em> register with another Eureka — it is
 *       the source of truth. The {@code eureka.client.register-with-eureka=false}
 *       and {@code fetch-registry=false} settings in application.yml enforce that.</li>
 *   <li>In production you would run two or three Eureka instances peering with
 *       each other for HA. For MVP we run a single node — fine on a laptop.</li>
 *   <li>Security: Eureka is intentionally open on the internal network for MVP.
 *       In Phase 2 (EKS) it lives in a private subnet/namespace; ingress is
 *       blocked at the network boundary.</li>
 * </ul>
 */
@SpringBootApplication
@EnableEurekaServer
public class DiscoveryServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(DiscoveryServiceApplication.class, args);
    }
}

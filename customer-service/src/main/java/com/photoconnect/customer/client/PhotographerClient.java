package com.photoconnect.customer.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

/**
 * Declarative HTTP client for photographer-service.
 *
 * <h2>How this works</h2>
 * <p>Spring Cloud OpenFeign scans for {@code @FeignClient} interfaces at
 * startup and generates a proxy that implements the interface. Each method
 * call becomes:</p>
 * <ol>
 *   <li>Resolve {@code "photographer-service"} → instance URL via Eureka /
 *       Spring Cloud LoadBalancer (round-robin across registered instances).</li>
 *   <li>Build an HTTP request from the Spring MVC annotations
 *       ({@code @GetMapping}, {@code @PathVariable}, etc).</li>
 *   <li>Execute, retry on 5xx (Feign default = 5 attempts), and deserialize
 *       the JSON response into the return type.</li>
 * </ol>
 *
 * <p>The endpoint we hit is {@code GET /api/v1/photographers/{id}} which is
 * a <em>public</em> path on photographer-service — no JWT needed for this
 * internal call. (Cross-service auth is a Phase 2 topic.)</p>
 *
 * <p>If photographer-service is down or returns 4xx/5xx, Feign throws a
 * {@code FeignException} subclass which {@link
 * com.photoconnect.customer.service.InquiryService} catches and translates
 * into a domain exception.</p>
 */
@FeignClient(name = "photographer-service")
public interface PhotographerClient {

    /**
     * Fetch a photographer profile by its PK. Returns the slim
     * {@link PhotographerSummary} — Jackson ignores any extra fields in the
     * full response.
     */
    @GetMapping("/api/v1/photographers/{id}")
    PhotographerSummary getPhotographer(@PathVariable("id") UUID id);
}

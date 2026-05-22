package com.photoconnect.reviews.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

/**
 * Feign client to photographer-service.
 *
 * <p>Hits the same internal endpoint as customer-service does — we only need
 * to confirm the photographer exists and capture their {@code userId} for
 * denormalisation onto the review row.</p>
 */
@FeignClient(name = "photographer-service")
public interface PhotographerClient {

    @GetMapping("/internal/v1/photographers/{id}")
    PhotographerSummary getPhotographer(@PathVariable("id") UUID id);
}

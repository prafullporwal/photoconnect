package com.photoconnect.reviews.client;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

/**
 * Adds {@code Authorization: Bearer &lt;service-token&gt;} to every outbound
 * Feign request. Spring Cloud OpenFeign auto-detects this bean and applies it
 * to every {@code @FeignClient} in this service.
 */
@Component
@RequiredArgsConstructor
public class ServiceTokenFeignInterceptor implements RequestInterceptor {

    private final ServiceTokenClient tokenClient;

    @Override
    public void apply(RequestTemplate template) {
        template.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenClient.getToken());
    }
}

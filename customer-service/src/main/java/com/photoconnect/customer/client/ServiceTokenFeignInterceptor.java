package com.photoconnect.customer.client;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

/**
 * Adds {@code Authorization: Bearer &lt;service-token&gt;} to every outbound
 * Feign request.
 *
 * <p>Auto-detected by Spring Cloud OpenFeign because it's a Spring bean
 * implementing {@link RequestInterceptor} — no extra configuration is
 * required. Applies to ALL {@code @FeignClient} beans in this service;
 * if we later add Feign clients that should NOT carry a service token,
 * scope this interceptor to a specific client via
 * {@code @FeignClient(configuration = ...)}.</p>
 *
 * <p>If {@link ServiceTokenClient#getToken()} throws (auth-service is down,
 * credentials are wrong), the exception propagates and Feign reports the
 * failure to the caller — exactly what we want: a 5xx on the inbound request
 * rather than a silent fall-through to an anonymous downstream call.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ServiceTokenFeignInterceptor implements RequestInterceptor {

    private final ServiceTokenClient tokenClient;

    @Override
    public void apply(RequestTemplate template) {
        template.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenClient.getToken());
    }
}

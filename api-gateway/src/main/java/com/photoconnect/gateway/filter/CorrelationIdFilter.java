package com.photoconnect.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Global filter that ensures every request entering the platform carries an
 * {@code X-Correlation-Id} header.
 *
 * <p><b>Behaviour:</b></p>
 * <ul>
 *   <li>If the incoming request already has {@code X-Correlation-Id}, we
 *       trust it (common pattern: a client correlates multiple user actions
 *       under one ID, or an upstream gateway has already set one).</li>
 *   <li>If absent or blank, we generate a fresh UUID.</li>
 *   <li>We mutate the request so downstream services see the header.</li>
 *   <li>We set the same header on the response so the client can correlate
 *       its own logs with ours.</li>
 * </ul>
 *
 * <p><b>Why a SEPARATE correlation ID when we already have W3C trace IDs?</b><br>
 * Trace IDs change per request span; a correlation ID can span <em>multiple
 * requests</em> initiated by the same user action (e.g. an SPA firing 5
 * parallel calls during checkout). Operators want to find "everything from
 * that one click" — the correlation ID is the human-friendly handle.</p>
 *
 * <p><b>Why {@link Ordered#HIGHEST_PRECEDENCE}?</b><br>
 * We want the ID established before any other filter runs, so logs from
 * later filters (JWT, rate-limit, …) all carry it.</p>
 *
 * <p><b>MDC propagation in WebFlux:</b><br>
 * The gateway runs on Reactor threads; classic ThreadLocal MDC doesn't work
 * here. Downstream services (Servlet stack) WILL put the header into their
 * MDC via a ServletFilter we'll add per service. At the gateway itself,
 * Micrometer Tracing's traceId is already in the log pattern; the
 * correlation ID rides in the header. That's enough for end-to-end debug.</p>
 */
@Component
public class CorrelationIdFilter implements GlobalFilter, Ordered {

    public static final String HEADER = "X-Correlation-Id";

    private static final Logger log = LoggerFactory.getLogger(CorrelationIdFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String correlationId = exchange.getRequest().getHeaders().getFirst(HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
            if (log.isDebugEnabled()) {
                log.debug("Generated correlation id {} for {} {}",
                        correlationId,
                        exchange.getRequest().getMethod(),
                        exchange.getRequest().getURI().getRawPath());
            }
        }
        final String cid = correlationId;

        // Add header to the downstream request
        ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                .header(HEADER, cid)
                .build();

        // Echo header back on the response so the client can pin its logs to ours
        exchange.getResponse().getHeaders().set(HEADER, cid);

        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}

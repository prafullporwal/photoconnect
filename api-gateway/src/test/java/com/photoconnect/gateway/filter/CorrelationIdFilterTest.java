package com.photoconnect.gateway.filter;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link CorrelationIdFilter}. Pure Mockito-free: just spin
 * up a real filter instance and a mock exchange — fast, no Spring context.
 */
class CorrelationIdFilterTest {

    private static final Pattern UUID_PATTERN = Pattern.compile(
            "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}",
            Pattern.CASE_INSENSITIVE);

    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @Test
    void generatesUuidWhenHeaderIsMissing() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/auth/login").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        AtomicReference<String> downstreamHeader = new AtomicReference<>();

        GatewayFilterChain chain = ex -> {
            downstreamHeader.set(ex.getRequest().getHeaders().getFirst(CorrelationIdFilter.HEADER));
            return Mono.empty();
        };

        filter.filter(exchange, chain).block();

        assertThat(downstreamHeader.get())
                .as("downstream request must carry a generated correlation id")
                .isNotNull()
                .matches(UUID_PATTERN);
        assertThat(exchange.getResponse().getHeaders().getFirst(CorrelationIdFilter.HEADER))
                .as("response must echo the same id back to the client")
                .isEqualTo(downstreamHeader.get());
    }

    @Test
    void preservesProvidedCorrelationId() {
        String supplied = "client-trace-9f2a3";
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/auth/login")
                .header(CorrelationIdFilter.HEADER, supplied)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        AtomicReference<String> downstreamHeader = new AtomicReference<>();

        GatewayFilterChain chain = ex -> {
            downstreamHeader.set(ex.getRequest().getHeaders().getFirst(CorrelationIdFilter.HEADER));
            return Mono.empty();
        };

        filter.filter(exchange, chain).block();

        assertThat(downstreamHeader.get())
                .as("must trust client-supplied correlation id")
                .isEqualTo(supplied);
        assertThat(exchange.getResponse().getHeaders().getFirst(CorrelationIdFilter.HEADER))
                .isEqualTo(supplied);
    }

    @Test
    void generatesUuidWhenHeaderIsBlank() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/whatever")
                .header(CorrelationIdFilter.HEADER, "   ")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        AtomicReference<String> downstreamHeader = new AtomicReference<>();

        GatewayFilterChain chain = ex -> {
            downstreamHeader.set(ex.getRequest().getHeaders().getFirst(CorrelationIdFilter.HEADER));
            return Mono.empty();
        };

        filter.filter(exchange, chain).block();

        assertThat(downstreamHeader.get()).matches(UUID_PATTERN);
    }
}

package com.photoconnect.auth.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * Wires the {@link RestClient} used by {@link com.photoconnect.auth.service.Msg91OtpDeliveryService}.
 *
 * <p>The whole class is only loaded when {@code app.otp.provider=msg91}, so
 * a dev-mode deployment doesn't need MSG91 credentials and doesn't pay the
 * cost of constructing an HTTP client it'll never use.</p>
 *
 * <p>{@link SimpleClientHttpRequestFactory} is sufficient for a single low-rate
 * outbound endpoint. For higher concurrency we'd switch to Apache HttpClient 5
 * or JDK 11+ HttpClient with a connection pool — both compose with RestClient
 * via {@code requestFactory(...)} with no code changes elsewhere.</p>
 */
@Configuration
@ConditionalOnProperty(name = "app.otp.provider", havingValue = "msg91")
@EnableConfigurationProperties(Msg91Properties.class)
public class OtpDeliveryConfig {

    @Bean("msg91RestClient")
    public RestClient msg91RestClient(Msg91Properties props) {
        // Connect timeout = how long to wait for TCP handshake.
        // Read timeout    = how long to wait for the response after the request is sent.
        // 2s + 3s is generous enough for cross-region MSG91 hops but bounded so a
        // stuck SMS gateway doesn't tie up our request thread for minutes.
        SimpleClientHttpRequestFactory rf = new SimpleClientHttpRequestFactory();
        rf.setConnectTimeout((int) Duration.ofSeconds(2).toMillis());
        rf.setReadTimeout((int) Duration.ofSeconds(3).toMillis());

        return RestClient.builder()
                .baseUrl(props.baseUrl())
                .defaultHeader("authkey", props.authKey())
                .defaultHeader("Content-Type", "application/json")
                .requestFactory(rf)
                .build();
    }
}

package com.photoconnect.customer;

import com.photoconnect.customer.config.ServiceClientProperties;
import com.photoconnect.customer.config.ServiceJwtProperties;
import com.photoconnect.customer.security.PemKeyLoader;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestClient;

import java.nio.file.Path;
import java.security.interfaces.RSAPublicKey;

/**
 * PhotoConnect — Customer Service entry point.
 *
 * <p>Owns customer profiles and inquiries. Uses MySQL (a different engine than
 * auth/photographer on purpose: each service picks its own storage).</p>
 *
 * <p>{@code @EnableFeignClients} scans the {@code client} package for
 * {@code @FeignClient}-annotated interfaces and generates proxies at startup.
 * Each proxy resolves its target service name via Eureka and load-balances
 * requests across instances.</p>
 */
@SpringBootApplication
@EnableFeignClients(basePackages = "com.photoconnect.customer.client")
@EnableConfigurationProperties({ServiceClientProperties.class, ServiceJwtProperties.class})
public class CustomerServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CustomerServiceApplication.class, args);
    }

    /**
     * Load-balanced {@link RestClient} builder for outbound HTTP calls that
     * need Eureka-based service resolution — specifically the call from
     * {@link com.photoconnect.customer.client.ServiceTokenClient} to
     * {@code http://auth-service/api/v1/auth/service-token}.
     */
    @Bean
    @LoadBalanced
    public RestClient.Builder loadBalancedRestClientBuilder() {
        return RestClient.builder();
    }

    /**
     * Public key used by {@code ServiceTokenAuthenticationFilter} to verify
     * service-to-service JWTs on {@code /internal/**} endpoints. Loaded once at
     * startup; RSA keys don't rotate at runtime in MVP.
     */
    @Bean
    public RSAPublicKey serviceJwtPublicKey(ServiceJwtProperties properties) throws Exception {
        return PemKeyLoader.loadPublicKey(Path.of(properties.publicKeyPath()));
    }
}

package com.photoconnect.reviews;

import com.photoconnect.reviews.config.ServiceClientProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestClient;

/**
 * PhotoConnect — Reviews Service entry point.
 *
 * <p>Owns customer-authored reviews of photographers. Persists to PostgreSQL
 * ({@code reviews_db}). Two outbound Feign clients sit in the
 * {@code .client} package:</p>
 * <ul>
 *   <li>{@code InquiryClient} → customer-service: verifies a completed
 *       engagement exists before allowing a review.</li>
 *   <li>{@code PhotographerClient} → photographer-service: validates the
 *       photographer-profile id and captures the photographer's userId.</li>
 * </ul>
 *
 * <p>Outbound calls carry a service-to-service JWT minted by auth-service.
 * The mint+cache logic lives in {@code ServiceTokenClient}; the JWT is stamped
 * onto every Feign call by {@code ServiceTokenFeignInterceptor}. Identical
 * pattern to customer-service.</p>
 */
@SpringBootApplication
@EnableFeignClients(basePackages = "com.photoconnect.reviews.client")
@EnableConfigurationProperties(ServiceClientProperties.class)
public class ReviewsServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReviewsServiceApplication.class, args);
    }

    /**
     * Load-balanced {@link RestClient} builder so {@code ServiceTokenClient}
     * can resolve {@code http://auth-service} via Eureka instead of needing a
     * hard-coded host.
     */
    @Bean
    @LoadBalanced
    public RestClient.Builder loadBalancedRestClientBuilder() {
        return RestClient.builder();
    }
}

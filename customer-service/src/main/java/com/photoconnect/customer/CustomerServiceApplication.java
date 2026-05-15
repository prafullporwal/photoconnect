package com.photoconnect.customer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

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
public class CustomerServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CustomerServiceApplication.class, args);
    }
}

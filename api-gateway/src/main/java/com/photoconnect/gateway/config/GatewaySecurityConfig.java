package com.photoconnect.gateway.config;

import com.photoconnect.gateway.security.PemKeyLoader;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;
import java.security.interfaces.RSAPublicKey;

/**
 * Wires the JWT properties record with the PEM key loader and exposes the
 * public key as a Spring bean.
 *
 * <h2>Why separate key-loading from the filter?</h2>
 * <p>When key-loading lives in a {@code @PostConstruct} inside the filter,
 * unit tests that construct the filter directly have no clean way to supply a
 * test key without touching the filesystem. By loading the key in a
 * {@code @Bean} method here, the filter declares {@code RSAPublicKey} as a
 * constructor dependency and tests simply pass a freshly generated key.</p>
 *
 * <p>Any failure (missing file, malformed PEM) surfaces immediately as a
 * context-start error — a loud, early failure rather than a silent 500 at
 * request time.</p>
 */
@Configuration
@EnableConfigurationProperties(JwtGatewayProperties.class)
public class GatewaySecurityConfig {

    /**
     * Load the RSA-2048 public key once at startup.
     * The key never changes at runtime; a restart is required to rotate it.
     */
    @Bean
    RSAPublicKey jwtPublicKey(JwtGatewayProperties properties) throws Exception {
        return PemKeyLoader.loadPublicKey(Path.of(properties.publicKeyPath()));
    }
}

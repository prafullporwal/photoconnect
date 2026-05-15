package com.photoconnect.photographer.storage;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

import java.net.URI;

/**
 * Builds the {@link S3Client} that photographer-service uses to talk to either
 * MinIO (local) or real S3 (Phase 2). Identical API both ways — the swap is a
 * properties change in config-repo, not a code change.
 *
 * <p>Three knobs distinguish MinIO from real S3:</p>
 * <ol>
 *   <li><b>Endpoint override</b> — point at {@code localhost:9000} instead of the
 *       AWS region URL.</li>
 *   <li><b>Path-style access</b> — MinIO uses {@code /bucket/key}; AWS prefers
 *       {@code bucket.host/key}.</li>
 *   <li><b>Static credentials</b> — local MinIO admin creds vs. real IAM creds
 *       (loaded via the default chain in Phase 2).</li>
 * </ol>
 */
@Configuration
@EnableConfigurationProperties(StorageProperties.class)
public class StorageConfig {

    @Bean
    public S3Client s3Client(StorageProperties props) {
        return S3Client.builder()
                .endpointOverride(URI.create(props.endpoint()))
                .region(Region.of(props.region()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(props.accessKey(), props.secretKey())))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(props.pathStyleAccess())
                        .build())
                .build();
    }
}

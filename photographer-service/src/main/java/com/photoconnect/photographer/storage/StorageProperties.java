package com.photoconnect.photographer.storage;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Typed config for object storage. Same shape works for MinIO (local) and
 * real S3 (Phase 2) — only values change.
 *
 * <p><b>endpoint</b> — full S3 API URL. {@code http://localhost:9000} for MinIO,
 * blank for real S3 (the AWS SDK picks the regional endpoint from {@code region}).</p>
 *
 * <p><b>publicUrlPrefix</b> — base URL the SPA fetches assets from. Locally
 * this is also MinIO's :9000. In production it could be a CloudFront
 * distribution sitting in front of S3, completely independent of the upload
 * endpoint.</p>
 *
 * <p><b>pathStyleAccess</b> — MinIO insists on path-style ({@code host/bucket/key}),
 * S3's modern default is virtual-host style ({@code bucket.host/key}). We let
 * this be a per-environment toggle.</p>
 */
@Validated
@ConfigurationProperties(prefix = "storage")
public record StorageProperties(
        @NotBlank String endpoint,
        @NotBlank String publicUrlPrefix,
        @NotBlank String bucket,
        @NotBlank String region,
        @NotBlank String accessKey,
        @NotBlank String secretKey,
        boolean pathStyleAccess
) {}

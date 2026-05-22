package com.photoconnect.auth.security;

import com.photoconnect.auth.config.JwtProperties;
import com.photoconnect.auth.testutil.TestKeyPair;

import java.nio.file.Path;
import java.time.Duration;

/**
 * Lives in the same package as {@link JwtService} so it can call its
 * package-private {@code loadKeys()}. Lets tests outside this package
 * construct a ready-to-use {@code JwtService} without exposing the
 * key-loading API to production callers.
 */
public final class TestJwtServiceFactory {

    private TestJwtServiceFactory() {}

    /** Build a {@link JwtService} backed by a fresh RSA-2048 key pair in {@code dir}. */
    public static JwtService createWithFreshKeys(Path dir) throws Exception {
        TestKeyPair.Paths paths = TestKeyPair.writeNew(dir);
        JwtProperties props = new JwtProperties(
                "photoconnect-test",
                "photoconnect-api-test",
                Duration.ofMinutes(15),
                Duration.ofDays(7),
                paths.privateKey().toString(),
                paths.publicKey().toString());
        JwtService service = new JwtService(props);
        service.loadKeys();
        return service;
    }
}

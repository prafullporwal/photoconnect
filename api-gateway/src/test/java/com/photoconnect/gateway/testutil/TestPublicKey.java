package com.photoconnect.gateway.testutil;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;

/**
 * Generates a fresh RSA-2048 key pair for gateway tests.
 *
 * <p><b>Two use-cases:</b></p>
 * <ol>
 *   <li><b>Unit tests</b> that construct {@link
 *       com.photoconnect.gateway.filter.JwtValidationFilter} directly can call
 *       {@link #publicKey()} and {@link #privateKey()} without any file I/O.</li>
 *   <li><b>Context tests</b> ({@code @SpringBootTest}) that need
 *       {@code gateway.jwt.public-key-path} to point at a real file use
 *       {@link #publicKeyPath()} in a {@code @DynamicPropertySource} method.</li>
 * </ol>
 *
 * <p>The static initializer runs once per JVM and is shared across all tests
 * in the same Maven Surefire fork, so key generation only happens once.</p>
 */
public final class TestPublicKey {

    private static final KeyPair KEY_PAIR;
    private static final Path PUBLIC_KEY_PATH;

    static {
        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(2048);
            KEY_PAIR = kpg.generateKeyPair();

            // Write the public key as a PEM file so @DynamicPropertySource tests can
            // reference it via a real filesystem path.
            byte[] encoded = KEY_PAIR.getPublic().getEncoded();
            String pem = "-----BEGIN PUBLIC KEY-----\n"
                    + Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(encoded)
                    + "\n-----END PUBLIC KEY-----\n";

            PUBLIC_KEY_PATH = Files.createTempFile("gateway-test-pub-", ".pem");
            Files.writeString(PUBLIC_KEY_PATH, pem);
            PUBLIC_KEY_PATH.toFile().deleteOnExit();

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate test RSA key pair for gateway tests", e);
        }
    }

    private TestPublicKey() {}

    /** The RSA-2048 public key for verifying test tokens. */
    public static RSAPublicKey publicKey() {
        return (RSAPublicKey) KEY_PAIR.getPublic();
    }

    /**
     * The private key used only in tests to sign tokens.
     * It never leaves the test JVM.
     */
    public static PrivateKey privateKey() {
        return KEY_PAIR.getPrivate();
    }

    /** Path to the temp PEM file containing the test public key. */
    public static Path publicKeyPath() {
        return PUBLIC_KEY_PATH;
    }
}

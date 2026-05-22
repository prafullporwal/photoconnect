package com.photoconnect.photographer.testutil;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;

/**
 * Test-only helper: writes a fresh RSA-2048 public key to a temp file so the
 * {@code ServiceJwtProperties.public-key-path} property has a real target
 * during context-load smoke tests. We don't need the private key here —
 * photographer-service only verifies signatures, never signs.
 *
 * <p>Mirrors auth-service's same-named test helper. Each module has its own
 * copy on purpose: avoids inventing a shared {@code test-support} jar for
 * a 30-line helper.</p>
 */
public final class TestKeyPair {

    private TestKeyPair() {}

    public static Path writePublicKey(Path dir) throws Exception {
        Files.createDirectories(dir);
        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048);
        KeyPair kp = gen.generateKeyPair();

        Path pub = dir.resolve("test_public_key.pem");
        Files.writeString(pub, toPem("PUBLIC KEY", kp.getPublic().getEncoded()));
        return pub;
    }

    private static String toPem(String type, byte[] der) {
        String b64 = Base64.getMimeEncoder(64, new byte[]{'\n'}).encodeToString(der);
        return "-----BEGIN " + type + "-----\n" + b64 + "\n-----END " + type + "-----\n";
    }
}

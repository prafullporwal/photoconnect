package com.photoconnect.auth.testutil;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;

/**
 * Generates a fresh RSA-2048 key pair into {@link java.nio.file.Path}
 * locations for use in tests. Avoids checking in real-looking key material.
 */
public final class TestKeyPair {

    private TestKeyPair() {}

    public static Paths writeNew(Path dir) throws Exception {
        Files.createDirectories(dir);
        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048);
        KeyPair kp = gen.generateKeyPair();

        Path priv = dir.resolve("test_private_key.pem");
        Path pub  = dir.resolve("test_public_key.pem");
        Files.writeString(priv, toPem("PRIVATE KEY", kp.getPrivate().getEncoded()));
        Files.writeString(pub,  toPem("PUBLIC KEY",  kp.getPublic().getEncoded()));
        return new Paths(priv, pub);
    }

    private static String toPem(String type, byte[] der) {
        String b64 = Base64.getMimeEncoder(64, new byte[]{'\n'}).encodeToString(der);
        return "-----BEGIN " + type + "-----\n" + b64 + "\n-----END " + type + "-----\n";
    }

    public record Paths(Path privateKey, Path publicKey) {}
}

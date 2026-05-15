package com.photoconnect.gateway.security;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * Loads a PEM-encoded RSA public key from the filesystem.
 *
 * <p>Expected format (standard {@code openssl rsa -pubout} / {@code openssl genpkey} output):</p>
 * <pre>
 *   -----BEGIN PUBLIC KEY-----    (X.509 SubjectPublicKeyInfo)
 *   ...base64...
 *   -----END PUBLIC KEY-----
 * </pre>
 *
 * <p>The gateway only <em>verifies</em> tokens, so only the public key loader
 * is provided here. The private key never leaves auth-service. This class is
 * intentionally identical in structure to
 * {@code com.photoconnect.auth.security.PemKeyLoader} — in Phase 2 both
 * could be extracted to a shared library if the number of services justifies
 * it, but for now duplication keeps each service self-contained.</p>
 */
public final class PemKeyLoader {

    private PemKeyLoader() {}

    /**
     * Parse an RSA public key from a PEM file.
     *
     * @param pemFile path to the {@code -----BEGIN PUBLIC KEY-----} PEM file
     * @return the parsed {@link RSAPublicKey}
     * @throws IOException            if the file cannot be read
     * @throws NoSuchAlgorithmException if the JVM lacks RSA (shouldn't happen)
     * @throws InvalidKeySpecException  if the PEM content is not a valid public key
     */
    public static RSAPublicKey loadPublicKey(Path pemFile)
            throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        byte[] der = stripAndDecode(
                Files.readString(pemFile),
                "-----BEGIN PUBLIC KEY-----",
                "-----END PUBLIC KEY-----");
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return (RSAPublicKey) kf.generatePublic(new X509EncodedKeySpec(der));
    }

    private static byte[] stripAndDecode(String pem, String beginMarker, String endMarker) {
        String stripped = pem
                .replace(beginMarker, "")
                .replace(endMarker, "")
                .replaceAll("\\s+", "");
        return Base64.getDecoder().decode(stripped);
    }
}

package com.photoconnect.auth.security;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * Tiny utility for loading PEM-encoded RSA keys from the filesystem.
 *
 * <p>Expected formats (default {@code openssl genpkey} output):</p>
 * <pre>
 *   -----BEGIN PRIVATE KEY-----   (PKCS#8 unencrypted)
 *   ...base64...
 *   -----END PRIVATE KEY-----
 *
 *   -----BEGIN PUBLIC KEY-----    (X.509 SubjectPublicKeyInfo)
 *   ...base64...
 *   -----END PUBLIC KEY-----
 * </pre>
 *
 * <p>In Phase 2 these readers are replaced by a {@code SecretsManagerKeyLoader}
 * that pulls the same PEMs from AWS Secrets Manager. The signature of the
 * methods stays the same, so {@code JwtService} doesn't change.</p>
 */
public final class PemKeyLoader {

    private PemKeyLoader() {}

    public static RSAPrivateKey loadPrivateKey(Path pemFile) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        byte[] der = stripAndDecode(
                Files.readString(pemFile),
                "-----BEGIN PRIVATE KEY-----",
                "-----END PRIVATE KEY-----");
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return (RSAPrivateKey) kf.generatePrivate(new PKCS8EncodedKeySpec(der));
    }

    public static RSAPublicKey loadPublicKey(Path pemFile) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
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

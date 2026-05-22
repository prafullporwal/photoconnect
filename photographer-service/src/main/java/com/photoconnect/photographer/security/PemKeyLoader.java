package com.photoconnect.photographer.security;

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
 * Loads a PEM-encoded RSA public key from disk. This service only verifies
 * signatures (never signs), so we don't have a private-key counterpart.
 *
 * <p>Mirrors the loader in auth-service and api-gateway — same file format,
 * different package, kept duplicated on purpose: each service owns its
 * verification setup so there are no shared-library version traps.</p>
 */
public final class PemKeyLoader {

    private PemKeyLoader() {}

    public static RSAPublicKey loadPublicKey(Path pemFile)
            throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        String pem = Files.readString(pemFile);
        String stripped = pem
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s+", "");
        byte[] der = Base64.getDecoder().decode(stripped);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return (RSAPublicKey) kf.generatePublic(new X509EncodedKeySpec(der));
    }
}

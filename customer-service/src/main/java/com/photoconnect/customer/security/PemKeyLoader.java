package com.photoconnect.customer.security;

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
 * Loads a PEM-encoded RSA public key from disk. Identical to the variant in
 * auth-service, api-gateway, and photographer-service — kept duplicated on
 * purpose so each service owns its verification setup without sharing a jar.
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

package com.eventrelay.serviceb.security;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import javax.crypto.*;
import javax.crypto.spec.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SigningSecrets {
    private final SecretKeySpec masterKey;
    public SigningSecrets(@Value("${app.signing.encryption-key:}") String encoded,
                          @Value("${app.signing.key-file:../service-a/.local/signing.key}") String path) {
        try {
            byte[] key = Base64.getDecoder().decode(encoded.isBlank() ? Files.readString(Path.of(path)).strip() : encoded);
            if (key.length != 32) throw new IllegalArgumentException("Expected 32-byte encryption key");
            masterKey = new SecretKeySpec(key, "AES");
        } catch (Exception ex) {
            throw new IllegalStateException("Cannot load signing master key. Start Service A first or configure the same SIGNING_SECRET_ENCRYPTION_KEY / SIGNING_SECRET_KEY_FILE for both services.", ex);
        }
    }
    public String signature(String encrypted, UUID endpointId, UUID deliveryId, long timestamp, String body) {
        try {
            byte[] envelope = Base64.getDecoder().decode(encrypted);
            if (envelope.length < 29) throw new IllegalArgumentException("Invalid signing ciphertext");
            var cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, masterKey, new GCMParameterSpec(128, Arrays.copyOf(envelope, 12)));
            cipher.updateAAD(endpointId.toString().getBytes(StandardCharsets.UTF_8));
            String secret = new String(cipher.doFinal(Arrays.copyOfRange(envelope, 12, envelope.length)), StandardCharsets.UTF_8);
            if (!secret.startsWith("whsec_")) throw new IllegalArgumentException("Invalid signing secret format");
            byte[] rawKey = Base64.getUrlDecoder().decode(secret.substring(6));
            if (rawKey.length != 32) throw new IllegalArgumentException("Invalid signing secret size");
            var mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(rawKey, "HmacSHA256"));
            String signed = deliveryId + "." + timestamp + "." + body;
            return "v1," + Base64.getEncoder().encodeToString(mac.doFinal(signed.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) { throw new IllegalStateException("Signing secret is unavailable or invalid", ex); }
    }
}

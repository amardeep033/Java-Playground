package com.eventrelay.servicea.security;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermissions;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.*;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SecretEncryption {
    private final SecretKeySpec key;
    private final SecureRandom random = new SecureRandom();

    public SecretEncryption(@Value("${app.signing.encryption-key:}") String encodedKey,
                            @Value("${app.signing.key-file:.local/signing.key}") String keyFile) {
        try {
            byte[] bytes = Base64.getDecoder().decode(encodedKey.isBlank() ? localKey(Path.of(keyFile)) : encodedKey);
            if (bytes.length != 32) throw new IllegalArgumentException("Signing encryption key must contain 32 bytes");
            key = new SecretKeySpec(bytes, "AES");
        } catch (IOException | IllegalArgumentException ex) {
            throw new IllegalStateException("Cannot load signing encryption key; check app.signing key configuration", ex);
        }
    }

    public String generateSecret() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return "whsec_" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public String encrypt(String secret, UUID endpointId) {
        try {
            byte[] nonce = new byte[12];
            random.nextBytes(nonce);
            Cipher cipher = cipher(Cipher.ENCRYPT_MODE, nonce, endpointId);
            byte[] encrypted = cipher.doFinal(secret.getBytes(StandardCharsets.UTF_8));
            byte[] envelope = new byte[nonce.length + encrypted.length];
            System.arraycopy(nonce, 0, envelope, 0, nonce.length);
            System.arraycopy(encrypted, 0, envelope, nonce.length, encrypted.length);
            return Base64.getEncoder().encodeToString(envelope);
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Cannot encrypt signing secret", ex);
        }
    }

    // Internal use by a future delivery implementation. There is no reveal API.
    public String decrypt(String encrypted, UUID endpointId) {
        try {
            byte[] envelope = Base64.getDecoder().decode(encrypted);
            if (envelope.length < 29) throw new IllegalArgumentException("Invalid encrypted secret");
            Cipher cipher = cipher(Cipher.DECRYPT_MODE, Arrays.copyOf(envelope, 12), endpointId);
            return new String(cipher.doFinal(Arrays.copyOfRange(envelope, 12, envelope.length)), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException ex) {
            throw new IllegalStateException("Cannot decrypt signing secret", ex);
        }
    }

    private Cipher cipher(int mode, byte[] nonce, UUID endpointId) throws GeneralSecurityException {
        var cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(mode, key, new GCMParameterSpec(128, nonce));
        cipher.updateAAD(endpointId.toString().getBytes(StandardCharsets.UTF_8));
        return cipher;
    }

    private String localKey(Path configured) throws IOException {
        Path path = configured.toAbsolutePath();
        Files.createDirectories(path.getParent(), PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------")));
        if (!Files.exists(path)) {
            Path temporary = Files.createTempFile(path.getParent(), ".signing-", ".tmp",
                    PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")));
            try {
                byte[] bytes = new byte[32];
                random.nextBytes(bytes);
                Files.writeString(temporary, Base64.getEncoder().encodeToString(bytes));
                try { Files.move(temporary, path); } catch (FileAlreadyExistsException ignored) { /* Another process initialized it. */ }
            } finally { Files.deleteIfExists(temporary); }
        }
        return Files.readString(path).strip();
    }
}

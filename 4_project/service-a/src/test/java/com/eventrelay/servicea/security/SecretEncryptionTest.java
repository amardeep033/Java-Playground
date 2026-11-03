package com.eventrelay.servicea.security;

import java.nio.file.Path;
import java.util.Base64;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class SecretEncryptionTest {
    @TempDir Path directory;

    @Test
    void ciphertextUsesFreshNoncesAndIsBoundToAnEndpoint() {
        var encryption = new SecretEncryption(Base64.getEncoder().encodeToString(new byte[32]), "unused");
        UUID endpointId = UUID.randomUUID();
        String secret = encryption.generateSecret();
        assertTrue(secret.matches("whsec_[A-Za-z0-9_-]{43}"));
        String first = encryption.encrypt(secret, endpointId);
        String second = encryption.encrypt(secret, endpointId);
        assertNotEquals(first, second);
        assertFalse(first.contains(secret));
        assertEquals(secret, encryption.decrypt(first, endpointId));
        assertThrows(IllegalStateException.class, () -> encryption.decrypt(first, UUID.randomUUID()));
        assertThrows(IllegalStateException.class, () -> encryption.decrypt("tampered", endpointId));
    }

    @Test
    void localMasterKeySurvivesRestart() {
        String path = directory.resolve("signing.key").toString();
        var first = new SecretEncryption("", path);
        UUID endpointId = UUID.randomUUID();
        String encrypted = first.encrypt("whsec_test", endpointId);
        var restarted = new SecretEncryption("", path);
        assertEquals("whsec_test", restarted.decrypt(encrypted, endpointId));
    }
}

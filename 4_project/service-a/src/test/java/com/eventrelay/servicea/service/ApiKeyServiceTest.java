package com.eventrelay.servicea.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import com.eventrelay.servicea.entity.ApiKey;
import com.eventrelay.servicea.repository.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApiKeyServiceTest {
    @Mock ApiKeyRepository keys;
    @Mock ResourceAccess access;
    ApiKeyService service;
    UUID appId = UUID.randomUUID();
    UUID keyId = UUID.randomUUID();

    @BeforeEach
    void setUp() { service = new ApiKeyService(keys, access,mock(com.eventrelay.servicea.workspace.AuditLogs.class)); }

    @Test
    void creationStoresHashAndReturnsSecretOnce() throws Exception {
        existingApplication();
        generatedIds();
        var issued = service.create(appId, " Production ");
        assertTrue(issued.secret().matches("er_[A-Za-z0-9_-]{43}"));
        var saved = ArgumentCaptor.forClass(ApiKey.class);
        verify(keys).saveAndFlush(saved.capture());
        String expectedHash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(issued.secret().getBytes(StandardCharsets.UTF_8)));
        assertEquals(expectedHash, saved.getValue().getKeyHash());
        assertEquals(issued.secret().substring(0, 11), issued.key().prefix());
        assertEquals("Production", issued.key().name());
        assertFalse(issued.toString().contains(issued.secret()));
    }

    @Test
    void rotationRevokesOldKeyAndRecordsReplacement() {
        existingApplication();
        generatedIds();
        var oldKey = key();
        when(keys.findForUpdate(keyId, appId)).thenReturn(Optional.of(oldKey));
        var issued = service.rotate(appId, keyId);
        assertNotNull(oldKey.getRevokedAt());
        assertEquals(issued.key().id(), oldKey.getReplacedById());
        assertNotEquals(keyId, issued.key().id());
    }

    @Test
    void revokedKeyCannotBeRotated() {
        existingApplication();
        var oldKey = key();
        oldKey.revoke();
        when(keys.findForUpdate(keyId, appId)).thenReturn(Optional.of(oldKey));
        var error = assertThrows(ResponseStatusException.class, () -> service.rotate(appId, keyId));
        assertEquals(409, error.getStatusCode().value());
        verify(keys, never()).saveAndFlush(any());
    }

    @Test
    void repeatedRevocationPreservesOriginalTimestamp() {
        existingApplication();
        var key = key();
        when(keys.findForUpdate(keyId, appId)).thenReturn(Optional.of(key));
        service.revoke(appId, keyId);
        Instant revokedAt = key.getRevokedAt();
        service.revoke(appId, keyId);
        assertEquals(revokedAt, key.getRevokedAt());
    }

    @Test
    void anotherApplicationsKeyIsNotRotated() {
        existingApplication();
        var error = assertThrows(ResponseStatusException.class, () -> service.rotate(appId, keyId));
        assertEquals(404, error.getStatusCode().value());
        verify(keys, never()).saveAndFlush(any());
    }

    @Test
    void authenticationOnlyAcceptsActiveKeyHashes() {
        String secret = "er_" + "a".repeat(43);
        when(keys.findByKeyHashAndRevokedAtIsNull(anyString())).thenReturn(Optional.of(key()));
        assertEquals(appId, service.authenticate(secret));
        when(keys.findByKeyHashAndRevokedAtIsNull(anyString())).thenReturn(Optional.empty());
        var error = assertThrows(ResponseStatusException.class, () -> service.authenticate(secret));
        assertEquals(401, error.getStatusCode().value());
    }

    @Test
    void malformedKeyIsRejectedBeforeDatabaseLookup() {
        var error = assertThrows(ResponseStatusException.class, () -> service.authenticate("invalid"));
        assertEquals(401, error.getStatusCode().value());
        verifyNoInteractions(keys);
    }

    private ApiKey key() {
        var key = new ApiKey(appId, "Production", "0".repeat(64), "er_example");
        ReflectionTestUtils.setField(key, "id", keyId);
        return key;
    }
    private void existingApplication() { when(access.lockedApplication(appId)).thenReturn(new com.eventrelay.servicea.entity.Application(UUID.randomUUID(), "Shop")); }
    private void generatedIds() {
        when(keys.saveAndFlush(any())).thenAnswer(call -> {
            ApiKey key = call.getArgument(0);
            ReflectionTestUtils.setField(key, "id", UUID.randomUUID());
            ReflectionTestUtils.setField(key, "createdAt", Instant.now());
            return key;
        });
    }
}

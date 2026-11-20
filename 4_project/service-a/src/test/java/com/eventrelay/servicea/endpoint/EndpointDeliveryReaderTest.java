package com.eventrelay.servicea.endpoint;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EndpointDeliveryReaderTest {
    @Test
    void dispatchSnapshotRetainsAcceptedConfigurationAndRedactsSecrets() {
        var repository = mock(EndpointRepository.class);
        var app = UUID.randomUUID();
        var id = UUID.randomUUID();
        var endpoint = new Endpoint(app, "https://example.com/original", null, Set.of());
        endpoint.configure(true, 10, 4, 10000, 5);
        endpoint.rotateSigningSecret("original-encrypted-secret");
        when(repository.findByIdAndApplicationId(id, app)).thenReturn(Optional.of(endpoint));
        var snapshot = new EndpointDeliveryReader(repository).findByIdAndApplicationId(id, app).orElseThrow();

        endpoint.updateUrl("https://example.com/changed");
        endpoint.configure(false, 50, 8, 20000, 10);
        endpoint.rotateSigningSecret("replacement-secret");

        assertEquals("https://example.com/original", snapshot.url());
        assertEquals(10, snapshot.rateLimitPerSecond());
        assertEquals(4, snapshot.concurrencyLimit());
        assertEquals(10000, snapshot.timeoutMs());
        assertEquals(5, snapshot.maxAttempts());
        assertEquals("original-encrypted-secret", snapshot.signingSecretEncrypted());
        assertFalse(snapshot.toString().contains(snapshot.signingSecretEncrypted()));
    }
}

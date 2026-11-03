package com.eventrelay.servicea.events;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.eventrelay.servicea.entity.*;
import com.eventrelay.servicea.messaging.*;
import com.eventrelay.servicea.repository.*;
import com.eventrelay.servicea.service.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventIngestionServiceTest {
    @Mock ApiKeyService keys;
    @Mock ResourceAccess access;
    @Mock EventTypeRepository types;
    @Mock EndpointRepository endpoints;
    @Mock EventStore store;
    @Mock Outbox outbox;
    ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
    UUID appId = UUID.randomUUID();
    UUID typeId = UUID.randomUUID();
    EventIngestionService service;
    @BeforeEach void setUp() { service = new EventIngestionService(keys, access, types, endpoints, store, outbox, mapper); }

    @Test void sameKeyAndReorderedJsonReturnOriginalReceiptWithoutPublishing() throws Exception {
        when(keys.authenticate("key")).thenReturn(appId);
        String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest("order.created\n{\"a\":1,\"b\":2}".getBytes(StandardCharsets.UTF_8)));
        var receipt = new EventApi.Accepted(UUID.randomUUID(), "order.created", 1, Instant.now());
        when(store.existing(appId, "same")).thenReturn(Optional.of(new EventStore.Stored(hash, receipt)));
        assertEquals(receipt, service.receive(appId, "key", "same", new EventApi.IngestRequest("order.created", mapper.readTree("{\"b\":2,\"a\":1}"))));
        verifyNoInteractions(outbox, types, endpoints);
    }
    @Test void keyReuseWithDifferentPayloadIsConflict() throws Exception {
        when(keys.authenticate("key")).thenReturn(appId);
        when(store.existing(appId, "same")).thenReturn(Optional.of(new EventStore.Stored("different", new EventApi.Accepted(UUID.randomUUID(), "order.created", 1, Instant.now()))));
        var error = assertThrows(ResponseStatusException.class, () -> service.receive(appId, "key", "same", new EventApi.IngestRequest("order.created", mapper.createObjectNode())));
        assertEquals(409, error.getStatusCode().value());
        verifyNoInteractions(outbox);
    }
    @Test void endpointSnapshotAndEventAreEnqueuedInTheSameServiceOperation() {
        when(keys.authenticate("key")).thenReturn(appId);
        var type = type();
        when(types.findByApplicationIdAndName(appId, "order.created")).thenReturn(Optional.of(type));
        var endpoint = new Endpoint(appId, "https://example.com/hook", null, Set.of(type));
        ReflectionTestUtils.setField(endpoint, "id", UUID.randomUUID());
        endpoint.rotateSigningSecret("encrypted");
        endpoint.configure(true, 25, 2, 9000, 3);
        when(endpoints.findSubscribed(appId, typeId)).thenReturn(List.of(endpoint));
        var receipt = service.receive(appId, "key", "new", new EventApi.IngestRequest("order.created", mapper.createObjectNode().put("orderId", "123")));
        assertEquals(1, receipt.deliveryCount());
        var command = ArgumentCaptor.forClass(DeliveryCommand.class);
        verify(outbox).enqueue(any(), any(), eq("event-relay.delivery"), eq("delivery.requested"), command.capture());
        assertEquals(endpoint.getId(), command.getValue().endpointId());
        assertEquals("encrypted", command.getValue().signingSecretEncrypted());
        assertEquals(9000, command.getValue().timeoutMs());
        assertEquals(3, command.getValue().maxAttempts());
        verify(store).save(eq(receipt.id()), eq(appId), eq(typeId), eq("order.created"), eq("new"), anyString(), anyString(), eq(1), any());
    }
    @Test void unsignedLegacyEndpointIsRejectedBeforePersistence() {
        when(keys.authenticate("key")).thenReturn(appId);
        var type = type();
        when(types.findByApplicationIdAndName(appId, "order.created")).thenReturn(Optional.of(type));
        when(endpoints.findSubscribed(appId, typeId)).thenReturn(List.of(new Endpoint(appId, "https://example.com", null, Set.of(type))));
        var error = assertThrows(ResponseStatusException.class, () -> service.receive(appId, "key", "new", new EventApi.IngestRequest("order.created", mapper.createObjectNode())));
        assertEquals(409, error.getStatusCode().value());
        verify(store, never()).save(any(), any(), any(), any(), any(), any(), any(), anyInt(), any());
        verifyNoInteractions(outbox);
    }
    @Test void anotherApplicationsKeyCannotPublish() {
        when(keys.authenticate("key")).thenReturn(UUID.randomUUID());
        var error = assertThrows(ResponseStatusException.class, () -> service.receive(appId, "key", "new", new EventApi.IngestRequest("order.created", mapper.createObjectNode())));
        assertEquals(403, error.getStatusCode().value());
        verifyNoInteractions(store, outbox, access);
    }
    @Test void missingIdempotencyKeyIsRejected() {
        when(keys.authenticate("key")).thenReturn(appId);
        var error = assertThrows(ResponseStatusException.class, () -> service.receive(appId, "key", null, new EventApi.IngestRequest("order.created", mapper.createObjectNode())));
        assertEquals(400, error.getStatusCode().value());
        verifyNoInteractions(store, outbox);
    }
    private EventType type() {
        var type = new EventType(appId, "order.created", null);
        ReflectionTestUtils.setField(type, "id", typeId);
        return type;
    }
}

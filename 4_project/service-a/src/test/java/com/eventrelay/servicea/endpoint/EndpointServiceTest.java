package com.eventrelay.servicea.endpoint;

import com.eventrelay.servicea.application.Application;
import com.eventrelay.servicea.auth.access.ResourceAccess;
import com.eventrelay.servicea.common.crypto.SecretEncryption;
import com.eventrelay.servicea.endpoint.EndpointRepository;
import com.eventrelay.servicea.endpoint.EndpointService;
import com.eventrelay.servicea.endpoint.dto.RegisterEndpoint;
import com.eventrelay.servicea.eventtype.EventType;
import com.eventrelay.servicea.eventtype.EventTypeRepository;
import com.eventrelay.servicea.workspace.AuditLogs;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EndpointServiceTest {
    @Mock EndpointRepository endpoints;
    @Mock EventTypeRepository eventTypes;
    @Mock ResourceAccess access;
    @Mock SecretEncryption encryption;
    @Mock AuditLogs audit;
    EndpointService endpointsService;
    UUID appId = UUID.randomUUID();
    @BeforeEach void setUp() { endpointsService = new EndpointService(endpoints,eventTypes,access,encryption,audit); }
    private void existingApplication() { when(access.lockedApplication(appId)).thenReturn(new Application(UUID.randomUUID(), "Shop")); }

    @Test
    void rejectsSubscriptionsFromAnotherApplication() {
        existingApplication();
        UUID eventId = UUID.randomUUID();
        when(eventTypes.findAllById(Set.of(eventId)))
                .thenReturn(List.of(new EventType(UUID.randomUUID(), "order.created", null)));
        var error = assertThrows(ResponseStatusException.class, () -> endpointsService.registerEndpoint(appId,
                new RegisterEndpoint("https://example.com/hook", null, Set.of(eventId), null)));
        assertEquals(400, error.getStatusCode().value());
        verifyNoInteractions(endpoints);
    }

    @Test
    void rejectsUnknownSubscriptions() {
        existingApplication();
        UUID eventId = UUID.randomUUID();
        when(eventTypes.findAllById(Set.of(eventId))).thenReturn(List.of());
        var error = assertThrows(ResponseStatusException.class, () -> endpointsService.registerEndpoint(appId,
                new RegisterEndpoint("https://example.com/hook", null, Set.of(eventId), null)));
        assertEquals(404, error.getStatusCode().value());
        verifyNoInteractions(endpoints);
    }

    @Test
    void rejectsNonHttpWebhookUrls() {
        existingApplication();
        var error = assertThrows(ResponseStatusException.class, () -> endpointsService.registerEndpoint(appId,
                new RegisterEndpoint("file:///tmp/hook", null, Set.of(UUID.randomUUID()), null)));
        assertEquals(400, error.getStatusCode().value());
        verifyNoInteractions(eventTypes, endpoints);
    }
}

package com.eventrelay.servicea.eventtype;

import com.eventrelay.servicea.auth.access.ResourceAccess;
import com.eventrelay.servicea.endpoint.EndpointRepository;
import com.eventrelay.servicea.eventtype.EventType;
import com.eventrelay.servicea.eventtype.EventTypeRepository;
import com.eventrelay.servicea.eventtype.EventTypeService;
import com.eventrelay.servicea.workspace.AuditLogs;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventTypeServiceTest {
    @Mock EventTypeRepository events;
    @Mock EndpointRepository endpoints;
    @Mock ResourceAccess access;
    EventTypeService eventService;
    UUID appId = UUID.randomUUID();
    @BeforeEach void setUp() { eventService = new EventTypeService(events,access,mock(AuditLogs.class)); }

    @Test void eventDeletionPreservesTheEntityAndSubscriptionHistory() {
        var event = new EventType(appId, "order.created", null);
        UUID id = UUID.randomUUID();
        when(events.findByIdAndApplicationId(id, appId)).thenReturn(Optional.of(event));
        eventService.deleteEventType(appId, id);
        assertNotNull(event.getDeletedAt());
        verify(events, never()).delete(any(EventType.class));
        verifyNoInteractions(endpoints);
    }
}

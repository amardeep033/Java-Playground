package com.eventrelay.servicea.service;

import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import com.eventrelay.servicea.entity.*;
import com.eventrelay.servicea.repository.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SoftDeletionServiceTest {
    @Mock ResourceAccess access;
    @Mock ApplicationRepository applications;
    @Mock EventTypeRepository events;
    @Mock EndpointRepository endpoints;
    @Mock ApiKeyRepository keys;
    SoftDeletionService service;
    UUID userId = UUID.randomUUID();
    UUID appId = UUID.randomUUID();

    @BeforeEach void setUp() { service = new SoftDeletionService(access, applications, events, endpoints, keys); }

    @Test void applicationDeletionRetainsRowsAndMarksAllChildren() {
        var app = new Application(userId, "Shop");
        ReflectionTestUtils.setField(app, "id", appId);
        when(access.lockedApplication(appId)).thenReturn(app);
        service.deleteApplication(userId, appId);
        assertNotNull(app.getDeletedAt());
        verify(endpoints).softDeleteByApplicationId(eq(appId), any());
        verify(events).softDeleteByApplicationId(eq(appId), any());
        verify(keys).softDeleteByApplicationId(eq(appId), any());
        verify(applications, never()).delete(any());
    }

    @Test void anotherUsersApplicationIsNotDeleted() {
        var app = new Application(UUID.randomUUID(), "Other shop");
        when(access.lockedApplication(appId)).thenReturn(app);
        var error = assertThrows(ResponseStatusException.class, () -> service.deleteApplication(userId, appId));
        assertEquals(404, error.getStatusCode().value());
        assertNull(app.getDeletedAt());
        verifyNoInteractions(endpoints, events, keys);
    }

    @Test void eventDeletionPreservesTheEntityAndSubscriptionHistory() {
        var event = new EventType(appId, "order.created", null);
        UUID id = UUID.randomUUID();
        when(events.findByIdAndApplicationId(id, appId)).thenReturn(Optional.of(event));
        service.deleteEventType(appId, id);
        assertNotNull(event.getDeletedAt());
        verify(events, never()).delete(any());
        verifyNoInteractions(endpoints);
    }
}

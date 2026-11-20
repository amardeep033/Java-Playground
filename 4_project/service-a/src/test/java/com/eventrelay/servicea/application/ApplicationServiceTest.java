package com.eventrelay.servicea.application;

import com.eventrelay.servicea.apikey.ApiKeyRepository;
import com.eventrelay.servicea.application.Application;
import com.eventrelay.servicea.application.ApplicationRepository;
import com.eventrelay.servicea.application.ApplicationService;
import com.eventrelay.servicea.application.dto.RegisterApplication;
import com.eventrelay.servicea.auth.access.ResourceAccess;
import com.eventrelay.servicea.endpoint.EndpointRepository;
import com.eventrelay.servicea.eventtype.EventTypeRepository;
import com.eventrelay.servicea.workspace.AuditLogs;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceTest {
    @Mock ResourceAccess access;
    @Mock ApplicationRepository applications;
    @Mock EndpointRepository endpoints;
    @Mock EventTypeRepository events;
    @Mock ApiKeyRepository keys;
    ApplicationService service;
    UUID userId = UUID.randomUUID();
    UUID appId = UUID.randomUUID();
    @BeforeEach void setUp() { service = new ApplicationService(applications,endpoints,events,keys,access,mock(AuditLogs.class),mock(JdbcTemplate.class)); }

    @Test
    void missingUserCannotRegisterAnApplication() {
        UUID userId = UUID.randomUUID();
        when(access.user(userId)).thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND));
        var error = assertThrows(ResponseStatusException.class,
                () -> service.registerApplication(userId, new RegisterApplication("Shop")));
        assertEquals(404, error.getStatusCode().value());
        verifyNoInteractions(applications);
    }

    @Test void applicationDeletionRetainsRowsAndMarksAllChildren() {
        var app = new Application(userId, "Shop");
        ReflectionTestUtils.setField(app, "id", appId);
        when(access.lockedApplication(appId)).thenReturn(app);
        service.deleteApplication(userId, appId);
        assertNotNull(app.getDeletedAt());
        verify(endpoints).softDeleteByApplicationId(eq(appId), any());
        verify(events).softDeleteByApplicationId(eq(appId), any());
        verify(keys).softDeleteByApplicationId(eq(appId), any());
        verify(applications, never()).delete(any(Application.class));
    }

    @Test void anotherUsersApplicationIsNotDeleted() {
        var app = new Application(UUID.randomUUID(), "Other shop");
        when(access.lockedApplication(appId)).thenReturn(app);
        var error = assertThrows(ResponseStatusException.class, () -> service.deleteApplication(userId, appId));
        assertEquals(404, error.getStatusCode().value());
        assertNull(app.getDeletedAt());
        verifyNoInteractions(endpoints, events, keys);
    }
}

package com.eventrelay.servicea.service;

import com.eventrelay.servicea.dto.Requests.*;
import com.eventrelay.servicea.entity.*;
import com.eventrelay.servicea.repository.*;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceTest {
    @Mock UserAccountRepository users;
    @Mock ApplicationRepository applications;
    @Mock EventTypeRepository eventTypes;
    @Mock EndpointRepository endpoints;
    @Mock ResourceAccess access;
    @Mock com.eventrelay.servicea.security.SecretEncryption encryption;
    @Mock com.eventrelay.servicea.workspace.AuditLogs audit;
    @Mock org.springframework.jdbc.core.JdbcTemplate db;
    RegistrationService service;
    UUID appId = UUID.randomUUID();
    PasswordEncoder passwords = Pbkdf2PasswordEncoder.defaultsForSpringSecurity_v5_8();

    @BeforeEach
    void setUp() {
        service = new RegistrationService(users, applications, eventTypes, endpoints, passwords, access, encryption,audit,db);
    }

    @Test
    void normalizesEmailBeforeSaving() {
        when(users.saveAndFlush(any())).thenAnswer(call -> call.getArgument(0));
        var response = service.registerUser(new Signup(" Alice ","Acme", "Alice@EXAMPLE.com", "ExamplePass123!"));
        assertEquals("alice@example.com", response.email());
        assertEquals("Alice", response.name());
        var saved = ArgumentCaptor.forClass(UserAccount.class);
        verify(users).saveAndFlush(saved.capture());
        assertNotEquals("ExamplePass123!", saved.getValue().getPasswordHash());
        assertTrue(passwords.matches("ExamplePass123!", saved.getValue().getPasswordHash()));
    }

    @Test
    void passwordChangeRequiresCurrentPasswordAndStoresNewHash() {
        UUID userId = UUID.randomUUID();
        var user = new UserAccount("Alice", "alice@example.com", passwords.encode("ExamplePass123!"));
        when(users.findForUpdate(userId)).thenReturn(Optional.of(user));
        service.changePassword(userId, new ChangePassword("ExamplePass123!", "ChangedPass123!"));
        assertTrue(passwords.matches("ChangedPass123!", user.getPasswordHash()));
        assertFalse(passwords.matches("ExamplePass123!", user.getPasswordHash()));
    }

    @Test
    void wrongCurrentPasswordLeavesHashUnchanged() {
        UUID userId = UUID.randomUUID();
        String hash = passwords.encode("ExamplePass123!");
        var user = new UserAccount("Alice", "alice@example.com", hash);
        when(users.findForUpdate(userId)).thenReturn(Optional.of(user));
        var error = assertThrows(ResponseStatusException.class, () -> service.changePassword(userId,
                new ChangePassword("IncorrectPass!", "ChangedPass123!")));
        assertEquals(401, error.getStatusCode().value());
        assertEquals(hash, user.getPasswordHash());
    }

    @Test
    void passwordChangeRejectsReusingCurrentPassword() {
        UUID userId = UUID.randomUUID();
        var user = new UserAccount("Alice", "alice@example.com", passwords.encode("ExamplePass123!"));
        when(users.findForUpdate(userId)).thenReturn(Optional.of(user));
        var error = assertThrows(ResponseStatusException.class, () -> service.changePassword(userId,
                new ChangePassword("ExamplePass123!", "ExamplePass123!")));
        assertEquals(400, error.getStatusCode().value());
    }

    @Test
    void missingUserCannotRegisterAnApplication() {
        UUID userId = UUID.randomUUID();
        when(access.user(userId)).thenThrow(new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND));
        var error = assertThrows(ResponseStatusException.class,
                () -> service.registerApplication(userId, new RegisterApplication("Shop")));
        assertEquals(404, error.getStatusCode().value());
        verifyNoInteractions(applications);
    }

    @Test
    void rejectsSubscriptionsFromAnotherApplication() {
        existingApplication();
        UUID eventId = UUID.randomUUID();
        when(eventTypes.findAllById(Set.of(eventId)))
                .thenReturn(List.of(new EventType(UUID.randomUUID(), "order.created", null)));
        var error = assertThrows(ResponseStatusException.class, () -> service.registerEndpoint(appId,
                new RegisterEndpoint("https://example.com/hook", null, Set.of(eventId), null)));
        assertEquals(400, error.getStatusCode().value());
        verifyNoInteractions(endpoints);
    }

    @Test
    void rejectsUnknownSubscriptions() {
        existingApplication();
        UUID eventId = UUID.randomUUID();
        when(eventTypes.findAllById(Set.of(eventId))).thenReturn(List.of());
        var error = assertThrows(ResponseStatusException.class, () -> service.registerEndpoint(appId,
                new RegisterEndpoint("https://example.com/hook", null, Set.of(eventId), null)));
        assertEquals(404, error.getStatusCode().value());
        verifyNoInteractions(endpoints);
    }

    @Test
    void rejectsNonHttpWebhookUrls() {
        existingApplication();
        var error = assertThrows(ResponseStatusException.class, () -> service.registerEndpoint(appId,
                new RegisterEndpoint("file:///tmp/hook", null, Set.of(UUID.randomUUID()), null)));
        assertEquals(400, error.getStatusCode().value());
        verifyNoInteractions(eventTypes, endpoints);
    }

    private void existingApplication() {
        when(access.lockedApplication(appId)).thenReturn(new Application(UUID.randomUUID(), "Shop"));
    }
}

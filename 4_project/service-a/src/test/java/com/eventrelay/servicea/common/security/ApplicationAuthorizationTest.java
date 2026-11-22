package com.eventrelay.servicea.common.security;

import com.eventrelay.servicea.apikey.ApiKeyService;
import com.eventrelay.servicea.auth.JwtUser;
import com.eventrelay.servicea.auth.access.ResourceAccess;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApplicationAuthorizationTest {
    @Mock ApiKeyService keys;
    @Mock ResourceAccess access;
    private ApplicationAuthorization authorization;
    private final UUID appId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        authorization = new ApplicationAuthorization(keys, access);
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void dashboardWithoutApiKeyChecksLiveApplicationAccess() {
        dashboardUser();
        authorization.authorize(appId, null);
        verify(access).application(appId);
        verifyNoInteractions(keys);
    }

    @Test
    void validApiKeyForApplicationIsAccepted() {
        when(keys.authenticate("key")).thenReturn(appId);
        authorization.authorize(appId, "key");
        verify(keys).authenticate("key");
        verifyNoInteractions(access);
    }

    @Test
    void anotherApplicationsKeyIsForbiddenEvenWithDashboardPrincipal() {
        dashboardUser();
        when(keys.authenticate("key")).thenReturn(UUID.randomUUID());
        var failure = assertThrows(ResponseStatusException.class, () -> authorization.authorize(appId, "key"));
        assertEquals(403, failure.getStatusCode().value());
        verifyNoInteractions(access);
    }

    @ParameterizedTest
    @ValueSource(strings = {"invalid", "", " "})
    void invalidExplicitKeyNeverFallsBackToDashboard(String key) {
        dashboardUser();
        var rejected = new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid API key");
        when(keys.authenticate(key)).thenThrow(rejected);
        assertSame(rejected, assertThrows(ResponseStatusException.class, () -> authorization.authorize(appId, key)));
        verifyNoInteractions(access);
    }

    @Test
    void missingCredentialsAreRejected() {
        when(keys.authenticate(null)).thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        var failure = assertThrows(ResponseStatusException.class, () -> authorization.authorize(appId, null));
        assertEquals(401, failure.getStatusCode().value());
        verifyNoInteractions(access);
    }

    private void dashboardUser() {
        var user = new JwtUser(UUID.randomUUID(), "USER", UUID.randomUUID(), Instant.now().plusSeconds(60));
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(user, null, List.of()));
    }
}

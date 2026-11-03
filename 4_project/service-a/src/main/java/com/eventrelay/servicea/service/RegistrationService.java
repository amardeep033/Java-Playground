package com.eventrelay.servicea.service;

import java.net.URI;
import java.util.*;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.eventrelay.servicea.dto.Requests.*;
import com.eventrelay.servicea.dto.Responses;
import com.eventrelay.servicea.entity.*;
import com.eventrelay.servicea.repository.*;
import org.springframework.security.crypto.password.PasswordEncoder;

@Service
@Transactional(readOnly = true)
public class RegistrationService {
    private final com.eventrelay.servicea.auth.AuthService auth;
    private final UserAccountRepository users;
    private final ApplicationRepository applications;
    private final EventTypeRepository eventTypes;
    private final EndpointRepository endpoints;
    private final PasswordEncoder passwords;
    private final ResourceAccess access;
    private final com.eventrelay.servicea.security.SecretEncryption encryption;

    public RegistrationService(UserAccountRepository users, ApplicationRepository applications,
                               EventTypeRepository eventTypes, EndpointRepository endpoints, PasswordEncoder passwords,
                               ResourceAccess access, com.eventrelay.servicea.security.SecretEncryption encryption, com.eventrelay.servicea.auth.AuthService auth) {
        this.auth = auth;
        this.users = users;
        this.applications = applications;
        this.eventTypes = eventTypes;
        this.endpoints = endpoints;
        this.passwords = passwords;
        this.access = access;
        this.encryption = encryption;
    }

    @Transactional
    public Responses.User registerUser(RegisterUser request) {
        var user = new UserAccount(request.name().strip(), request.email().strip().toLowerCase(Locale.ROOT),
                passwords.encode(request.password()));
        return Responses.User.from(users.saveAndFlush(user));
    }

    @Transactional
    public void changePassword(UUID userId, ChangePassword request) {
        var user = users.findForUpdate(userId).orElseThrow(() -> notFound("User"));
        if (!passwords.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Current password is incorrect");
        }
        if (passwords.matches(request.newPassword(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "New password must differ from the current password");
        }
        user.changePasswordHash(passwords.encode(request.newPassword()));
        auth.revokeAll(userId);
    }

    public Responses.User getUser(UUID userId) {
        return Responses.User.from(requireUser(userId));
    }

    @Transactional
    public Responses.App registerApplication(UUID userId, RegisterApplication request) {
        access.lockedUser(userId);
        return Responses.App.from(applications.saveAndFlush(new Application(userId, request.name().strip())));
    }

    public Responses.PageResult<Responses.App> listApplications(UUID userId, Pageable page) {
        requireUser(userId);
        return Responses.PageResult.from(applications.findByUserId(userId, page).map(Responses.App::from));
    }

    public Responses.App getApplication(UUID userId, UUID applicationId) {
        requireUser(userId);
        var app = applications.findByIdAndUserId(applicationId, userId)
                .orElseThrow(() -> notFound("Application"));
        return Responses.App.from(app);
    }

    @Transactional
    public Responses.Event registerEventType(UUID applicationId, RegisterEventType request) {
        access.lockedApplication(applicationId);
        var event = new EventType(applicationId, request.name(), request.description());
        return Responses.Event.from(eventTypes.saveAndFlush(event));
    }

    public Responses.PageResult<Responses.Event> listEventTypes(UUID applicationId, Pageable page) {
        requireApplication(applicationId);
        return Responses.PageResult.from(eventTypes.findByApplicationId(applicationId, page).map(Responses.Event::from));
    }

    public Responses.Event getEventType(UUID applicationId, UUID eventTypeId) {
        requireApplication(applicationId);
        return Responses.Event.from(eventTypes.findByIdAndApplicationId(eventTypeId, applicationId)
                .orElseThrow(() -> notFound("Event type")));
    }

    @Transactional
    public Responses.IssuedEndpoint registerEndpoint(UUID applicationId, RegisterEndpoint request) {
        access.lockedApplication(applicationId);
        validateUrl(request.url());
        var subscriptions = eventTypes.findAllById(request.eventTypeIds());
        if (subscriptions.size() != request.eventTypeIds().size()) {
            throw notFound("Event type");
        }
        if (subscriptions.stream().anyMatch(event -> !event.getApplicationId().equals(applicationId))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "All event types must belong to this application");
        }
        var endpoint = new Endpoint(applicationId, request.url(), request.description(), new HashSet<>(subscriptions));
        var config = request.configuration() == null ? EndpointConfiguration.defaults() : request.configuration();
        endpoint.configure(config.enabled(), config.rateLimitPerSecond(), config.concurrencyLimit(), config.timeoutMs(), config.maxAttempts());
        endpoints.saveAndFlush(endpoint);
        return issueSigningSecret(endpoint);
    }

    public Responses.PageResult<Responses.WebhookEndpoint> listEndpoints(UUID applicationId, Pageable page) {
        requireApplication(applicationId);
        return Responses.PageResult.from(endpoints.findByApplicationId(applicationId, page).map(Responses.WebhookEndpoint::from));
    }

    public Responses.WebhookEndpoint getEndpoint(UUID applicationId, UUID endpointId) {
        requireApplication(applicationId);
        return Responses.WebhookEndpoint.from(endpoints.findByIdAndApplicationId(endpointId, applicationId)
                .orElseThrow(() -> notFound("Endpoint")));
    }

    @Transactional
    public Responses.WebhookEndpoint configureEndpoint(UUID applicationId, UUID endpointId, EndpointConfiguration config) {
        access.lockedApplication(applicationId);
        var endpoint = endpoints.findByIdAndApplicationId(endpointId, applicationId).orElseThrow(() -> notFound("Endpoint"));
        endpoint.configure(config.enabled(), config.rateLimitPerSecond(), config.concurrencyLimit(), config.timeoutMs(), config.maxAttempts());
        return Responses.WebhookEndpoint.from(endpoint);
    }

    @Transactional
    public Responses.IssuedEndpoint rotateSigningSecret(UUID applicationId, UUID endpointId) {
        access.lockedApplication(applicationId);
        var endpoint = endpoints.findByIdAndApplicationId(endpointId, applicationId).orElseThrow(() -> notFound("Endpoint"));
        return issueSigningSecret(endpoint);
    }

    private Responses.IssuedEndpoint issueSigningSecret(Endpoint endpoint) {
        String secret = encryption.generateSecret();
        endpoint.rotateSigningSecret(encryption.encrypt(secret, endpoint.getId()));
        return new Responses.IssuedEndpoint(Responses.WebhookEndpoint.from(endpoint), secret);
    }

    private UserAccount requireUser(UUID id) {
        return access.user(id);
    }

    private Application requireApplication(UUID id) {
        return access.application(id);
    }

    private static ResponseStatusException notFound(String resource) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, resource + " not found");
    }

    private static void validateUrl(String url) {
        try {
            var uri = URI.create(url);
            if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                    || uri.getHost() == null || uri.getUserInfo() != null || uri.getFragment() != null
                    || uri.getPort() > 65535 || uri.getPort() == 0) {
                throw new IllegalArgumentException();
            }
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "URL must be HTTP(S) with a valid host, without credentials or fragment");
        }
    }
}

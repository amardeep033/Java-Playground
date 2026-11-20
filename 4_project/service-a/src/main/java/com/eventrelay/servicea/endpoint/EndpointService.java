package com.eventrelay.servicea.endpoint;

import com.eventrelay.servicea.application.Application;
import com.eventrelay.servicea.auth.access.ResourceAccess;
import com.eventrelay.servicea.common.crypto.SecretEncryption;
import com.eventrelay.servicea.common.dto.PageResult;
import com.eventrelay.servicea.common.search.ListSearch;
import com.eventrelay.servicea.endpoint.dto.AdminEndpointUpdate;
import com.eventrelay.servicea.endpoint.dto.EndpointConfiguration;
import com.eventrelay.servicea.endpoint.dto.EndpointSettings;
import com.eventrelay.servicea.endpoint.dto.IssuedEndpoint;
import com.eventrelay.servicea.endpoint.dto.RegisterEndpoint;
import com.eventrelay.servicea.endpoint.dto.UpdateEndpoint;
import com.eventrelay.servicea.endpoint.dto.WebhookEndpoint;
import com.eventrelay.servicea.eventtype.EventType;
import com.eventrelay.servicea.eventtype.EventTypeRepository;
import com.eventrelay.servicea.workspace.AuditLogs;
import java.net.URI;
import java.util.*;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import static com.eventrelay.servicea.common.search.ListSearch.activationFilter;

@Service
@Transactional(readOnly = true)
public class EndpointService {
    private final EndpointRepository endpoints;
    private final EventTypeRepository eventTypes;
    private final ResourceAccess access;
    private final SecretEncryption encryption;
    private final AuditLogs audit;

    public EndpointService(EndpointRepository endpoints, EventTypeRepository eventTypes, ResourceAccess access,
            SecretEncryption encryption, AuditLogs audit) {
        this.endpoints = endpoints;
        this.eventTypes = eventTypes;
        this.access = access;
        this.encryption = encryption;
        this.audit = audit;
    }

    @Transactional
    public IssuedEndpoint registerEndpoint(UUID applicationId, RegisterEndpoint request) {
        access.lockedApplication(applicationId);
        validateUrl(request.url());
        var subscriptions = eventTypes.findAllById(request.eventTypeIds());
        if (subscriptions.size() != request.eventTypeIds().size()) {
            throw notFound("Event type");
        }
        if (subscriptions.stream().anyMatch(event -> !event.getApplicationId().equals(applicationId))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "All event types must belong to this application");
        }
        var endpoint = new Endpoint(applicationId, request.url(), request.description(), new HashSet<>(subscriptions));
        String name = request.name() == null ? URI.create(request.url()).getHost() : request.name();
        if (name.isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Endpoint name cannot be blank");
        endpoint.updateName(name.strip().substring(0, Math.min(120, name.strip().length())));
        var config = request.configuration() == null ? EndpointConfiguration.defaults() : request.configuration();
        endpoint.configure(config.enabled(), config.rateLimitPerSecond(), config.concurrencyLimit(), config.timeoutMs(),
                config.maxAttempts());
        endpoints.saveAndFlush(endpoint);
        return issueSigningSecret(endpoint, true);
    }

    public WebhookEndpoint getEndpoint(UUID applicationId, UUID endpointId) {
        requireApplication(applicationId);
        return WebhookEndpoint.from(endpoints.findByIdAndApplicationId(endpointId, applicationId)
                .orElseThrow(() -> notFound("Endpoint")));
    }

    @Transactional
    public IssuedEndpoint rotateSigningSecret(UUID applicationId, UUID endpointId) {
        access.lockedApplication(applicationId);
        var endpoint = endpoints.findByIdAndApplicationId(endpointId, applicationId)
                .orElseThrow(() -> notFound("Endpoint"));
        return issueSigningSecret(endpoint, false);
    }

    @Transactional
    public WebhookEndpoint updateEndpoint(UUID appId, UUID id, UpdateEndpoint request) {
        access.lockedApplication(appId);
        var endpoint = endpoints.findByIdAndApplicationId(id, appId).orElseThrow(() -> notFound("Endpoint"));
        if (request.name() != null) {
            if (request.name().isBlank())
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Endpoint name cannot be blank");
            audit.changed(appId, "endpoints", id, "name", endpoint.getName(), request.name().strip());
            endpoint.updateName(request.name().strip());
        }
        if (request.hasDescription())
            endpoint.updateDescription(request.description());
        if (request.configuration() != null) {
            var config = request.configuration();
            return updateEndpointSettings(appId, id,
                    new EndpointSettings(null, null,
                            new EndpointConfiguration(endpoint.isEnabled(), config.rateLimitPerSecond(),
                                    config.concurrencyLimit(), config.timeoutMs(), config.maxAttempts())));
        }
        return WebhookEndpoint.from(endpoint);
    }

    @Transactional
    public WebhookEndpoint adminEndpointUpdate(UUID appId, UUID id, AdminEndpointUpdate request) {
        access.lockedApplication(appId);
        access.workspaceAdmin(appId);
        updateEndpointSettings(appId, id, new EndpointSettings(request.url(), request.eventTypeIds(), null));
        var endpoint = endpoints.findByIdAndApplicationId(id, appId).orElseThrow(() -> notFound("Endpoint"));
        if (request.active() != null) {
            audit.changed(appId, "endpoints", id, "is_active", endpoint.isEnabled(), request.active());
            endpoint.setActive(request.active());
        }
        return WebhookEndpoint.from(endpoint);
    }

    public PageResult<WebhookEndpoint> listEndpoints(UUID appId, String q, String status, Pageable page) {
        requireApplication(appId);
        var spec = ListSearch.<Endpoint>scoped("applicationId", appId, q, "name", "url", "description");
        spec = activationFilter(spec, status, "enabled");
        return PageResult.from(endpoints.findAll(spec, page).map(WebhookEndpoint::from));
    }

    private WebhookEndpoint updateEndpointSettings(UUID appId, UUID id, EndpointSettings request) {
        access.lockedApplication(appId);
        var endpoint = endpoints.findByIdAndApplicationId(id, appId).orElseThrow(() -> notFound("Endpoint"));
        if (request.url() != null && !endpoint.getUrl().equals(request.url())) {
            access.workspaceAdmin(appId);
            validateUrl(request.url());
            audit.write(appId, "endpoints", id, "ENDPOINT_UPDATED",
                    Map.of("field", "url", "old", endpoint.getUrl(), "new", request.url()));
            endpoint.updateUrl(request.url());
        }
        if (request.eventTypeIds() != null) {
            var subscriptions = eventTypes.findAllById(request.eventTypeIds());
            if (subscriptions.size() != request.eventTypeIds().size())
                throw notFound("Event type");
            if (subscriptions.stream().anyMatch(type -> !type.getApplicationId().equals(appId)))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "All event types must belong to this application");
            var old = endpoint.getEventTypes().stream().map(EventType::getId).sorted().toList();
            var next = subscriptions.stream().map(EventType::getId).sorted().toList();
            if (!old.equals(next)) {
                access.workspaceAdmin(appId);
                audit.write(appId, "endpoints", id, "EVENT_TYPE_MAPPINGS_UPDATED",
                        Map.of("field", "eventTypeIds", "old", old, "new", next));
                endpoint.updateSubscriptions(new HashSet<>(subscriptions));
            }
        }
        var config = request.configuration();
        if (config != null) {
            if (endpoint.isEnabled() != config.enabled()) {
                access.workspaceAdmin(appId);
                audit.changed(appId, "endpoints", id, "is_active", endpoint.isEnabled(), config.enabled());
            }
            var before = configurationAudit(endpoint);
            var after = Map.of("rateLimitPerSecond", config.rateLimitPerSecond(), "concurrencyLimit",
                    config.concurrencyLimit(), "timeoutMs", config.timeoutMs(), "maxAttempts", config.maxAttempts());
            if (!before.equals(after))
                audit.write(appId, "endpoints", id, "CONFIG_UPDATED", Map.of("old", before, "new", after));
            endpoint.configure(config.enabled(), config.rateLimitPerSecond(), config.concurrencyLimit(),
                    config.timeoutMs(), config.maxAttempts());
        }
        return WebhookEndpoint.from(endpoint);
    }

    private Map<String, Integer> configurationAudit(Endpoint endpoint) {
        return Map.of("rateLimitPerSecond", endpoint.getRateLimitPerSecond(), "concurrencyLimit",
                endpoint.getConcurrencyLimit(), "timeoutMs", endpoint.getTimeoutMs(), "maxAttempts",
                endpoint.getMaxAttempts());
    }

    private IssuedEndpoint issueSigningSecret(Endpoint endpoint, boolean initial) {
        String secret = encryption.generateSecret();
        endpoint.rotateSigningSecret(encryption.encrypt(secret, endpoint.getId()));
        audit.write(endpoint.getApplicationId(), "endpoints", endpoint.getId(),
                initial ? "CREATED" : "SIGNING_CREDENTIAL_ROTATED",
                Map.of("rotation", endpoint.getSigningSecretVersion()));
        return new IssuedEndpoint(WebhookEndpoint.from(endpoint), secret);
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
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "URL must be HTTP(S) with a valid host, without credentials or fragment");
        }
    }

    @Transactional
    public void deleteEndpoint(UUID appId, UUID endpointId) {
        access.lockedApplication(appId);
        endpoints.findByIdAndApplicationId(endpointId, appId).orElseThrow(() -> ResourceAccess.notFound("Endpoint"))
                .softDelete();
    }

}

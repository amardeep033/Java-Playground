package com.eventrelay.servicea.endpoint.dto;

import com.eventrelay.servicea.endpoint.Endpoint;
import com.eventrelay.servicea.eventtype.EventType;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record WebhookEndpoint(UUID id, UUID applicationId, String url, String description,
        List<UUID> eventTypeIds, Instant createdAt, EndpointConfiguration configuration,
        int signingSecretVersion, Instant signingSecretRotatedAt, boolean active, String name) {
    public static WebhookEndpoint from(Endpoint value) {
        return new WebhookEndpoint(value.getId(), value.getApplicationId(), value.getUrl(), value.getDescription(),
                value.getEventTypes().stream().map(EventType::getId).sorted().toList(), value.getCreatedAt(),
                new EndpointConfiguration(value.isEnabled(), value.getRateLimitPerSecond(),
                        value.getConcurrencyLimit(), value.getTimeoutMs(), value.getMaxAttempts()),
                value.getSigningSecretVersion(), value.getSigningSecretRotatedAt(), value.isEnabled(),
                value.getName());
    }
}

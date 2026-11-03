package com.eventrelay.servicea.messaging;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;

public record DeliveryCommand(int schemaVersion, UUID messageId, UUID deliveryId, UUID eventId,
        UUID applicationId, UUID endpointId, String eventType, String url, String body,
        String signingSecretEncrypted, int signingSecretVersion, int rateLimitPerSecond,
        int concurrencyLimit, int timeoutMs, int maxAttempts, int attemptNumber, Instant createdAt, UUID replayOfId) {
    public DeliveryCommand(int schemaVersion, UUID messageId, UUID deliveryId, UUID eventId,
        UUID applicationId, UUID endpointId, String eventType, String url, String body,
        String signingSecretEncrypted, int signingSecretVersion, int rateLimitPerSecond,
        int concurrencyLimit, int timeoutMs, int maxAttempts, int attemptNumber, Instant createdAt) {
        this(schemaVersion, messageId, deliveryId, eventId, applicationId, endpointId, eventType, url, body, signingSecretEncrypted, signingSecretVersion, rateLimitPerSecond, concurrencyLimit, timeoutMs, maxAttempts, attemptNumber, createdAt, null);
    }
    public void validate() {
        if (schemaVersion != 1 || messageId == null || deliveryId == null || eventId == null
                || applicationId == null || endpointId == null || createdAt == null
                || eventType == null || !eventType.matches("[a-zA-Z0-9_.-]{1,120}")
                || body == null || body.isBlank() || body.length() > 1100000
                || signingSecretEncrypted == null || signingSecretEncrypted.isBlank()
                || signingSecretEncrypted.length() > 4096 || signingSecretVersion < 1
                || rateLimitPerSecond < 1 || rateLimitPerSecond > 100000
                || concurrencyLimit < 1 || concurrencyLimit > 1000
                || timeoutMs < 100 || timeoutMs > 60000 || maxAttempts < 1 || maxAttempts > 20
                || attemptNumber < 1 || attemptNumber > maxAttempts || url == null) {
            throw new IllegalArgumentException("Invalid delivery command");
        }
        var target = URI.create(url);
        if (!("https".equalsIgnoreCase(target.getScheme()) || "http".equalsIgnoreCase(target.getScheme()))
                || target.getHost() == null || target.getUserInfo() != null || target.getFragment() != null) {
            throw new IllegalArgumentException("Invalid delivery URL");
        }
    }
    public DeliveryCommand retry() {
        return forAttempt(attemptNumber + 1);
    }
    public DeliveryCommand forAttempt(int number) {
        return new DeliveryCommand(1, UUID.randomUUID(), deliveryId, eventId, applicationId, endpointId,
                eventType, url, body, signingSecretEncrypted, signingSecretVersion, rateLimitPerSecond,
                concurrencyLimit, timeoutMs, maxAttempts, number, createdAt, replayOfId);
    }
    @Override public String toString() { return "DeliveryCommand[deliveryId=" + deliveryId + ", attempt=" + attemptNumber + "]"; }
}

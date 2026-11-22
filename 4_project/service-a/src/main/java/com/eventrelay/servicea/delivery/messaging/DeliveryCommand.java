package com.eventrelay.servicea.delivery.messaging;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;

// {
//   "schemaVersion": 1,
//   "messageId": "11111111-1111-4111-8111-111111111111",
//   "deliveryId": "22222222-2222-4222-8222-222222222222",
//   "eventId": "33333333-3333-4333-8333-333333333333",
//   "applicationId": "44444444-4444-4444-8444-444444444444",
//   "endpointId": "55555555-5555-4555-8555-555555555555",
//   "eventType": "order.created",
//   "url": "https://customer.example/webhooks",
//   "body": "{\"id\":\"33333333-3333-4333-8333-333333333333\",\"type\":\"order.created\",\"createdAt\":\"2026-11-22T10:00:00Z\",\"data\":{\"orderId\":\"ORD-101\",\"amount\":500}}",
//   "signingSecretEncrypted": "<encrypted-signing-secret>",
//   "signingSecretVersion": 1,
//   "rateLimitPerSecond": 10,
//   "concurrencyLimit": 4,
//   "timeoutMs": 10000,
//   "maxAttempts": 5,
//   "attemptNumber": 1,
//   "createdAt": "2026-11-22T10:00:00Z",
//   "replayOfId": null
// }

public record DeliveryCommand(int schemaVersion, UUID messageId, UUID deliveryId, UUID eventId,
        UUID applicationId, UUID endpointId, String eventType, String url, String body,
        String signingSecretEncrypted, int signingSecretVersion, int rateLimitPerSecond,
        int concurrencyLimit, int timeoutMs, int maxAttempts, int attemptNumber, Instant createdAt, UUID replayOfId) 
{
    public DeliveryCommand(int schemaVersion, UUID messageId, UUID deliveryId, UUID eventId,
            UUID applicationId, UUID endpointId, String eventType, String url, String body,
            String signingSecretEncrypted, int signingSecretVersion, int rateLimitPerSecond,
            int concurrencyLimit, int timeoutMs, int maxAttempts, int attemptNumber, Instant createdAt) 
    {
        this(schemaVersion, messageId, deliveryId, eventId, applicationId, endpointId, eventType, url, body,
                signingSecretEncrypted, signingSecretVersion, rateLimitPerSecond, concurrencyLimit, timeoutMs,
                maxAttempts, attemptNumber, createdAt, null);
    }

    public void validate() {

        // currently only one type of contract is supported
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

    @Override
    public String toString() {
        return "DeliveryCommand[deliveryId=" + deliveryId + ", attempt=" + attemptNumber + "]";
    }
}

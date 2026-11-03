package com.eventrelay.servicea.messaging;

import java.time.Instant;
import java.util.UUID;

public record DeliveryStatusMessage(int schemaVersion, UUID messageId, UUID deliveryId, UUID eventId,
        UUID applicationId, UUID endpointId, String status, int attemptCount, long sequence,
        Integer httpStatus, String error, Instant nextAttemptAt, Instant updatedAt, Attempt attempt) {
    public DeliveryStatusMessage(int schemaVersion, UUID messageId, UUID deliveryId, UUID eventId,
        UUID applicationId, UUID endpointId, String status, int attemptCount, long sequence,
        Integer httpStatus, String error, Instant nextAttemptAt, Instant updatedAt) {
        this(schemaVersion, messageId, deliveryId, eventId, applicationId, endpointId, status, attemptCount, sequence, httpStatus, error, nextAttemptAt, updatedAt, null);
    }
    public record Attempt(int attemptNumber, Integer httpStatus, String error, Instant startedAt,
            Instant completedAt, String responseBody, String contentType, boolean responseTruncated) {}
    public void validate() {
        if (schemaVersion != 1 || messageId == null || deliveryId == null || eventId == null
                || applicationId == null || endpointId == null || updatedAt == null || sequence < 1
                || (error != null && error.length() > 1000) || (httpStatus != null && (httpStatus < 100 || httpStatus > 599))
                || attemptCount < 0 || attemptCount > 20 || status == null
                || !java.util.Set.of("SUCCEEDED", "RETRY_SCHEDULED", "DEAD").contains(status)) {
            throw new IllegalArgumentException("Invalid delivery status");
        }
        if (attempt != null && (attempt.attemptNumber() < 1 || attempt.attemptNumber() > attemptCount || attempt.startedAt() == null
                || attempt.completedAt() == null || attempt.completedAt().isBefore(attempt.startedAt())
                || (attempt.responseBody() != null && attempt.responseBody().length() > 4096)
                || (attempt.contentType() != null && attempt.contentType().length() > 256)
                || (attempt.error() != null && attempt.error().length() > 1000))) {
            throw new IllegalArgumentException("Invalid delivery attempt");
        }
    }
}

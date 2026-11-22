package com.eventrelay.servicea.monitoring.dto;

import java.time.Instant;
import java.util.UUID;

public record DeliverySummary(UUID id, UUID eventId, UUID endpointId, String eventType, String endpointUrl,
        String status, int attemptCount, Integer httpStatus, String error,
        Instant createdAt, Instant updatedAt, UUID replayOfId, UUID replayId, boolean canReplay,
        String replayReason) {
}

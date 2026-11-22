package com.eventrelay.servicea.event.dto;

import java.time.Instant;
import java.util.UUID;

public record EventDeliveryView(UUID id, UUID endpointId, String status, int attemptCount,
                Integer httpStatus, String error, Instant updatedAt, String endpointUrl, UUID replayOfId,
                UUID replayId) {
}

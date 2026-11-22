package com.eventrelay.servicea.event.dto;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record EventDetails(UUID id, UUID applicationId, String eventType, JsonNode payload,
                Instant createdAt, String status, List<EventDeliveryView> deliveries, UUID incomingRequestId) {
}

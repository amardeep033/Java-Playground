package com.eventrelay.servicea.eventtype.dto;

import com.eventrelay.servicea.eventtype.EventType;
import java.time.Instant;
import java.util.UUID;

public record EventTypeResponse(UUID id, UUID applicationId, String name, String description, Instant createdAt,
        boolean active) {
    public static EventTypeResponse from(EventType value) {
        return new EventTypeResponse(value.getId(), value.getApplicationId(), value.getName(), value.getDescription(),
                value.getCreatedAt(), value.isActive());
    }
}

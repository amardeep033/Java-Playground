package com.eventrelay.servicea.application.dto;

import com.eventrelay.servicea.application.Application;
import java.time.Instant;
import java.util.UUID;

public record ApplicationResponse(UUID id, UUID userId, String name, String description, Instant createdAt, boolean active,
        String clientRole, UUID clientId) {
    public static ApplicationResponse from(Application value) {
        return new ApplicationResponse(value.getId(), value.getUserId(), value.getName(), value.getDescription(),
                value.getCreatedAt(), value.isActive(), null, value.getClientId());
    }

    public ApplicationResponse withRole(String role) {
        return new ApplicationResponse(id, userId, name, description, createdAt, active, role, clientId);
    }
}

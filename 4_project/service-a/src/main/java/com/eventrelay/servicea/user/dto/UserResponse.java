package com.eventrelay.servicea.user.dto;

import com.eventrelay.servicea.user.UserAccount;
import java.time.Instant;
import java.util.UUID;

public record UserResponse(UUID id, String name, String email, Instant createdAt, String role, UUID clientId) {
    public static UserResponse from(UserAccount value) {
        return new UserResponse(value.getId(), value.getName(), value.getEmail(), value.getCreatedAt(), value.getRole(),
                value.getClientId());
    }
}

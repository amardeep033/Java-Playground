package com.eventrelay.servicea.apikey.dto;

import com.eventrelay.servicea.apikey.ApiKey;
import java.time.Instant;
import java.util.UUID;

public record ApiKeyDetails(UUID id, UUID applicationId, String name, String prefix,
        Instant createdAt, Instant revokedAt, UUID replacedById) {
    public static ApiKeyDetails from(ApiKey value) {
        return new ApiKeyDetails(value.getId(), value.getApplicationId(), value.getName(), value.getKeyPrefix(),
                value.getCreatedAt(), value.getRevokedAt(), value.getReplacedById());
    }
}

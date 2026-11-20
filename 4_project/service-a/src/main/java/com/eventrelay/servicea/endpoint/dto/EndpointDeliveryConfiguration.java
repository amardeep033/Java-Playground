package com.eventrelay.servicea.endpoint.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.constraints.*;

public record EndpointDeliveryConfiguration(@NotNull @Min(1) @Max(100000) Integer rateLimitPerSecond,
        @NotNull @Min(1) @Max(1000) Integer concurrencyLimit, @NotNull @Min(100) @Max(60000) Integer timeoutMs,
        @NotNull @Min(1) @Max(20) Integer maxAttempts) {
    @JsonAnySetter
    public void reject(String field, Object value) {
        throw new IllegalArgumentException("Only delivery limits are allowed; activation is admin-only");
    }
}

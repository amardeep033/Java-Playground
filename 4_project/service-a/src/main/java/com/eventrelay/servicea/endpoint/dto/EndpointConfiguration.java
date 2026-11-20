package com.eventrelay.servicea.endpoint.dto;

import jakarta.validation.constraints.*;

public record EndpointConfiguration(
        @NotNull Boolean enabled,
        @NotNull @Min(1) @Max(100000) Integer rateLimitPerSecond,
        @NotNull @Min(1) @Max(1000) Integer concurrencyLimit,
        @NotNull @Min(100) @Max(60000) Integer timeoutMs,
        @NotNull @Min(1) @Max(20) Integer maxAttempts) {
    public static EndpointConfiguration defaults() {
        return new EndpointConfiguration(true, 10, 4, 10000, 5);
    }
}

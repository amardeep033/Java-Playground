package com.eventrelay.servicea.endpoint.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.Set;
import java.util.UUID;

public record RegisterEndpoint(
        @Size(max = 120) String name,
        @NotBlank @Size(max = 2048) String url,
        @Size(max = 500) String description,
        @NotEmpty @Size(max = 100) Set<@NotNull UUID> eventTypeIds,
        @Valid EndpointConfiguration configuration) {
    public RegisterEndpoint(String url, String description, Set<UUID> eventTypeIds,
            EndpointConfiguration configuration) {
        this(null, url, description, eventTypeIds, configuration);
    }
}

package com.eventrelay.servicea.endpoint.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.Set;
import java.util.UUID;

public record EndpointSettings(@Size(min = 1, max = 2048) String url,
        @Size(min = 1, max = 100) Set<@NotNull UUID> eventTypeIds,
        @Valid EndpointConfiguration configuration) {
    @AssertTrue
    public boolean isValidUpdate() {
        return url != null || eventTypeIds != null || configuration != null;
    }

    @JsonAnySetter
    public void reject(String field, Object value) {
        throw new IllegalArgumentException("Only url, eventTypeIds and configuration can be updated");
    }
}

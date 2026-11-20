package com.eventrelay.servicea.endpoint.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.*;
import java.util.Set;
import java.util.UUID;

public record AdminEndpointUpdate(Boolean active, @Size(min = 1, max = 2048) String url,
        @Size(min = 1, max = 100) Set<@NotNull UUID> eventTypeIds) {
    @AssertTrue
    @JsonIgnore
    public boolean isValidUpdate() {
        return (active != null || url != null || eventTypeIds != null) && (url == null || !url.isBlank());
    }

    @JsonAnySetter
    public void reject(String field, Object value) {
        throw new IllegalArgumentException("Only active, url and eventTypeIds are allowed");
    }
}

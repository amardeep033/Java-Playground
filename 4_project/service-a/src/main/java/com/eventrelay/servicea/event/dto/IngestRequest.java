package com.eventrelay.servicea.event.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record IngestRequest(
                @NotBlank @Size(max = 120) @Pattern(regexp = "[a-zA-Z0-9_.-]+") String eventType,
                @NotNull JsonNode payload) {
}

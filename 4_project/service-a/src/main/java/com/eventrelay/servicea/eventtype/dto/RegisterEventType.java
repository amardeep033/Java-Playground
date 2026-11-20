package com.eventrelay.servicea.eventtype.dto;

import jakarta.validation.constraints.*;

public record RegisterEventType(
        @NotBlank @Size(max = 120) @Pattern(regexp = "[a-zA-Z0-9_.-]+") String name,
        @Size(max = 500) String description) {
}

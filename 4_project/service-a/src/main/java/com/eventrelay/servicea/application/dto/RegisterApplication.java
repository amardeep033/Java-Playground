package com.eventrelay.servicea.application.dto;

import jakarta.validation.constraints.*;
import java.util.UUID;

public record RegisterApplication(@NotBlank @Size(max = 120) String name, UUID clientId) {
    public RegisterApplication(String name) {
        this(name, null);
    }
}

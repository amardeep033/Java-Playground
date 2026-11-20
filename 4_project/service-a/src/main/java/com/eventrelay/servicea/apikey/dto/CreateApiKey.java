package com.eventrelay.servicea.apikey.dto;

import jakarta.validation.constraints.*;

public record CreateApiKey(@NotBlank @Size(max = 120) String name) {
}

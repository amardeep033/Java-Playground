package com.eventrelay.servicea.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;

public record Signup(@NotBlank @Size(max = 120) String name, @NotBlank @Size(max = 120) String companyName,
        @NotBlank @Email @Size(max = 320) String email,
        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) @NotBlank @Size(min = 12, max = 128) String password) {
    @Override
    public String toString() {
        return "Signup[credentials redacted]";
    }
}

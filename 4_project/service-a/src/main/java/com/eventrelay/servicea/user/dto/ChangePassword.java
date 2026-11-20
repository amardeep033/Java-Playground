package com.eventrelay.servicea.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;

public record ChangePassword(
        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) @NotBlank @Size(max = 128) String currentPassword,
        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) @NotBlank @Size(min = 12, max = 128) String newPassword) {
    @Override
    public String toString() {
        return "ChangePassword[credentials redacted]";
    }
}

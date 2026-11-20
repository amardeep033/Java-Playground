package com.eventrelay.servicea.workspace.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;

public record ResetPassword(
        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) @NotBlank @Size(min = 12, max = 128) String newPassword) {
    @Override
    public String toString() {
        return "ResetPassword[redacted]";
    }
}

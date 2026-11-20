package com.eventrelay.servicea.workspace.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import java.util.Set;
import java.util.UUID;

public record ClientUser(@NotBlank @Size(max = 120) String name, @NotBlank @Email @Size(max = 320) String email,
        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) @NotBlank @Size(min = 12, max = 128) String password,
        @NotBlank @Pattern(regexp = "USER|ADMIN") String role, @Size(max = 100) Set<@NotNull UUID> applicationIds) {
    @Override
    public String toString() {
        return "ClientUser[credentials redacted]";
    }
}

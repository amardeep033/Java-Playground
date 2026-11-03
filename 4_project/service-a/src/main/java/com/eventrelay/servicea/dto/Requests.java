package com.eventrelay.servicea.dto;
import java.util.Set;
import java.util.UUID;
import jakarta.validation.constraints.*;
import com.fasterxml.jackson.annotation.JsonProperty;
public final class Requests {
    public record EndpointConfiguration(
            @NotNull Boolean enabled,
            @NotNull @Min(1) @Max(100000) Integer rateLimitPerSecond,
            @NotNull @Min(1) @Max(1000) Integer concurrencyLimit,
            @NotNull @Min(100) @Max(60000) Integer timeoutMs,
            @NotNull @Min(1) @Max(20) Integer maxAttempts) {
        public static EndpointConfiguration defaults() { return new EndpointConfiguration(true, 10, 4, 10000, 5); }
    }

    private Requests() {}
    public record RegisterUser(
            @NotBlank @Size(max = 120) String name,
            @NotBlank @Email @Size(max = 320) String email,
            @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
            @NotBlank @Size(min = 12, max = 128) String password) {
        @Override public String toString() { return "RegisterUser[credentials redacted]"; }
    }

    public record ChangePassword(
            @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) @NotBlank @Size(max = 128) String currentPassword,
            @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) @NotBlank @Size(min = 12, max = 128) String newPassword) {
        @Override public String toString() { return "ChangePassword[credentials redacted]"; }
    }

    public record CreateApiKey(@NotBlank @Size(max = 120) String name) {}

    public record RegisterApplication(@NotBlank @Size(max = 120) String name) {}

    public record RegisterEventType(
            @NotBlank @Size(max = 120) @Pattern(regexp = "[a-zA-Z0-9_.-]+") String name,
            @Size(max = 500) String description) {}
            
    public record RegisterEndpoint(
            @NotBlank @Size(max = 2048) String url,
            @Size(max = 500) String description,
            @NotEmpty @Size(max = 100) Set<@NotNull UUID> eventTypeIds,
            @jakarta.validation.Valid EndpointConfiguration configuration) {}
}

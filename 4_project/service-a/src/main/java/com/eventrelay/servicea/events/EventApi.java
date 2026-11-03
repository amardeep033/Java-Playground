package com.eventrelay.servicea.events;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.*;

public final class EventApi {
    private EventApi() {}
    public record IngestRequest(
            @NotBlank @Size(max = 120) @Pattern(regexp = "[a-zA-Z0-9_.-]+") String eventType,
            @NotNull JsonNode payload) {}
    public record Accepted(UUID id, String eventType, int deliveryCount, Instant createdAt) {}
    public record DeliveryView(UUID id, UUID endpointId, String status, int attemptCount,
                               Integer httpStatus, String error, Instant nextAttemptAt, Instant updatedAt, String endpointUrl, UUID replayOfId, UUID replayId) {}
    public record Details(UUID id, UUID applicationId, String eventType, JsonNode payload,
                          Instant createdAt, String status, List<DeliveryView> deliveries) {}
}

package com.eventrelay.servicea.monitoring;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import com.fasterxml.jackson.databind.JsonNode;

public final class MonitoringApi {
    private MonitoringApi() {}
    public record EventSummary(UUID id, String eventType, int deliveryCount, String status, Instant createdAt) {}
    public record DeliverySummary(UUID id, UUID eventId, UUID endpointId, String eventType, String endpointUrl,
            String status, int attemptCount, Integer httpStatus, String error,
            Instant createdAt, Instant updatedAt, UUID replayOfId, UUID replayId, boolean canReplay, String replayReason) {}
    public record Attempt(int attemptNumber, Integer httpStatus, String error, Instant startedAt, Instant completedAt,
            long durationMs, String responseBody, String contentType, boolean responseTruncated) {}
    public record DeliveryDetails(DeliverySummary delivery, JsonNode requestBody, List<Attempt> attempts) {}
    public record ReplayReceipt(UUID id, UUID eventId, UUID replayOfId, Instant createdAt) {}
    public record Summary(long events, long deliveries, long succeeded, long pending, long failed, long replays) {}
}

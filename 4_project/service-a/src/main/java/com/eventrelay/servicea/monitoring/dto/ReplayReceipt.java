package com.eventrelay.servicea.monitoring.dto;

import java.time.Instant;
import java.util.UUID;

public record ReplayReceipt(UUID id, UUID eventId, UUID replayOfId, Instant createdAt) {
}

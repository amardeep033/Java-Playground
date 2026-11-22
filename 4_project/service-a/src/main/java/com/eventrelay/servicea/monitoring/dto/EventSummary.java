package com.eventrelay.servicea.monitoring.dto;

import java.time.Instant;
import java.util.UUID;

public record EventSummary(UUID id, String eventType, int deliveryCount, String status, Instant createdAt) {
}

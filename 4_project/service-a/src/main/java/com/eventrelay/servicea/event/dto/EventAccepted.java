package com.eventrelay.servicea.event.dto;

import java.time.Instant;
import java.util.UUID;

public record EventAccepted(UUID id, String eventType, int deliveryCount, Instant createdAt) {
}

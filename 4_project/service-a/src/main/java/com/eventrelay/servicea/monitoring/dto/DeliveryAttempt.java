package com.eventrelay.servicea.monitoring.dto;

import java.time.Instant;

public record DeliveryAttempt(int attemptNumber, Integer httpStatus, String error, Instant startedAt,
        Instant completedAt,
        long durationMs, String responseBody, String contentType, boolean responseTruncated) {
}

package com.eventrelay.servicea.monitoring.dto;

public record MonitoringSummary(long events, long deliveries, long succeeded, long pending, long failed, long replays) {
}

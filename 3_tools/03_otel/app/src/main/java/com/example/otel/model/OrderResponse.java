package com.example.otel.model;

import io.opentelemetry.api.trace.Span;
import org.slf4j.MDC;

public record OrderResponse(String message, String requestId, String traceId, String correlationId) {
    public static OrderResponse of(String message) {
        return new OrderResponse(
            message, // Received from the controller.
            MDC.get("requestId"),
            Span.current().getSpanContext().getTraceId(), // Also present in MDC as traceId, set by our filter.
            MDC.get("correlationId"));
    }
}

package com.eventrelay.servicea.ingress.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record IncomingRequestEntry(UUID id, UUID applicationId, String requestedApplicationId, Instant receivedAt,
        long durationMs,
        int httpStatus, String outcome, String reasonCode, String reason, UUID eventId, String eventType,
        String authMethod, boolean idempotencyKeyPresent, String clientIp, List<UUID> skippedEndpoints, UUID apiKeyId,
        String apiKeyName, UUID duplicateOfId) {
}

// id --- submission attempt
// applicationId --- found in db
// requestedApplicationId --- supplied in URL
// receivedAt --- 
// durationMs --- total processing time
// httpStatus --- `202` accepted, `401` unauthorized, or `409` conflict (Initially `102` while processing)
// outcome --- `PROCESSING`, `ACCEPTED`, `REJECTED`, `ERROR`
// reasonCode --- `INVALID_JWT`, `IDEMPOTENCY_CONFLICT`
// reason --- 
// *eventId --- Accepted event’s ID
// eventType --- 
// authMethod --- `DASHBOARD`, `API_KEY`, `NONE`
// idempotencyKeyPresent --- boolean
// clientIp --- Remote IP address
// skippedEndpoints --- IDs of inactive subscribed endpoints
// apiKeyId --- 
// apiKeyName --- 
// duplicateOfId --- ID of the original ingress audit entry

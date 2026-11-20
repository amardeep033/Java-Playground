package com.eventrelay.servicea.ingress.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record IncomingRequestEntry(UUID id,UUID applicationId,String requestedApplicationId,Instant receivedAt,long durationMs,
                        int httpStatus,String outcome,String reasonCode,String reason,UUID eventId,String eventType,
                        String authMethod,boolean idempotencyKeyPresent,String clientIp,List<UUID> skippedEndpoints,UUID apiKeyId,String apiKeyName,UUID duplicateOfId) {}

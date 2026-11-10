package com.eventrelay.servicea.events;

import java.util.UUID;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/applications/{applicationId}/events")
public class EventController {
    private final EventIngestionService service;
    public EventController(EventIngestionService service) { this.service = service; }
    // One POST path for both callers: customer servers send X-API-Key;
    // the dashboard omits it and sends its HttpOnly JWT cookie plus a CSRF header.
    // Both use this path. JWT verification is stateless; API-key revocation does
    // not block an authorized dashboard user, and invalid keys never fall back to JWT.
    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public EventApi.Accepted receive(@PathVariable UUID applicationId,
            @RequestHeader(value = "X-API-Key", required = false) String apiKey,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody EventApi.IngestRequest request) {
        return service.receive(applicationId, apiKey, idempotencyKey, request);
    }
    @GetMapping("/{eventId}")
    public EventApi.Details details(@PathVariable UUID applicationId, @PathVariable UUID eventId,
            @RequestHeader(value = "X-API-Key", required = false) String apiKey,
            @RequestParam(defaultValue="true") boolean includeDeliveries) {
        return service.details(applicationId, eventId, apiKey, includeDeliveries);
    }
}

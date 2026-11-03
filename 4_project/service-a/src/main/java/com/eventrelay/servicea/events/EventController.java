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

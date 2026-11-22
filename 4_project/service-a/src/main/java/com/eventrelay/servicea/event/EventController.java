package com.eventrelay.servicea.event;

import com.eventrelay.servicea.event.dto.EventAccepted;
import com.eventrelay.servicea.event.dto.EventDetails;
import com.eventrelay.servicea.event.dto.IngestRequest;

import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/applications/{applicationId}/events")
public class EventController {
    // request dto: IngestRequest
    // response dto: EventAccepted, EventDetails

    private final EventIngestionService service;

    public EventController(EventIngestionService service) {
        this.service = service;
    }

    // ---------------------------------------------------------------------

    // One POST path for both callers: customer servers send X-API-Key
    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public EventAccepted receive(@PathVariable("applicationId") UUID app_id,
            @RequestHeader(value = "X-API-Key", required = false) String api_key,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotency_key,
            @Valid @RequestBody IngestRequest request) {
        return service.receive(app_id, api_key, idempotency_key, request);
    }

    @GetMapping("/{eventId}")
    public EventDetails details(@PathVariable("applicationId") UUID app_id, @PathVariable("eventId") UUID event_id,
            @RequestHeader(value = "X-API-Key", required = false) String api_key,
            @RequestParam(name = "includeDeliveries", defaultValue = "true") boolean include_deliveries) {
        return service.details(app_id, event_id, api_key, include_deliveries);
    }

    // List all part of MonitoringController

}

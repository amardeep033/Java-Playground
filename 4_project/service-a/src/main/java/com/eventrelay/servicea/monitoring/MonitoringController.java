package com.eventrelay.servicea.monitoring;

import com.eventrelay.servicea.monitoring.dto.DeliveryDetails;
import com.eventrelay.servicea.monitoring.dto.DeliverySummary;
import com.eventrelay.servicea.monitoring.dto.EventSummary;
import com.eventrelay.servicea.monitoring.dto.MonitoringSummary;
import com.eventrelay.servicea.monitoring.dto.ReplayReceipt;

import com.eventrelay.servicea.common.dto.PageResult;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/applications/{applicationId}")
public class MonitoringController {
    // request dto: none (query/path parameters and headers only)
    // response dto: PageResult<EventSummary>, PageResult<DeliverySummary>, DeliveryDetails, MonitoringSummary, ReplayReceipt

    private final MonitoringService service;

    public MonitoringController(MonitoringService service) {
        this.service = service;
    }

    // ----------------------------------------------------------------------------

    // list events(which was missing in events)
    @GetMapping("/events")
    public PageResult<EventSummary> events(@PathVariable("applicationId") UUID app_id,
            @RequestHeader(value = "X-API-Key", required = false) String api_key,
            @RequestParam(defaultValue = "") String status,
            @RequestParam(defaultValue = "") String q, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.events(app_id, api_key, status, q, page, size);
    }

    // ----------------------------------------------------------------------------

    // list of all deliveries 
    @GetMapping("/deliveries")
    public PageResult<DeliverySummary> deliveries(@PathVariable("applicationId") UUID app_id,
            @RequestHeader(value = "X-API-Key", required = false) String api_key,
            @RequestParam(defaultValue = "") String status,
            @RequestParam(defaultValue = "") String q, @RequestParam(name = "eventId", required = false) UUID event_id,
            @RequestParam(name = "endpointId", required = false) UUID endpoint_id,
            @RequestParam(name = "includeReplayed", defaultValue = "true") boolean include_replayed, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.deliveries(app_id, api_key, status, q, event_id, endpoint_id, include_replayed, page, size);
    }

    // specific delivery detail 
    @GetMapping("/deliveries/{deliveryId}")
    public DeliveryDetails delivery(@PathVariable("applicationId") UUID app_id, @PathVariable("deliveryId") UUID delivery_id,
            @RequestHeader(value = "X-API-Key", required = false) String api_key) {
        return service.delivery(app_id, delivery_id, api_key);
    }

    // replay from dlq
    @PostMapping("/deliveries/{deliveryId}/replay")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ReplayReceipt replay(@PathVariable("applicationId") UUID app_id, @PathVariable("deliveryId") UUID delivery_id,
            @RequestHeader(value = "X-API-Key", required = false) String api_key,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotency_key) {
        return service.replay(app_id, delivery_id, api_key, idempotency_key);
    }

    // ----------------------------------------------------------------------------

    // counters at app level
    @GetMapping("/activity-summary")
    public MonitoringSummary summary(@PathVariable("applicationId") UUID app_id,
            @RequestHeader(value = "X-API-Key", required = false) String api_key) {
        return service.summary(app_id, api_key);
    }

}

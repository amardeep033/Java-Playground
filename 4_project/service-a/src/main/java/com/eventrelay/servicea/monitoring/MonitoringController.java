package com.eventrelay.servicea.monitoring;

import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import com.eventrelay.servicea.dto.Responses.PageResult;

@RestController
@RequestMapping("/api/v1/applications/{applicationId}")
public class MonitoringController {
    private final MonitoringService service;
    public MonitoringController(MonitoringService service) { this.service=service; }
    @GetMapping("/events")
    public PageResult<MonitoringApi.EventSummary> events(@PathVariable UUID applicationId,
            @RequestHeader(value="X-API-Key",required=false)String key,@RequestParam(defaultValue="")String status,
            @RequestParam(defaultValue="")String q,@RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="20")int size) {
        return service.events(applicationId,key,status,q,page,size);
    }
    @GetMapping("/deliveries")
    public PageResult<MonitoringApi.DeliverySummary> deliveries(@PathVariable UUID applicationId,
            @RequestHeader(value="X-API-Key",required=false)String key,@RequestParam(defaultValue="")String status,
            @RequestParam(defaultValue="")String q,@RequestParam(required=false)UUID eventId,@RequestParam(required=false)UUID endpointId,
            @RequestParam(defaultValue="true")boolean includeReplayed,@RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="20")int size) {
        return service.deliveries(applicationId,key,status,q,eventId,endpointId,includeReplayed,page,size);
    }
    @GetMapping("/deliveries/{deliveryId}")
    public MonitoringApi.DeliveryDetails delivery(@PathVariable UUID applicationId,@PathVariable UUID deliveryId,
            @RequestHeader(value="X-API-Key",required=false)String key) { return service.delivery(applicationId,deliveryId,key); }
    @GetMapping("/activity-summary")
    public MonitoringApi.Summary summary(@PathVariable UUID applicationId,@RequestHeader(value="X-API-Key",required=false)String key) { return service.summary(applicationId,key); }
    @PostMapping("/deliveries/{deliveryId}/replay")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public MonitoringApi.ReplayReceipt replay(@PathVariable UUID applicationId,@PathVariable UUID deliveryId,
            @RequestHeader(value="X-API-Key",required=false)String key,@RequestHeader(value="Idempotency-Key",required=false)String idempotencyKey) {
        return service.replay(applicationId,deliveryId,key,idempotencyKey);
    }
}

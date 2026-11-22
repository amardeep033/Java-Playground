package com.eventrelay.servicea.eventtype;

import com.eventrelay.servicea.common.dto.PageResult;
import com.eventrelay.servicea.eventtype.dto.Activation;
import com.eventrelay.servicea.eventtype.dto.EventTypeResponse;
import com.eventrelay.servicea.eventtype.dto.RegisterEventType;
import com.eventrelay.servicea.eventtype.dto.UpdateDescription;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/applications/{applicationId}/event-types")
public class EventTypeController {
    //request dto: RegisterEventType, UpdateDescription, Activation
    //response dto: EventTypeResponse, PageResult<EventTypeResponse>

    private final EventTypeService service;

    public EventTypeController(EventTypeService service) {
        this.service = service;
    }

    //------------------------------------------------

    @PostMapping("")
    @ResponseStatus(HttpStatus.CREATED)
    public EventTypeResponse register(@PathVariable("applicationId") UUID app_id, @Valid @RequestBody RegisterEventType request) {
        return service.registerEventType(app_id, request);
    }

    @GetMapping("")
    public PageResult<EventTypeResponse> list(@PathVariable("applicationId") UUID app_id,
            @RequestParam(defaultValue = "") String q, @RequestParam(defaultValue = "") String status,
            @PageableDefault(sort = "createdAt") Pageable pageable) {
        return service.listEventTypes(app_id, q, status, pageable);
    }

    @GetMapping("/{eventTypeId}")
    public EventTypeResponse get(@PathVariable("applicationId") UUID app_id, @PathVariable("eventTypeId") UUID event_type_id) {
        return service.getEventType(app_id, event_type_id);
    }

    @PutMapping("/{eventTypeId}")
    public EventTypeResponse update(@PathVariable("applicationId") UUID app_id, @PathVariable("eventTypeId") UUID event_type_id,
            @Valid @RequestBody UpdateDescription request) {
        return service.updateEventType(app_id, event_type_id, request);
    }

    @PutMapping("/{eventTypeId}/activation")
    public EventTypeResponse activation(@PathVariable("applicationId") UUID app_id, @PathVariable("eventTypeId") UUID event_type_id,
            @Valid @RequestBody Activation request) {
        return service.setEventTypeActive(app_id, event_type_id, request.active());
    }

    @DeleteMapping("/{eventTypeId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable("applicationId") UUID app_id, @PathVariable("eventTypeId") UUID event_type_id) {
        service.deleteEventType(app_id, event_type_id);
    }
    
}

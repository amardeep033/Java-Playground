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
    public EventTypeResponse register(@PathVariable UUID applicationId, @Valid @RequestBody RegisterEventType request) {
        return service.registerEventType(applicationId, request);
    }

    @GetMapping("")
    public PageResult<EventTypeResponse> list(@PathVariable UUID applicationId,
            @RequestParam(defaultValue = "") String q, @RequestParam(defaultValue = "") String status,
            @PageableDefault(sort = "createdAt") Pageable pageable) {
        return service.listEventTypes(applicationId, q, status, pageable);
    }

    @GetMapping("/{eventTypeId}")
    public EventTypeResponse get(@PathVariable UUID applicationId, @PathVariable UUID eventTypeId) {
        return service.getEventType(applicationId, eventTypeId);
    }

    @PutMapping("/{eventTypeId}")
    public EventTypeResponse update(@PathVariable UUID applicationId, @PathVariable UUID eventTypeId,
            @Valid @RequestBody UpdateDescription request) {
        return service.updateEventType(applicationId, eventTypeId, request);
    }

    @PutMapping("/{eventTypeId}/activation")
    public EventTypeResponse activation(@PathVariable UUID applicationId, @PathVariable UUID eventTypeId,
            @Valid @RequestBody Activation request) {
        return service.setEventTypeActive(applicationId, eventTypeId, request.active());
    }

    @DeleteMapping("/{eventTypeId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID applicationId, @PathVariable UUID eventTypeId) {
        service.deleteEventType(applicationId, eventTypeId);
    }
    
}

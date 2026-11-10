package com.eventrelay.servicea.controller;
import java.util.UUID;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import com.eventrelay.servicea.dto.Requests.*;
import com.eventrelay.servicea.dto.Responses;
import com.eventrelay.servicea.service.RegistrationService;

@RestController
@RequestMapping("/api/v1/applications/{applicationId}/event-types")
public class EventTypeController {
    private final RegistrationService service;
    private final com.eventrelay.servicea.service.SoftDeletionService deletion;
    public EventTypeController(RegistrationService service, com.eventrelay.servicea.service.SoftDeletionService deletion) { this.service = service; this.deletion = deletion; }

    @PostMapping("")
    @ResponseStatus(HttpStatus.CREATED)
    public Responses.Event register(@PathVariable UUID applicationId, @Valid @RequestBody RegisterEventType request) {
        return service.registerEventType(applicationId, request);
    }

    @GetMapping("")
    public Responses.PageResult<Responses.Event> list(@PathVariable UUID applicationId, @RequestParam(defaultValue="") String q, @RequestParam(defaultValue="") String status, @PageableDefault(sort = "createdAt") Pageable pageable) {
        return service.listEventTypes(applicationId, q, status, pageable);
    }
    
    @GetMapping("/{eventTypeId}")
    public Responses.Event get(@PathVariable UUID applicationId, @PathVariable UUID eventTypeId) {
        return service.getEventType(applicationId, eventTypeId);
    }

    @DeleteMapping("/{eventTypeId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID applicationId, @PathVariable UUID eventTypeId) {
        deletion.deleteEventType(applicationId, eventTypeId);
    }
    @PutMapping("/{eventTypeId}")
    public Responses.Event update(@PathVariable UUID applicationId, @PathVariable UUID eventTypeId, @Valid @RequestBody UpdateDescription request) {
        return service.updateEventType(applicationId, eventTypeId, request);
    }

    @PutMapping("/{eventTypeId}/activation")
    public Responses.Event activation(@PathVariable UUID applicationId,@PathVariable UUID eventTypeId,@Valid @RequestBody Activation request) {
        return service.setEventTypeActive(applicationId,eventTypeId,request.active());
    }
}

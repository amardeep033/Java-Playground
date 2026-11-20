package com.eventrelay.servicea.eventtype;

import com.eventrelay.servicea.application.Application;
import com.eventrelay.servicea.auth.access.ResourceAccess;
import com.eventrelay.servicea.common.dto.PageResult;
import com.eventrelay.servicea.common.search.ListSearch;
import com.eventrelay.servicea.eventtype.dto.EventTypeResponse;
import com.eventrelay.servicea.eventtype.dto.RegisterEventType;
import com.eventrelay.servicea.eventtype.dto.UpdateDescription;
import com.eventrelay.servicea.workspace.AuditLogs;
import java.util.*;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import static com.eventrelay.servicea.common.search.ListSearch.activationFilter;

@Service
@Transactional(readOnly = true)
public class EventTypeService {
    private final EventTypeRepository eventTypes;
    private final ResourceAccess access;
    private final AuditLogs audit;

    public EventTypeService(EventTypeRepository eventTypes, ResourceAccess access, AuditLogs audit) {
        this.eventTypes = eventTypes;
        this.access = access;
        this.audit = audit;
    }

    @Transactional
    public EventTypeResponse registerEventType(UUID applicationId, RegisterEventType request) {
        access.lockedApplication(applicationId);
        var event = new EventType(applicationId, request.name(), request.description());
        return EventTypeResponse.from(eventTypes.saveAndFlush(event));
    }

    public EventTypeResponse getEventType(UUID applicationId, UUID eventTypeId) {
        requireApplication(applicationId);
        return EventTypeResponse.from(eventTypes.findByIdAndApplicationId(eventTypeId, applicationId)
                .orElseThrow(() -> notFound("Event type")));
    }

    @Transactional
    public EventTypeResponse updateEventType(UUID appId, UUID id, UpdateDescription request) {
        access.lockedApplication(appId);
        var event = eventTypes.findByIdAndApplicationId(id, appId).orElseThrow(() -> notFound("Event type"));
        event.updateDescription(request.description());
        return EventTypeResponse.from(event);
    }

    @Transactional
    public EventTypeResponse setEventTypeActive(UUID appId, UUID id, boolean active) {
        access.lockedApplication(appId);
        access.workspaceAdmin(appId);
        var type = eventTypes.findByIdAndApplicationId(id, appId).orElseThrow(() -> notFound("Event type"));
        audit.changed(appId, "event_types", id, "is_active", type.isActive(), active);
        type.setActive(active);
        return EventTypeResponse.from(type);
    }

    public PageResult<EventTypeResponse> listEventTypes(UUID appId, String q, Pageable page) {
        return listEventTypes(appId, q, "", page);
    }

    public PageResult<EventTypeResponse> listEventTypes(UUID appId, String q, String status, Pageable page) {
        requireApplication(appId);
        return PageResult.from(eventTypes.findAll(activationFilter(
                ListSearch.<EventType>scoped("applicationId", appId, q, "name", "description"), status, "active"), page)
                .map(EventTypeResponse::from));
    }

    private Application requireApplication(UUID id) {
        return access.application(id);
    }

    private static ResponseStatusException notFound(String resource) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, resource + " not found");
    }

    @Transactional
    public void deleteEventType(UUID appId, UUID eventId) {
        access.lockedApplication(appId);
        eventTypes.findByIdAndApplicationId(eventId, appId).orElseThrow(() -> ResourceAccess.notFound("Event type"))
                .softDelete();
    }

}

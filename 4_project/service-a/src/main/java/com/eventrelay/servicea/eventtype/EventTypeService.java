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

    public EventTypeService(EventTypeRepository event_types, ResourceAccess access, AuditLogs audit) {
        this.eventTypes = event_types;
        this.access = access;
        this.audit = audit;
    }

    @Transactional
    public EventTypeResponse registerEventType(UUID app_id, RegisterEventType request) {
        access.lockedApplication(app_id);
        var event = new EventType(app_id, request.name(), request.description());
        return EventTypeResponse.from(eventTypes.saveAndFlush(event));
    }

    public EventTypeResponse getEventType(UUID app_id, UUID event_type_id) {
        requireApplication(app_id);
        return EventTypeResponse.from(eventTypes.findByIdAndApplicationId(event_type_id, app_id)
                .orElseThrow(() -> notFound("Event type")));
    }

    @Transactional
    public EventTypeResponse updateEventType(UUID app_id, UUID event_type_id, UpdateDescription request) {
        access.lockedApplication(app_id);
        var event = eventTypes.findByIdAndApplicationId(event_type_id, app_id).orElseThrow(() -> notFound("Event type"));
        event.updateDescription(request.description());
        return EventTypeResponse.from(event);
    }

    @Transactional
    public EventTypeResponse setEventTypeActive(UUID app_id, UUID event_type_id, boolean active) {
        access.lockedApplication(app_id);
        access.workspaceAdmin(app_id);
        var type = eventTypes.findByIdAndApplicationId(event_type_id, app_id).orElseThrow(() -> notFound("Event type"));
        audit.changed(app_id, "event_types", event_type_id, "is_active", type.isActive(), active);
        type.setActive(active);
        return EventTypeResponse.from(type);
    }

    public PageResult<EventTypeResponse> listEventTypes(UUID app_id, String q, Pageable page) {
        return listEventTypes(app_id, q, "", page);
    }

    public PageResult<EventTypeResponse> listEventTypes(UUID app_id, String q, String status, Pageable page) {
        requireApplication(app_id);
        return PageResult.from(eventTypes.findAll(activationFilter(
                ListSearch.<EventType>scoped("applicationId", app_id, q, "name", "description"), status, "active"), page)
                .map(EventTypeResponse::from));
    }

    private Application requireApplication(UUID app_id) {
        return access.application(app_id);
    }

    private static ResponseStatusException notFound(String resource) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, resource + " not found");
    }

    @Transactional
    public void deleteEventType(UUID app_id, UUID event_id) {
        access.lockedApplication(app_id);
        eventTypes.findByIdAndApplicationId(event_id, app_id).orElseThrow(() -> ResourceAccess.notFound("Event type"))
                .softDelete();
    }

}

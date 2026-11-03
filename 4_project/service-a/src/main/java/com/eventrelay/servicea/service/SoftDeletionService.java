package com.eventrelay.servicea.service;

import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.eventrelay.servicea.entity.Application;
import com.eventrelay.servicea.repository.*;

@Service
@Transactional
public class SoftDeletionService {
    private final ResourceAccess access;
    private final ApplicationRepository applications;
    private final EventTypeRepository events;
    private final EndpointRepository endpoints;
    private final ApiKeyRepository keys;

    public SoftDeletionService(ResourceAccess access, ApplicationRepository applications,
                               EventTypeRepository events, EndpointRepository endpoints, ApiKeyRepository keys) {
        this.access = access;
        this.applications = applications;
        this.events = events;
        this.endpoints = endpoints;
        this.keys = keys;
    }
    public void deleteUser(UUID userId) {
        var user = access.lockedUser(userId);
        for (var app : applications.findAllByUserId(userId)) {
            deleteApplicationTree(access.lockedApplication(app.getId()));
        }
        user.softDelete();
    }
    public void deleteApplication(UUID userId, UUID appId) {
        var app = access.lockedApplication(appId);
        if (!app.getUserId().equals(userId)) throw ResourceAccess.notFound("Application");
        deleteApplicationTree(app);
    }
    public void deleteEventType(UUID appId, UUID eventId) {
        access.lockedApplication(appId);
        events.findByIdAndApplicationId(eventId, appId).orElseThrow(() -> ResourceAccess.notFound("Event type")).softDelete();
    }
    public void deleteEndpoint(UUID appId, UUID endpointId) {
        access.lockedApplication(appId);
        endpoints.findByIdAndApplicationId(endpointId, appId).orElseThrow(() -> ResourceAccess.notFound("Endpoint")).softDelete();
    }
    private void deleteApplicationTree(Application app) {
        Instant now = Instant.now();
        endpoints.softDeleteByApplicationId(app.getId(), now);
        events.softDeleteByApplicationId(app.getId(), now);
        keys.softDeleteByApplicationId(app.getId(), now);
        app.softDelete();
    }
}

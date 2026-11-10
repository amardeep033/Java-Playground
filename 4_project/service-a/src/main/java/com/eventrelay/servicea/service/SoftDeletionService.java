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

    private final org.springframework.jdbc.core.JdbcTemplate db;
    public SoftDeletionService(ResourceAccess access, ApplicationRepository applications,
                               EventTypeRepository events, EndpointRepository endpoints, ApiKeyRepository keys,org.springframework.jdbc.core.JdbcTemplate db) {
        this.db=db;
        this.access = access;
        this.applications = applications;
        this.events = events;
        this.endpoints = endpoints;
        this.keys = keys;
    }
    public void deleteUser(UUID userId) {
        access.account(userId,false);
        UUID client=db.queryForObject("SELECT client_id FROM service_a.users WHERE id=?",UUID.class,userId);
        db.queryForList("SELECT id FROM service_a.clients WHERE id=? FOR UPDATE",UUID.class,client);
        var user=access.lockedUser(userId);
        if("ADMIN".equals(user.getRole())&&!Boolean.TRUE.equals(db.queryForObject("SELECT EXISTS(SELECT 1 FROM service_a.users WHERE client_id=? AND id<>? AND role='ADMIN' AND deleted_at IS NULL)",Boolean.class,client,userId)))
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT,"Keep at least one company ADMIN");
        db.update("UPDATE service_a.refresh_token_families SET revoked_at=now(),revocation_reason='ACCOUNT_DELETED' WHERE user_id=? AND revoked_at IS NULL",userId);
        db.update("UPDATE service_a.workspace_members SET deleted_at=now() WHERE user_id=? AND deleted_at IS NULL",userId);
        db.update("INSERT INTO service_a.audit_logs(id,client_id,actor_id,entity_type,entity_id,action,details) VALUES (?,?,?,'users',?,'DELETED','{}'::jsonb)",UUID.randomUUID(),client,com.eventrelay.servicea.auth.JwtUser.current().id(),userId);
        user.softDelete();
    }
    public void deleteApplication(UUID userId, UUID appId) {
        var app = access.lockedApplication(appId);
        var viewer=com.eventrelay.servicea.auth.JwtUser.current();
        if (!app.getUserId().equals(userId)&&(viewer==null||!viewer.id().equals(userId)&&!viewer.admin())) throw ResourceAccess.notFound("Application");
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

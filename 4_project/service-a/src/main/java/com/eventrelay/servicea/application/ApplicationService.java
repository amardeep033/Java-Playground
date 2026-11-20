package com.eventrelay.servicea.application;

import com.eventrelay.servicea.apikey.ApiKeyRepository;
import com.eventrelay.servicea.application.dto.ApplicationResponse;
import com.eventrelay.servicea.application.dto.RegisterApplication;
import com.eventrelay.servicea.application.dto.UpdateApplication;
import com.eventrelay.servicea.auth.JwtUser;
import com.eventrelay.servicea.auth.access.ResourceAccess;
import com.eventrelay.servicea.common.dto.PageResult;
import com.eventrelay.servicea.common.search.ListSearch;
import com.eventrelay.servicea.endpoint.EndpointRepository;
import com.eventrelay.servicea.eventtype.EventTypeRepository;
import com.eventrelay.servicea.user.UserAccount;
import com.eventrelay.servicea.workspace.AuditLogs;
import java.time.Instant;
import java.util.*;
import org.hibernate.query.criteria.JpaExpression;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import static com.eventrelay.servicea.common.search.ListSearch.activationFilter;

@Service
@Transactional(readOnly = true)
public class ApplicationService {
    private final ApplicationRepository applications;
    private final EndpointRepository endpoints;
    private final EventTypeRepository events;
    private final ApiKeyRepository keys;
    private final ResourceAccess access;
    private final AuditLogs audit;
    private final JdbcTemplate db;

    public ApplicationService(ApplicationRepository applications, EndpointRepository endpoints,
            EventTypeRepository events, ApiKeyRepository keys, ResourceAccess access, AuditLogs audit,
            JdbcTemplate db) {
        this.applications = applications;
        this.endpoints = endpoints;
        this.events = events;
        this.keys = keys;
        this.access = access;
        this.audit = audit;
        this.db = db;
    }

    @Transactional
    public ApplicationResponse registerApplication(UUID userId, RegisterApplication request) {
        requireUser(userId);
        UUID client = db.queryForObject("SELECT client_id FROM service_a.users WHERE id=?", UUID.class, userId);
        if (request.clientId() != null && !request.clientId().equals(client))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Client mismatch");
        db.queryForList("SELECT id FROM service_a.clients WHERE id=? AND deleted_at IS NULL FOR SHARE", UUID.class,
                client);
        access.clientAdmin(client);
        var value = new Application(userId, request.name().strip());
        value.assignClient(client);
        var app = applications.saveAndFlush(value);
        return appResponse(app);
    }

    public ApplicationResponse getApplication(UUID userId, UUID applicationId) {
        requireUser(userId);
        var app = access.application(applicationId);
        validateAppUser(userId, app);
        return appResponse(app);
    }

    @Transactional
    public ApplicationResponse updateApplication(UUID userId, UUID appId, UpdateApplication request) {
        var app = access.lockedApplication(appId);
        validateAppUser(userId, app);
        access.workspaceAdmin(appId);
        if (request.name() != null)
            audit.changed(appId, "applications", appId, "name", app.getName(), request.name().strip());
        if (request.active() != null) {
            audit.changed(appId, "applications", appId, "is_active", app.isActive(), request.active());
            app.setActive(request.active());
        }
        app.updateMetadata(request.name() == null ? app.getName() : request.name().strip(),
                request.hasDescription() ? request.description() : app.getDescription());
        return appResponse(app);
    }

    public PageResult<ApplicationResponse> listApplications(UUID userId, String q, Pageable page) {
        return listApplications(userId, q, "", page);
    }

    public PageResult<ApplicationResponse> listApplications(UUID userId, String q, String status, Pageable page) {
        return listApplications(userId, q, status, null, page);
    }

    public PageResult<ApplicationResponse> listApplications(UUID userId, String q, String status, UUID clientId,
            Pageable page) {
        if (clientId != null)
            access.clientRole(clientId);
        requireUser(userId);
        ListSearch.text(q);
        var appIds = db.queryForList(
                """
                        SELECT a.id FROM service_a.applications a JOIN service_a.clients c ON c.id=a.client_id
                        JOIN service_a.users u ON u.id=?
                        WHERE a.deleted_at IS NULL AND c.deleted_at IS NULL AND u.deleted_at IS NULL
                          AND (?::uuid IS NULL OR a.client_id=?)
                          AND u.client_id=a.client_id AND (u.role='ADMIN' OR EXISTS(SELECT 1 FROM service_a.workspace_members m WHERE m.application_id=a.id AND m.user_id=u.id AND m.deleted_at IS NULL))
                        """,
                UUID.class, userId, clientId, clientId);
        Specification<Application> spec = (root, query, cb) -> {
            var scope = root.get("id").in(appIds);
            if (q.isBlank())
                return scope;
            String pattern = ListSearch.pattern(q);
            return cb.and(scope,
                    cb.or(cb.like(cb.lower(root.get("name")), pattern, '!'),
                            cb.like(cb.lower(root.get("description")), pattern, '!'),
                            cb.like(((JpaExpression<?>) root.get("id")).cast(String.class), pattern, '!')));
        };
        return PageResult.from(applications.findAll(activationFilter(spec, status, "active"), page)
                .map(app -> ApplicationResponse.from(app).withRole(access.applicationRole(app.getId()))));
    }

    private ApplicationResponse appResponse(Application app) {
        return ApplicationResponse.from(app).withRole(access.applicationRole(app.getId()));
    }

    private void validateAppUser(UUID userId, Application app) {
        var viewer = JwtUser.current();
        if (!app.getUserId().equals(userId) && (viewer == null || !viewer.id().equals(userId) && !viewer.admin()))
            throw notFound("Application");
    }

    private UserAccount requireUser(UUID id) {
        return access.user(id);
    }

    private static ResponseStatusException notFound(String resource) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, resource + " not found");
    }

    @Transactional
    public void deleteApplication(UUID userId, UUID appId) {
        var app = access.lockedApplication(appId);
        var viewer = JwtUser.current();
        if (!app.getUserId().equals(userId) && (viewer == null || !viewer.id().equals(userId) && !viewer.admin()))
            throw ResourceAccess.notFound("Application");
        deleteApplicationTree(app);
    }

    private void deleteApplicationTree(Application app) {
        Instant now = Instant.now();
        endpoints.softDeleteByApplicationId(app.getId(), now);
        events.softDeleteByApplicationId(app.getId(), now);
        keys.softDeleteByApplicationId(app.getId(), now);
        app.softDelete();
    }

}

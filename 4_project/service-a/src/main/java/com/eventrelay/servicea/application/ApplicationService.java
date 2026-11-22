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
    public ApplicationResponse registerApplication(UUID user_id, RegisterApplication request) {
        requireUser(user_id);
        UUID client_id = db.queryForObject("SELECT client_id FROM service_a.users WHERE id=?", UUID.class, user_id);
        if (request.clientId() != null && !request.clientId().equals(client_id))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Client mismatch");
        db.queryForList("SELECT id FROM service_a.clients WHERE id=? AND deleted_at IS NULL FOR SHARE", UUID.class,
                client_id);
        access.clientAdmin(client_id);
        var value = new Application(user_id, request.name().strip());
        value.assignClient(client_id);
        var application = applications.saveAndFlush(value);
        return appResponse(application);
    }

    public ApplicationResponse getApplication(UUID user_id, UUID app_id) {
        requireUser(user_id);
        var application = access.application(app_id);
        validateAppUser(user_id, application);
        return appResponse(application);
    }

    @Transactional
    public ApplicationResponse updateApplication(UUID user_id, UUID app_id, UpdateApplication request) {
        var application = access.lockedApplication(app_id);
        validateAppUser(user_id, application);
        access.workspaceAdmin(app_id);
        if (request.name() != null)
            audit.changed(app_id, "applications", app_id, "name", application.getName(), request.name().strip());
        if (request.active() != null) {
            audit.changed(app_id, "applications", app_id, "is_active", application.isActive(), request.active());
            application.setActive(request.active());
        }
        application.updateMetadata(request.name() == null ? application.getName() : request.name().strip(),
                request.hasDescription() ? request.description() : application.getDescription());
        return appResponse(application);
    }

    public PageResult<ApplicationResponse> listApplications(UUID user_id, String q, Pageable page) {
        return listApplications(user_id, q, "", page);
    }

    public PageResult<ApplicationResponse> listApplications(UUID user_id, String q, String status, Pageable page) {
        return listApplications(user_id, q, status, null, page);
    }

    public PageResult<ApplicationResponse> listApplications(UUID user_id, String q, String status, UUID client_id,
            Pageable page) {
        if (client_id != null)
            access.clientRole(client_id);
        requireUser(user_id);
        ListSearch.text(q);
        var app_ids = db.queryForList(
                """
                        SELECT a.id FROM service_a.applications a JOIN service_a.clients c ON c.id=a.client_id
                        JOIN service_a.users u ON u.id=?
                        WHERE a.deleted_at IS NULL AND c.deleted_at IS NULL AND u.deleted_at IS NULL
                          AND (?::uuid IS NULL OR a.client_id=?)
                          AND u.client_id=a.client_id AND (u.role='ADMIN' OR EXISTS(SELECT 1 FROM service_a.workspace_members m WHERE m.application_id=a.id AND m.user_id=u.id AND m.deleted_at IS NULL))
                        """,
                UUID.class, user_id, client_id, client_id);
        Specification<Application> spec = (root, query, cb) -> {
            var scope = root.get("id").in(app_ids);
            if (q.isBlank())
                return scope;
            String pattern = ListSearch.pattern(q);
            return cb.and(scope,
                    cb.or(cb.like(cb.lower(root.get("name")), pattern, '!'),
                            cb.like(cb.lower(root.get("description")), pattern, '!'),
                            cb.like(((JpaExpression<?>) root.get("id")).cast(String.class), pattern, '!')));
        };
        return PageResult.from(applications.findAll(activationFilter(spec, status, "active"), page)
                .map(application -> ApplicationResponse.from(application).withRole(access.applicationRole(application.getId()))));
    }

    private ApplicationResponse appResponse(Application application) {
        return ApplicationResponse.from(application).withRole(access.applicationRole(application.getId()));
    }

    private void validateAppUser(UUID user_id, Application application) {
        var viewer = JwtUser.current();
        if (!application.getUserId().equals(user_id) && (viewer == null || !viewer.id().equals(user_id) && !viewer.admin()))
            throw notFound("Application");
    }

    private UserAccount requireUser(UUID user_id) {
        return access.user(user_id);
    }

    private static ResponseStatusException notFound(String resource) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, resource + " not found");
    }

    @Transactional
    public void deleteApplication(UUID user_id, UUID app_id) {
        var application = access.lockedApplication(app_id);
        var viewer = JwtUser.current();
        if (!application.getUserId().equals(user_id) && (viewer == null || !viewer.id().equals(user_id) && !viewer.admin()))
            throw ResourceAccess.notFound("Application");
        deleteApplicationTree(application);
    }

    private void deleteApplicationTree(Application application) {
        Instant now = Instant.now();
        endpoints.softDeleteByApplicationId(application.getId(), now);
        events.softDeleteByApplicationId(application.getId(), now);
        keys.softDeleteByApplicationId(application.getId(), now);
        application.softDelete();
    }

}

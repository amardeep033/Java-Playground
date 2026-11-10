package com.eventrelay.servicea.service;

import java.net.URI;
import java.util.*;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.eventrelay.servicea.dto.Requests.*;
import com.eventrelay.servicea.dto.Responses;
import com.eventrelay.servicea.entity.*;
import com.eventrelay.servicea.repository.*;
import org.springframework.security.crypto.password.PasswordEncoder;

@Service
@Transactional(readOnly = true)
public class RegistrationService {
    private final UserAccountRepository users;
    private final ApplicationRepository applications;
    private final EventTypeRepository eventTypes;
    private final EndpointRepository endpoints;
    private final PasswordEncoder passwords;
    private final ResourceAccess access;
    private final com.eventrelay.servicea.security.SecretEncryption encryption;

    private final com.eventrelay.servicea.workspace.AuditLogs audit;
    private final org.springframework.jdbc.core.JdbcTemplate db;
    public RegistrationService(UserAccountRepository users, ApplicationRepository applications,
                               EventTypeRepository eventTypes, EndpointRepository endpoints, PasswordEncoder passwords,
                               ResourceAccess access, com.eventrelay.servicea.security.SecretEncryption encryption, com.eventrelay.servicea.workspace.AuditLogs audit,org.springframework.jdbc.core.JdbcTemplate db) {
        this.audit=audit;this.db=db;
        this.users = users;
        this.applications = applications;
        this.eventTypes = eventTypes;
        this.endpoints = endpoints;
        this.passwords = passwords;
        this.access = access;
        this.encryption = encryption;
    }

    @Transactional
    public Responses.User registerUser(Signup request) {
        UUID client=UUID.randomUUID();
        db.update("INSERT INTO service_a.clients(id,name) VALUES (?,?)",client,request.companyName().strip());
        var user=new UserAccount(request.name().strip(),request.email().strip().toLowerCase(Locale.ROOT),passwords.encode(request.password()));
        user.assignClient(client);user.changeRole("ADMIN");
        return Responses.User.from(users.saveAndFlush(user));
    }

    @Transactional
    public void changePassword(UUID userId, ChangePassword request) {
        access.lockAccount(userId);
        access.account(userId,false);
        var user = users.findForUpdate(userId).orElseThrow(() -> notFound("User"));
        if (!passwords.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Current password is incorrect");
        }
        if (passwords.matches(request.newPassword(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "New password must differ from the current password");
        }
        user.changePasswordHash(passwords.encode(request.newPassword()));
        db.update("UPDATE service_a.refresh_token_families SET revoked_at=now(),revocation_reason='PASSWORD_CHANGED' WHERE user_id=? AND revoked_at IS NULL",userId);
        audit.write(null,"users",userId,"PASSWORD_CHANGED",Map.of());
    }

    public Responses.User getUser(UUID userId) {
        return Responses.User.from(requireUser(userId));
    }

    @Transactional
    public Responses.App registerApplication(UUID userId, RegisterApplication request) {
        requireUser(userId);
        UUID client=db.queryForObject("SELECT client_id FROM service_a.users WHERE id=?",UUID.class,userId);
        if(request.clientId()!=null&&!request.clientId().equals(client)) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Client mismatch");
        db.queryForList("SELECT id FROM service_a.clients WHERE id=? AND deleted_at IS NULL FOR SHARE",UUID.class,client);
        access.clientAdmin(client);
        var value=new Application(userId,request.name().strip());value.assignClient(client);
        var app=applications.saveAndFlush(value);
        return appResponse(app);
    }

    public Responses.App getApplication(UUID userId, UUID applicationId) {
        requireUser(userId);
        var app=access.application(applicationId);validateAppUser(userId,app);
        return appResponse(app);
    }

    @Transactional
    public Responses.Event registerEventType(UUID applicationId, RegisterEventType request) {
        access.lockedApplication(applicationId);
        var event = new EventType(applicationId, request.name(), request.description());
        return Responses.Event.from(eventTypes.saveAndFlush(event));
    }

    public Responses.Event getEventType(UUID applicationId, UUID eventTypeId) {
        requireApplication(applicationId);
        return Responses.Event.from(eventTypes.findByIdAndApplicationId(eventTypeId, applicationId)
                .orElseThrow(() -> notFound("Event type")));
    }

    @Transactional
    public Responses.IssuedEndpoint registerEndpoint(UUID applicationId, RegisterEndpoint request) {
        access.lockedApplication(applicationId);
        validateUrl(request.url());
        var subscriptions = eventTypes.findAllById(request.eventTypeIds());
        if (subscriptions.size() != request.eventTypeIds().size()) {
            throw notFound("Event type");
        }
        if (subscriptions.stream().anyMatch(event -> !event.getApplicationId().equals(applicationId))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "All event types must belong to this application");
        }
        var endpoint = new Endpoint(applicationId, request.url(), request.description(), new HashSet<>(subscriptions));
        String name=request.name()==null?URI.create(request.url()).getHost():request.name();
        if(name.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Endpoint name cannot be blank");
        endpoint.updateName(name.strip().substring(0,Math.min(120,name.strip().length())));
        var config = request.configuration() == null ? EndpointConfiguration.defaults() : request.configuration();
        endpoint.configure(config.enabled(), config.rateLimitPerSecond(), config.concurrencyLimit(), config.timeoutMs(), config.maxAttempts());
        endpoints.saveAndFlush(endpoint);
        return issueSigningSecret(endpoint,true);
    }

    public Responses.WebhookEndpoint getEndpoint(UUID applicationId, UUID endpointId) {
        requireApplication(applicationId);
        return Responses.WebhookEndpoint.from(endpoints.findByIdAndApplicationId(endpointId, applicationId)
                .orElseThrow(() -> notFound("Endpoint")));
    }

    @Transactional
    public Responses.IssuedEndpoint rotateSigningSecret(UUID applicationId, UUID endpointId) {
        access.lockedApplication(applicationId);
        var endpoint = endpoints.findByIdAndApplicationId(endpointId, applicationId).orElseThrow(() -> notFound("Endpoint"));
        return issueSigningSecret(endpoint,false);
    }

    @Transactional
    public Responses.User updateUser(UUID userId, UpdateName request) {
        access.lockAccount(userId);
        access.account(userId,false);
        var user=access.lockedUser(userId);user.updateName(request.name().strip());return Responses.User.from(user);
    }
    @Transactional
    public Responses.App updateApplication(UUID userId, UUID appId, UpdateApplication request) {
        var app=access.lockedApplication(appId);
        validateAppUser(userId,app);access.workspaceAdmin(appId);
        if(request.name()!=null) audit.changed(appId,"applications",appId,"name",app.getName(),request.name().strip());
        if(request.active()!=null) {
            audit.changed(appId,"applications",appId,"is_active",app.isActive(),request.active());
            app.setActive(request.active());
        }
        app.updateMetadata(request.name()==null?app.getName():request.name().strip(),request.hasDescription()?request.description():app.getDescription());
        return appResponse(app);
    }
    @Transactional
    public Responses.Event updateEventType(UUID appId, UUID id, UpdateDescription request) {
        access.lockedApplication(appId);
        var event=eventTypes.findByIdAndApplicationId(id,appId).orElseThrow(()->notFound("Event type"));
        event.updateDescription(request.description());return Responses.Event.from(event);
    }
    @Transactional
    public Responses.WebhookEndpoint updateEndpoint(UUID appId, UUID id, UpdateEndpoint request) {
        access.lockedApplication(appId);
        var endpoint=endpoints.findByIdAndApplicationId(id,appId).orElseThrow(()->notFound("Endpoint"));
        if(request.name()!=null) {
            if(request.name().isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Endpoint name cannot be blank");
            audit.changed(appId,"endpoints",id,"name",endpoint.getName(),request.name().strip());
            endpoint.updateName(request.name().strip());
        }
        if(request.hasDescription())endpoint.updateDescription(request.description());
        if(request.configuration()!=null) {
            var config=request.configuration();
            return updateEndpointSettings(appId,id,new EndpointSettings(null,null,new EndpointConfiguration(endpoint.isEnabled(),config.rateLimitPerSecond(),config.concurrencyLimit(),config.timeoutMs(),config.maxAttempts())));
        }
        return Responses.WebhookEndpoint.from(endpoint);
    }
    @Transactional
    public Responses.Event setEventTypeActive(UUID appId,UUID id,boolean active) {
        access.lockedApplication(appId);access.workspaceAdmin(appId);
        var type=eventTypes.findByIdAndApplicationId(id,appId).orElseThrow(()->notFound("Event type"));
        audit.changed(appId,"event_types",id,"is_active",type.isActive(),active);
        type.setActive(active);return Responses.Event.from(type);
    }
    @Transactional
    public Responses.WebhookEndpoint adminEndpointUpdate(UUID appId,UUID id,AdminEndpointUpdate request) {
        access.lockedApplication(appId);access.workspaceAdmin(appId);
        updateEndpointSettings(appId,id,new EndpointSettings(request.url(),request.eventTypeIds(),null));
        var endpoint=endpoints.findByIdAndApplicationId(id,appId).orElseThrow(()->notFound("Endpoint"));
        if(request.active()!=null){audit.changed(appId,"endpoints",id,"is_active",endpoint.isEnabled(),request.active());endpoint.setActive(request.active());}
        return Responses.WebhookEndpoint.from(endpoint);
    }
    private <T> org.springframework.data.jpa.domain.Specification<T> activationFilter(
            org.springframework.data.jpa.domain.Specification<T> spec,String status,String field) {
        var selected=ListSearch.choices(status,Set.of("ACTIVE","INACTIVE","ENABLED","DISABLED"));
        if(selected.isEmpty())return spec;
        var flags=selected.stream().map(value->Set.of("ACTIVE","ENABLED").contains(value)).distinct().toList();
        return spec.and((root,query,cb)->root.get(field).in(flags));
    }
    public Responses.PageResult<Responses.App> listApplications(UUID userId, String q, Pageable page) {
        return listApplications(userId,q,"",page);
    }
    public Responses.PageResult<Responses.App> listApplications(UUID userId,String q,String status,Pageable page) {
        return listApplications(userId,q,status,null,page);
    }
    public Responses.PageResult<Responses.App> listApplications(UUID userId,String q,String status,UUID clientId,Pageable page) {
        if(clientId!=null) access.clientRole(clientId);
        requireUser(userId);ListSearch.text(q);
        var appIds=db.queryForList("""
            SELECT a.id FROM service_a.applications a JOIN service_a.clients c ON c.id=a.client_id
            JOIN service_a.users u ON u.id=?
            WHERE a.deleted_at IS NULL AND c.deleted_at IS NULL AND u.deleted_at IS NULL
              AND (?::uuid IS NULL OR a.client_id=?)
              AND u.client_id=a.client_id AND (u.role='ADMIN' OR EXISTS(SELECT 1 FROM service_a.workspace_members m WHERE m.application_id=a.id AND m.user_id=u.id AND m.deleted_at IS NULL))
            """,UUID.class,userId,clientId,clientId);
        org.springframework.data.jpa.domain.Specification<Application> spec=(root,query,cb)->{
            var scope=root.get("id").in(appIds);
            if(q.isBlank()) return scope;
            String pattern=ListSearch.pattern(q);
            return cb.and(scope,cb.or(cb.like(cb.lower(root.get("name")),pattern,'!'),cb.like(cb.lower(root.get("description")),pattern,'!'),cb.like(((org.hibernate.query.criteria.JpaExpression<?>)root.get("id")).cast(String.class),pattern,'!')));
        };
        return Responses.PageResult.from(applications.findAll(activationFilter(spec,status,"active"),page).map(app->Responses.App.from(app).withRole(access.applicationRole(app.getId()))));
    }
    public Responses.PageResult<Responses.Event> listEventTypes(UUID appId, String q, Pageable page) {
        return listEventTypes(appId,q,"",page);
    }
    public Responses.PageResult<Responses.Event> listEventTypes(UUID appId,String q,String status,Pageable page) {
        requireApplication(appId);
        return Responses.PageResult.from(eventTypes.findAll(activationFilter(ListSearch.<EventType>scoped("applicationId",appId,q,"name","description"),status,"active"),page).map(Responses.Event::from));
    }
    public Responses.PageResult<Responses.WebhookEndpoint> listEndpoints(UUID appId, String q, String status, Pageable page) {
        requireApplication(appId);
        var spec=ListSearch.<Endpoint>scoped("applicationId",appId,q,"name","url","description");
        spec=activationFilter(spec,status,"enabled");
        return Responses.PageResult.from(endpoints.findAll(spec,page).map(Responses.WebhookEndpoint::from));
    }

    private Responses.WebhookEndpoint updateEndpointSettings(UUID appId,UUID id,EndpointSettings request) {
        access.lockedApplication(appId);
        var endpoint=endpoints.findByIdAndApplicationId(id,appId).orElseThrow(()->notFound("Endpoint"));
        if(request.url()!=null&&!endpoint.getUrl().equals(request.url())) {
            access.workspaceAdmin(appId);validateUrl(request.url());
            audit.write(appId,"endpoints",id,"ENDPOINT_UPDATED",Map.of("field","url","old",endpoint.getUrl(),"new",request.url()));
            endpoint.updateUrl(request.url());
        }
        if(request.eventTypeIds()!=null) {
            var subscriptions=eventTypes.findAllById(request.eventTypeIds());
            if(subscriptions.size()!=request.eventTypeIds().size()) throw notFound("Event type");
            if(subscriptions.stream().anyMatch(type->!type.getApplicationId().equals(appId)))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"All event types must belong to this application");
            var old=endpoint.getEventTypes().stream().map(EventType::getId).sorted().toList();
            var next=subscriptions.stream().map(EventType::getId).sorted().toList();
            if(!old.equals(next)) {
                access.workspaceAdmin(appId);audit.write(appId,"endpoints",id,"EVENT_TYPE_MAPPINGS_UPDATED",Map.of("field","eventTypeIds","old",old,"new",next));
                endpoint.updateSubscriptions(new HashSet<>(subscriptions));
            }
        }
        var config=request.configuration();
        if(config!=null) {
            if(endpoint.isEnabled()!=config.enabled()) {
                access.workspaceAdmin(appId);
                audit.changed(appId,"endpoints",id,"is_active",endpoint.isEnabled(),config.enabled());
            }
            var before=configurationAudit(endpoint);
            var after=Map.of("rateLimitPerSecond",config.rateLimitPerSecond(),"concurrencyLimit",config.concurrencyLimit(),"timeoutMs",config.timeoutMs(),"maxAttempts",config.maxAttempts());
            if(!before.equals(after)) audit.write(appId,"endpoints",id,"CONFIG_UPDATED",Map.of("old",before,"new",after));
            endpoint.configure(config.enabled(),config.rateLimitPerSecond(),config.concurrencyLimit(),config.timeoutMs(),config.maxAttempts());
        }
        return Responses.WebhookEndpoint.from(endpoint);
    }
    private Map<String,Integer> configurationAudit(Endpoint endpoint) {
        return Map.of("rateLimitPerSecond",endpoint.getRateLimitPerSecond(),"concurrencyLimit",endpoint.getConcurrencyLimit(),"timeoutMs",endpoint.getTimeoutMs(),"maxAttempts",endpoint.getMaxAttempts());
    }
    private Responses.IssuedEndpoint issueSigningSecret(Endpoint endpoint,boolean initial) {
        String secret = encryption.generateSecret();
        endpoint.rotateSigningSecret(encryption.encrypt(secret, endpoint.getId()));
        audit.write(endpoint.getApplicationId(),"endpoints",endpoint.getId(),initial?"CREATED":"SIGNING_CREDENTIAL_ROTATED",
                Map.of("rotation",endpoint.getSigningSecretVersion()));
        return new Responses.IssuedEndpoint(Responses.WebhookEndpoint.from(endpoint), secret);
    }

    private Responses.App appResponse(Application app) {
        return Responses.App.from(app).withRole(access.applicationRole(app.getId()));
    }
    private void validateAppUser(UUID userId,Application app) {
        var viewer=com.eventrelay.servicea.auth.JwtUser.current();
        if(!app.getUserId().equals(userId)&&(viewer==null||!viewer.id().equals(userId)&&!viewer.admin())) throw notFound("Application");
    }
    private UserAccount requireUser(UUID id) {
        return access.user(id);
    }

    private Application requireApplication(UUID id) {
        return access.application(id);
    }

    private static ResponseStatusException notFound(String resource) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, resource + " not found");
    }

    private static void validateUrl(String url) {
        try {
            var uri = URI.create(url);
            if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                    || uri.getHost() == null || uri.getUserInfo() != null || uri.getFragment() != null
                    || uri.getPort() > 65535 || uri.getPort() == 0) {
                throw new IllegalArgumentException();
            }
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "URL must be HTTP(S) with a valid host, without credentials or fragment");
        }
    }
}

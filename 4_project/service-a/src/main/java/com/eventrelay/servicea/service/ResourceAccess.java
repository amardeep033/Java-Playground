package com.eventrelay.servicea.service;

import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import com.eventrelay.servicea.entity.*;
import com.eventrelay.servicea.repository.*;

@Component
public class ResourceAccess {
    @jakarta.persistence.PersistenceContext private jakarta.persistence.EntityManager entityManager;
    private final UserAccountRepository users;
    private final ApplicationRepository applications;

    private final com.eventrelay.servicea.workspace.WorkspaceAccess permissions;
    public ResourceAccess(UserAccountRepository users, ApplicationRepository applications,com.eventrelay.servicea.workspace.WorkspaceAccess permissions) {
        this.permissions=permissions;
        this.users = users;
        this.applications = applications;
    }
    public UserAccount user(UUID id) { return users.findById(id).orElseThrow(() -> notFound("User")); }
    public UserAccount lockedUser(UUID id) { return users.findForUpdate(id).orElseThrow(() -> notFound("User")); }
    public Application application(UUID id) {
        var app = applications.findById(id).orElseThrow(() -> notFound("Application"));
        if(com.eventrelay.servicea.auth.JwtUser.current()!=null) permissions.member(id);
        return app;
    }
    // Every child mutation locks its app. Parent deletion takes the same lock.
    public Application lockedApplication(UUID id) {
        permissions.lockClientOfApp(id);
        var app = applications.findForUpdate(id).orElseThrow(() -> notFound("Application"));
        // Authorization may already have loaded this app before a concurrent pause.
        // Refresh under the mutation lock before deciding whether to accept new work.
        entityManager.refresh(app);
        if(com.eventrelay.servicea.auth.JwtUser.current()!=null) permissions.member(id);
        return app;
    }
    public String clientRole(UUID client) { return permissions.clientRole(client); }
    public void clientAdmin(UUID client) { permissions.clientAdmin(client); }
    public void workspaceAdmin(UUID app) { permissions.admin(app); }
    public void lockAccount(UUID id) { permissions.lockAccount(id); }
    public void account(UUID id,boolean admin) { permissions.account(id,admin); }
    public void self(UUID id) { permissions.self(id); }
    public String applicationRole(UUID app) { return permissions.role(app); }
    public static ResponseStatusException notFound(String type) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, type + " not found");
    }
}

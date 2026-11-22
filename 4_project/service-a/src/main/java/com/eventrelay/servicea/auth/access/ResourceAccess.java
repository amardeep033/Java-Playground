package com.eventrelay.servicea.auth.access;

import com.eventrelay.servicea.application.Application;
import com.eventrelay.servicea.application.ApplicationRepository;
import com.eventrelay.servicea.auth.JwtUser;
import com.eventrelay.servicea.user.UserAccount;
import com.eventrelay.servicea.user.UserAccountRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class ResourceAccess {
    @PersistenceContext
    private EntityManager entityManager;
    private final UserAccountRepository users;
    private final ApplicationRepository applications;

    private final WorkspaceAccess permissions;

    public ResourceAccess(UserAccountRepository users, ApplicationRepository applications,
            WorkspaceAccess permissions) {
        this.permissions = permissions;
        this.users = users;
        this.applications = applications;
    }

    public UserAccount user(UUID user_id) {
        return users.findById(user_id).orElseThrow(() -> notFound("User"));
    }

    public UserAccount lockedUser(UUID user_id) {
        return users.findForUpdate(user_id).orElseThrow(() -> notFound("User"));
    }

    //called from incoming request controller + app authz
    public Application application(UUID app_id) {
        var application = applications.findById(app_id).orElseThrow(() -> notFound("Application"));
        if (JwtUser.current() != null)
            permissions.member(app_id);
        return application;
    }

    // Every child mutation locks its app. Parent deletion takes the same lock.
    public Application lockedApplication(UUID app_id) {
        permissions.lockClientOfApp(app_id);
        var application = applications.findForUpdate(app_id).orElseThrow(() -> notFound("Application"));
        // Authorization may already have loaded this app before a concurrent pause.
        // Refresh under the mutation lock before deciding whether to accept new work.
        entityManager.refresh(application);
        if (JwtUser.current() != null)
            permissions.member(app_id);
        return application;
    }

    public String clientRole(UUID client_id) {
        return permissions.clientRole(client_id);
    }

    public void clientAdmin(UUID client_id) {
        permissions.clientAdmin(client_id);
    }

    public void workspaceAdmin(UUID app_id) {
        permissions.admin(app_id);
    }

    public void lockAccount(UUID user_id) {
        permissions.lockAccount(user_id);
    }

    public void account(UUID user_id, boolean admin) {
        permissions.account(user_id, admin);
    }

    public void self(UUID user_id) {
        permissions.self(user_id);
    }

    public String applicationRole(UUID app_id) {
        return permissions.role(app_id);
    }

    public static ResponseStatusException notFound(String type) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, type + " not found");
    }
}

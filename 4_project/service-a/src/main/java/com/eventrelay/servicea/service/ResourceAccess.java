package com.eventrelay.servicea.service;

import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import com.eventrelay.servicea.entity.*;
import com.eventrelay.servicea.repository.*;

@Component
public class ResourceAccess {
    private final UserAccountRepository users;
    private final ApplicationRepository applications;

    public ResourceAccess(UserAccountRepository users, ApplicationRepository applications) {
        this.users = users;
        this.applications = applications;
    }
    public UserAccount user(UUID id) { return users.findById(id).orElseThrow(() -> notFound("User")); }
    public UserAccount lockedUser(UUID id) { return users.findForUpdate(id).orElseThrow(() -> notFound("User")); }
    public Application application(UUID id) {
        var app = applications.findById(id).orElseThrow(() -> notFound("Application"));
        user(app.getUserId());
        return app;
    }
    // Every child mutation locks its app. Parent deletion takes the same lock.
    public Application lockedApplication(UUID id) {
        var app = applications.findForUpdate(id).orElseThrow(() -> notFound("Application"));
        user(app.getUserId());
        return app;
    }
    public static ResponseStatusException notFound(String type) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, type + " not found");
    }
}

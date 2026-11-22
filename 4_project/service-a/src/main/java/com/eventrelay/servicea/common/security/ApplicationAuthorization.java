package com.eventrelay.servicea.common.security;

import com.eventrelay.servicea.apikey.ApiKeyService;
import com.eventrelay.servicea.auth.JwtUser;
import com.eventrelay.servicea.auth.access.ResourceAccess;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class ApplicationAuthorization {
    private final ApiKeyService keys;
    private final ResourceAccess access;

    public ApplicationAuthorization(ApiKeyService keys, ResourceAccess access) {
        this.keys = keys;
        this.access = access;
    }

    // Middleware authenticates the user - Dashboard user(JWT) + API-key caller
    // but the service authorizes access to a specific application
    // Our middleware deliberately allows API-key requests through without validating the key. That's why authorize() must validate it.
    // Also, an invalid explicitly supplied API key cannot silently fall back to JWT authentication.

    public void authorize(UUID app_id, String api_key) {

        // Dashboard JWT : Does user have access to appId?
        if (api_key == null && JwtUser.current() != null) {
            access.application(app_id);
            return;
        }

        // API key :  Is key valid and belongs to appId?
        if (!keys.authenticate(api_key).equals(app_id)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "API key belongs to another application");
        }
    }
}

// only authn of jwt in middleware -- no authz or authn of apikey
// here authz of jwt + authn and authz of apikey together
// @PreAuthorize checks once before entry -- but we need to check again after locking
// we lock to prevent race condition at create ingestion + create replay
// since two checks are there -- therefore no preauth
// api key -- create event, list events, list deliveries, get one event, get delivery, activity summary and replay

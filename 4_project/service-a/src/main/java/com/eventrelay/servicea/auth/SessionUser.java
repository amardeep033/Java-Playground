package com.eventrelay.servicea.auth;

import java.time.Instant;
import java.util.UUID;
import org.springframework.security.core.context.SecurityContextHolder;

public record SessionUser(UUID id, String role, UUID sessionId, Instant expiresAt) {
    public boolean admin() { return "ADMIN".equals(role); }
    public static SessionUser current() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getPrincipal() instanceof SessionUser user ? user : null;
    }
}

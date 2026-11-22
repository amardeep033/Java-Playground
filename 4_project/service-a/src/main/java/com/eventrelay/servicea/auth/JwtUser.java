package com.eventrelay.servicea.auth;

import java.time.Instant;
import java.util.UUID;
import org.springframework.security.core.context.SecurityContextHolder;

//S0: record because JwtUser is a small, immutable snapshot of the authenticated caller(not JPA entity and auto getters)
//id is user id
//token id -- only for tracing -- not used currently
public record JwtUser(UUID id, String role, UUID tokenId, Instant expiresAt) {
    public boolean admin() {
        return "ADMIN".equals(role);
    }

    public static JwtUser current() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getPrincipal() instanceof JwtUser user ? user : null;
    }
}

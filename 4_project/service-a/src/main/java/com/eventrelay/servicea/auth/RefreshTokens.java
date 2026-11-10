package com.eventrelay.servicea.auth;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Access JWT verification stays stateless; only refresh credentials have database state. */
@Service
public class RefreshTokens {
    private final JdbcTemplate db;
    private final SecureRandom random = new SecureRandom();
    private final int minutes;
    private final int days;

    public RefreshTokens(JdbcTemplate db, @Value("${app.auth.token-minutes:15}") int minutes,
                         @Value("${app.auth.refresh-days:30}") int days) {
        this.db = db; this.minutes = minutes; this.days = days;
        if (minutes < 1 || minutes > 60 || days < 1 || days > 30)
            throw new IllegalArgumentException("Access lifetime must be 1–60 minutes; refresh lifetime must be 1–30 days");
    }
    public record Grant(JwtUser user, String token, Instant expiresAt) { }
    private record Token(UUID id, UUID familyId) { }
    private record Family(UUID userId, Instant expiresAt, Instant revokedAt) { }

    // Called inside the password-verified login transaction, which already locks the user.
    @Transactional
    public Grant create(JwtUser user) {
        UUID family = UUID.randomUUID();
        Instant expires = db.queryForObject("SELECT now() + (? * interval '1 day')", (r,n) -> r.getTimestamp(1).toInstant(), days);
        db.update("INSERT INTO service_a.refresh_token_families(id,user_id,expires_at) VALUES (?,?,?)",
                family, user.id(), Timestamp.from(expires));
        return insert(user, family, expires);
    }
    @Transactional
    public Grant rotate(String raw) {
        Token token = find(raw);
        if (token == null) return null;
        // Consistent user -> family lock order also serializes password changes/deletion.
        var roles = db.query("SELECT role FROM service_a.users WHERE id=(SELECT user_id FROM service_a.refresh_token_families WHERE id=?) AND deleted_at IS NULL FOR UPDATE",
                (r,n) -> r.getString(1), token.familyId());
        var family = db.queryForObject("SELECT user_id,expires_at,revoked_at FROM service_a.refresh_token_families WHERE id=? FOR UPDATE",
                (r,n) -> new Family(r.getObject(1,UUID.class),r.getTimestamp(2).toInstant(),r.getTimestamp(3)==null ? null : r.getTimestamp(3).toInstant()),token.familyId());
        if (family.revokedAt()!=null) return null;
        Instant now = Instant.now();
        if (roles.isEmpty() || !family.expiresAt().isAfter(now)) {
            revoke(token.familyId(), roles.isEmpty() ? "ACCOUNT_UNAVAILABLE" : "EXPIRED");
            return null;
        }
        boolean used = Boolean.TRUE.equals(db.queryForObject("SELECT used_at IS NOT NULL FROM service_a.refresh_tokens WHERE id=?",Boolean.class,token.id()));
        if (used) {
            // Return instead of throwing: reuse revocation must commit with the failed request.
            revoke(token.familyId(), "REUSE_DETECTED");
            return null;
        }
        Instant accessExpiry = now.plus(Duration.ofMinutes(minutes));
        if (accessExpiry.isAfter(family.expiresAt())) accessExpiry = family.expiresAt();
        var user = new JwtUser(family.userId(), roles.getFirst(), UUID.randomUUID(), accessExpiry);
        Grant next = insert(user,token.familyId(),family.expiresAt());
        db.update("UPDATE service_a.refresh_tokens SET used_at=now() WHERE id=?",token.id());
        return next;
    }
    @Transactional
    public void logout(String raw) {
        Token token = find(raw);
        if (token != null) revoke(token.familyId(),"LOGOUT");
    }
    private void revoke(UUID family, String reason) {
        db.update("UPDATE service_a.refresh_token_families SET revoked_at=now(),revocation_reason=? WHERE id=? AND revoked_at IS NULL",reason,family);
    }
    private Token find(String raw) {
        if (raw == null || !raw.matches("[A-Za-z0-9_-]{43}")) return null;
        var found = db.query("SELECT id,family_id FROM service_a.refresh_tokens WHERE token_hash=?",
                (r,n) -> new Token(r.getObject(1,UUID.class),r.getObject(2,UUID.class)),hash(raw));
        return found.isEmpty() ? null : found.getFirst();
    }
    private Grant insert(JwtUser user, UUID family, Instant expires) {
        byte[] bytes = new byte[32]; random.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        db.update("INSERT INTO service_a.refresh_tokens(id,family_id,token_hash) VALUES (?,?,?)",UUID.randomUUID(),family,hash(raw));
        return new Grant(user,raw,expires);
    }
    private static String hash(String raw) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException error) { throw new IllegalStateException(error); }
    }
}

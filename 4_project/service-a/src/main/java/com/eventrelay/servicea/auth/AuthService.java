package com.eventrelay.servicea.auth;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final JdbcTemplate db;
    private final PasswordEncoder passwords;
    private final String dummy;
    private final int hours;
    public AuthService(JdbcTemplate db, PasswordEncoder passwords, @Value("${app.auth.session-hours:8}") int hours) {
        this.db = db; this.passwords = passwords; this.hours = hours;
        dummy = passwords.encode(UUID.randomUUID().toString());
    }
    public record LoginResult(SessionUser user, int status, String message, long retryAfter) { }
    private record Account(UUID id, String password, String role) { }
    // Return failures instead of throwing, so audit rows and lockouts commit.
    @Transactional
    public LoginResult login(String email, String password, String ip) {
        String normalized = email.strip().toLowerCase(Locale.ROOT), hash = hash(normalized);
        db.update("INSERT INTO service_a.login_guards(email_hash) VALUES (?) ON CONFLICT DO NOTHING", hash);
        var guard = db.queryForMap("SELECT * FROM service_a.login_guards WHERE email_hash=? FOR UPDATE", hash);
        Instant now = Instant.now();
        var accounts = db.query("SELECT id,password_hash,role FROM service_a.users WHERE email=? AND deleted_at IS NULL FOR UPDATE",
                (r,n) -> new Account(r.getObject("id",UUID.class),r.getString("password_hash"),r.getString("role")), normalized);
        Account account = accounts.isEmpty() ? null : accounts.getFirst();
        Instant locked = instant(guard.get("locked_until"));
        if (locked != null && locked.isAfter(now)) {
            audit(account,hash,ip,false,"LOCKED");
            return new LoginResult(null,429,"Too many login attempts. Try again later.",Math.max(1,Duration.between(now,locked).toSeconds()));
        }
        boolean matches = passwords.matches(password, account == null ? dummy : account.password());
        if (account == null || !matches) {
            Instant started = instant(guard.get("window_started_at"));
            int failures = locked != null || started.plusSeconds(900).isBefore(now) ? 1 : ((Number)guard.get("failures")).intValue()+1;
            Instant until = failures >= 5 ? now.plusSeconds(900) : null;
            db.update("UPDATE service_a.login_guards SET failures=?,window_started_at=?,locked_until=? WHERE email_hash=?",
                    failures,java.sql.Timestamp.from(failures==1 ? now : started),until==null?null:java.sql.Timestamp.from(until),hash);
            audit(account,hash,ip,false,"BAD_CREDENTIALS");
            return new LoginResult(null,401,"Email or password is incorrect.",0);
        }
        db.update("UPDATE service_a.login_guards SET failures=0,window_started_at=now(),locked_until=NULL WHERE email_hash=?",hash);
        audit(account,hash,ip,true,"SUCCESS");
        var user = new SessionUser(account.id(),account.role(),UUID.randomUUID(),now.plus(Duration.ofHours(hours)));
        db.update("INSERT INTO service_a.auth_sessions(id,user_id,expires_at,ip_address) VALUES (?,?,?,?)",
                user.sessionId(),user.id(),java.sql.Timestamp.from(user.expiresAt()),ip);
        return new LoginResult(user,200,null,0);
    }
    public SessionUser session(UUID id, UUID user) {
        var rows=db.query("""
            SELECT u.role,s.expires_at FROM service_a.auth_sessions s JOIN service_a.users u ON u.id=s.user_id
            WHERE s.id=? AND s.user_id=? AND s.revoked_at IS NULL AND s.expires_at>now() AND u.deleted_at IS NULL
            """,(r,n)->new SessionUser(user,r.getString("role"),id,r.getTimestamp("expires_at").toInstant()),id,user);
        return rows.isEmpty()?null:rows.getFirst();
    }
    public void logout(SessionUser user) { db.update("UPDATE service_a.auth_sessions SET revoked_at=coalesce(revoked_at,now()) WHERE id=?",user.sessionId()); }
    public void revokeAll(UUID user) { db.update("UPDATE service_a.auth_sessions SET revoked_at=coalesce(revoked_at,now()) WHERE user_id=?",user); }
    public List<Map<String,Object>> attempts(UUID user) {
        return db.queryForList("SELECT attempted_at,success,reason,ip_address FROM service_a.login_attempts WHERE user_id=? ORDER BY attempted_at DESC,id DESC LIMIT 20",user);
    }
    private void audit(Account user,String hash,String ip,boolean success,String reason) {
        db.update("INSERT INTO service_a.login_attempts(id,user_id,email_hash,success,reason,ip_address) VALUES (?,?,?,?,?,?)",UUID.randomUUID(),user==null?null:user.id(),hash,success,reason,ip);
    }
    private static Instant instant(Object value) { return value==null?null:((java.sql.Timestamp)value).toInstant(); }
    private static String hash(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException error) { throw new IllegalStateException(error); }
    }
}

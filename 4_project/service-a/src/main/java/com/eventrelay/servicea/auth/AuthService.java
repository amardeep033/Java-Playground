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
    private final int minutes;
    private final RefreshTokens refreshTokens;
    public AuthService(JdbcTemplate db, PasswordEncoder passwords, @Value("${app.auth.token-minutes:15}") int minutes, RefreshTokens refreshTokens) {
        this.refreshTokens = refreshTokens;
        this.db = db; this.passwords = passwords; this.minutes = minutes;
        if (minutes < 1 || minutes > 60) throw new IllegalArgumentException("JWT lifetime must be 1–60 minutes");
        dummy = passwords.encode(UUID.randomUUID().toString());
    }
    public record LoginResult(JwtUser user, int status, String message, long retryAfter, RefreshTokens.Grant refresh) { }
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
            audit(account,hash,ip,"LOCKED");
            return new LoginResult(null,429,"Too many login attempts. Try again later.",Math.max(1,Duration.between(now,locked).toSeconds()),null);
        }
        boolean matches = passwords.matches(password, account == null ? dummy : account.password());
        if (account == null || !matches) {
            Instant started = instant(guard.get("window_started_at"));
            int failures = locked != null || started.plusSeconds(900).isBefore(now) ? 1 : ((Number)guard.get("failures")).intValue()+1;
            Instant until = failures >= 5 ? now.plusSeconds(900) : null;
            db.update("UPDATE service_a.login_guards SET failures=?,window_started_at=?,locked_until=? WHERE email_hash=?",
                    failures,java.sql.Timestamp.from(failures==1 ? now : started),until==null?null:java.sql.Timestamp.from(until),hash);
            audit(account,hash,ip,"BAD_CREDENTIALS");
            return new LoginResult(null,401,"Email or password is incorrect.",0,null);
        }
        db.update("UPDATE service_a.login_guards SET failures=0,window_started_at=now(),locked_until=NULL WHERE email_hash=?",hash);
        audit(account,hash,ip,"SUCCESS");
        var user = new JwtUser(account.id(),account.role(),UUID.randomUUID(),now.plus(Duration.ofMinutes(minutes)));
        return new LoginResult(user,200,null,0,refreshTokens.create(user));
    }
    public List<Map<String,Object>> attempts(UUID user, String q) {
        return db.queryForList("SELECT attempted_at,(reason='SUCCESS') AS success,reason,ip_address FROM service_a.login_attempts WHERE user_id=? AND (lower(reason) LIKE ? ESCAPE '!' OR lower(ip_address) LIKE ? ESCAPE '!') ORDER BY attempted_at DESC,id DESC LIMIT 20",user,com.eventrelay.servicea.service.ListSearch.pattern(q),com.eventrelay.servicea.service.ListSearch.pattern(q));
    }
    private void audit(Account user,String hash,String ip,String reason) {
        db.update("INSERT INTO service_a.login_attempts(id,user_id,email_hash,reason,ip_address) VALUES (?,?,?,?,?)",UUID.randomUUID(),user==null?null:user.id(),hash,reason,ip);
    }
    private static Instant instant(Object value) { return value==null?null:((java.sql.Timestamp)value).toInstant(); }
    private static String hash(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException error) { throw new IllegalStateException(error); }
    }
}

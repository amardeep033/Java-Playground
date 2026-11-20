package com.eventrelay.servicea.user;

import com.eventrelay.servicea.auth.JwtUser;
import com.eventrelay.servicea.auth.access.ResourceAccess;
import com.eventrelay.servicea.common.dto.UpdateName;
import com.eventrelay.servicea.user.dto.ChangePassword;
import com.eventrelay.servicea.user.dto.Signup;
import com.eventrelay.servicea.user.dto.UserResponse;
import com.eventrelay.servicea.workspace.AuditLogs;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class UserService {
    private final UserAccountRepository users;
    private final PasswordEncoder passwords;
    private final ResourceAccess access;
    private final AuditLogs audit;
    private final JdbcTemplate db;

    public UserService(UserAccountRepository users, PasswordEncoder passwords, ResourceAccess access, AuditLogs audit,
            JdbcTemplate db) {
        this.users = users;
        this.passwords = passwords;
        this.access = access;
        this.audit = audit;
        this.db = db;
    }

    @Transactional
    public UserResponse registerUser(Signup request) {
        UUID client = UUID.randomUUID();
        db.update("INSERT INTO service_a.clients(id,name) VALUES (?,?)", client, request.companyName().strip());
        var user = new UserAccount(request.name().strip(), request.email().strip().toLowerCase(Locale.ROOT),
                passwords.encode(request.password()));
        user.assignClient(client);
        user.changeRole("ADMIN");
        return UserResponse.from(users.saveAndFlush(user));
    }

    @Transactional
    public void changePassword(UUID userId, ChangePassword request) {
        access.lockAccount(userId);
        access.account(userId, false);
        var user = users.findForUpdate(userId).orElseThrow(() -> notFound("User"));
        if (!passwords.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Current password is incorrect");
        }
        if (passwords.matches(request.newPassword(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "New password must differ from the current password");
        }
        user.changePasswordHash(passwords.encode(request.newPassword()));
        db.update(
                "UPDATE service_a.refresh_token_families SET revoked_at=now(),revocation_reason='PASSWORD_CHANGED' WHERE user_id=? AND revoked_at IS NULL",
                userId);
        audit.write(null, "users", userId, "PASSWORD_CHANGED", Map.of());
    }

    public UserResponse getUser(UUID userId) {
        return UserResponse.from(requireUser(userId));
    }

    @Transactional
    public UserResponse updateUser(UUID userId, UpdateName request) {
        access.lockAccount(userId);
        access.account(userId, false);
        var user = access.lockedUser(userId);
        user.updateName(request.name().strip());
        return UserResponse.from(user);
    }

    private UserAccount requireUser(UUID id) {
        return access.user(id);
    }

    private static ResponseStatusException notFound(String resource) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, resource + " not found");
    }

    @Transactional
    public void deleteUser(UUID userId) {
        access.account(userId, false);
        UUID client = db.queryForObject("SELECT client_id FROM service_a.users WHERE id=?", UUID.class, userId);
        db.queryForList("SELECT id FROM service_a.clients WHERE id=? FOR UPDATE", UUID.class, client);
        var user = access.lockedUser(userId);
        if ("ADMIN".equals(user.getRole()) && !Boolean.TRUE.equals(db.queryForObject(
                "SELECT EXISTS(SELECT 1 FROM service_a.users WHERE client_id=? AND id<>? AND role='ADMIN' AND deleted_at IS NULL)",
                Boolean.class, client, userId)))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Keep at least one company ADMIN");
        db.update(
                "UPDATE service_a.refresh_token_families SET revoked_at=now(),revocation_reason='ACCOUNT_DELETED' WHERE user_id=? AND revoked_at IS NULL",
                userId);
        db.update("UPDATE service_a.workspace_members SET deleted_at=now() WHERE user_id=? AND deleted_at IS NULL",
                userId);
        db.update(
                "INSERT INTO service_a.audit_logs(id,client_id,actor_id,entity_type,entity_id,action,details) VALUES (?,?,?,'users',?,'DELETED','{}'::jsonb)",
                UUID.randomUUID(), client, JwtUser.current().id(), userId);
        user.softDelete();
    }

}

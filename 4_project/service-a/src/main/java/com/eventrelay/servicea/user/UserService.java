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
        UUID client_id = UUID.randomUUID();
        db.update("INSERT INTO service_a.clients(id,name) VALUES (?,?)", client_id, request.companyName().strip());
        var user_account = new UserAccount(request.name().strip(), request.email().strip().toLowerCase(Locale.ROOT),
                passwords.encode(request.password()));
        user_account.assignClient(client_id);
        user_account.changeRole("ADMIN");
        return UserResponse.from(users.saveAndFlush(user_account));
    }

    @Transactional
    public void changePassword(UUID user_id, ChangePassword request) {
        access.lockAccount(user_id);
        access.account(user_id, false);
        var user_account = users.findForUpdate(user_id).orElseThrow(() -> notFound("User"));
        if (!passwords.matches(request.currentPassword(), user_account.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Current password is incorrect");
        }
        if (passwords.matches(request.newPassword(), user_account.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "New password must differ from the current password");
        }
        user_account.changePasswordHash(passwords.encode(request.newPassword()));
        db.update(
                "UPDATE service_a.refresh_token_families SET revoked_at=now(),revocation_reason='PASSWORD_CHANGED' WHERE user_id=? AND revoked_at IS NULL",
                user_id);
        audit.write(null, "users", user_id, "PASSWORD_CHANGED", Map.of());
    }

    public UserResponse getUser(UUID user_id) {
        return UserResponse.from(requireUser(user_id));
    }

    @Transactional
    public UserResponse updateUser(UUID user_id, UpdateName request) {
        access.lockAccount(user_id);
        access.account(user_id, false);
        var user_account = access.lockedUser(user_id);
        user_account.updateName(request.name().strip());
        return UserResponse.from(user_account);
    }

    private UserAccount requireUser(UUID user_id) {
        return access.user(user_id);
    }

    private static ResponseStatusException notFound(String resource) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, resource + " not found");
    }

    @Transactional
    public void deleteUser(UUID user_id) {
        access.account(user_id, false);
        UUID client_id = db.queryForObject("SELECT client_id FROM service_a.users WHERE id=?", UUID.class, user_id);
        db.queryForList("SELECT id FROM service_a.clients WHERE id=? FOR UPDATE", UUID.class, client_id);
        var user_account = access.lockedUser(user_id);
        if ("ADMIN".equals(user_account.getRole()) && !Boolean.TRUE.equals(db.queryForObject(
                "SELECT EXISTS(SELECT 1 FROM service_a.users WHERE client_id=? AND id<>? AND role='ADMIN' AND deleted_at IS NULL)",
                Boolean.class, client_id, user_id)))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Keep at least one company ADMIN");
        db.update(
                "UPDATE service_a.refresh_token_families SET revoked_at=now(),revocation_reason='ACCOUNT_DELETED' WHERE user_id=? AND revoked_at IS NULL",
                user_id);
        db.update("UPDATE service_a.workspace_members SET deleted_at=now() WHERE user_id=? AND deleted_at IS NULL",
                user_id);
        db.update(
                "INSERT INTO service_a.audit_logs(id,client_id,actor_id,entity_type,entity_id,action,details) VALUES (?,?,?,'users',?,'DELETED','{}'::jsonb)",
                UUID.randomUUID(), client_id, JwtUser.current().id(), user_id);
        user_account.softDelete();
    }

}

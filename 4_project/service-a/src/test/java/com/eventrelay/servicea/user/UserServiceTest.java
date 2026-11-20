package com.eventrelay.servicea.user;

import com.eventrelay.servicea.auth.access.ResourceAccess;
import com.eventrelay.servicea.user.UserAccount;
import com.eventrelay.servicea.user.UserAccountRepository;
import com.eventrelay.servicea.user.UserService;
import com.eventrelay.servicea.user.dto.ChangePassword;
import com.eventrelay.servicea.user.dto.Signup;
import com.eventrelay.servicea.workspace.AuditLogs;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    @Mock UserAccountRepository users;
    @Mock ResourceAccess access;
    @Mock AuditLogs audit;
    @Mock JdbcTemplate db;
    UserService usersService;
    PasswordEncoder passwords = Pbkdf2PasswordEncoder.defaultsForSpringSecurity_v5_8();
    @BeforeEach void setUp() { usersService = new UserService(users,passwords,access,audit,db); }

    @Test
    void normalizesEmailBeforeSaving() {
        when(users.saveAndFlush(any())).thenAnswer(call -> call.getArgument(0));
        var response = usersService.registerUser(new Signup(" Alice ","Acme", "Alice@EXAMPLE.com", "ExamplePass123!"));
        assertEquals("alice@example.com", response.email());
        assertEquals("Alice", response.name());
        var saved = ArgumentCaptor.forClass(UserAccount.class);
        verify(users).saveAndFlush(saved.capture());
        assertNotEquals("ExamplePass123!", saved.getValue().getPasswordHash());
        assertTrue(passwords.matches("ExamplePass123!", saved.getValue().getPasswordHash()));
    }

    @Test
    void passwordChangeRequiresCurrentPasswordAndStoresNewHash() {
        UUID userId = UUID.randomUUID();
        var user = new UserAccount("Alice", "alice@example.com", passwords.encode("ExamplePass123!"));
        when(users.findForUpdate(userId)).thenReturn(Optional.of(user));
        usersService.changePassword(userId, new ChangePassword("ExamplePass123!", "ChangedPass123!"));
        assertTrue(passwords.matches("ChangedPass123!", user.getPasswordHash()));
        assertFalse(passwords.matches("ExamplePass123!", user.getPasswordHash()));
    }

    @Test
    void wrongCurrentPasswordLeavesHashUnchanged() {
        UUID userId = UUID.randomUUID();
        String hash = passwords.encode("ExamplePass123!");
        var user = new UserAccount("Alice", "alice@example.com", hash);
        when(users.findForUpdate(userId)).thenReturn(Optional.of(user));
        var error = assertThrows(ResponseStatusException.class, () -> usersService.changePassword(userId,
                new ChangePassword("IncorrectPass!", "ChangedPass123!")));
        assertEquals(401, error.getStatusCode().value());
        assertEquals(hash, user.getPasswordHash());
    }

    @Test
    void passwordChangeRejectsReusingCurrentPassword() {
        UUID userId = UUID.randomUUID();
        var user = new UserAccount("Alice", "alice@example.com", passwords.encode("ExamplePass123!"));
        when(users.findForUpdate(userId)).thenReturn(Optional.of(user));
        var error = assertThrows(ResponseStatusException.class, () -> usersService.changePassword(userId,
                new ChangePassword("ExamplePass123!", "ExamplePass123!")));
        assertEquals(400, error.getStatusCode().value());
    }
}

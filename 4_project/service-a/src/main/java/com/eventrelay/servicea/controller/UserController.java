package com.eventrelay.servicea.controller;
import java.util.UUID;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import com.eventrelay.servicea.dto.Requests.*;
import com.eventrelay.servicea.dto.Responses;
import com.eventrelay.servicea.service.RegistrationService;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    private final RegistrationService service;
    private final com.eventrelay.servicea.service.SoftDeletionService deletion;
    private final com.eventrelay.servicea.auth.AuthService auth;
    private final com.eventrelay.servicea.auth.SessionCookies sessions;
    public UserController(RegistrationService service, com.eventrelay.servicea.service.SoftDeletionService deletion,
            com.eventrelay.servicea.auth.AuthService auth, com.eventrelay.servicea.auth.SessionCookies sessions) {
        this.service=service;this.deletion=deletion;this.auth=auth;this.sessions=sessions;
    }

    @PostMapping("")
    @ResponseStatus(HttpStatus.CREATED)
    public Responses.User register(@Valid @RequestBody RegisterUser request, jakarta.servlet.http.HttpServletRequest http, jakarta.servlet.http.HttpServletResponse response) {
        var user = service.registerUser(request);
        var result = auth.login(request.email(), request.password(), http.getRemoteAddr());
        if (result.user() != null) sessions.setSession(result.user(), http, response);
        return user;
    }
    
    @GetMapping("/{userId}")
    public Responses.User get(@PathVariable UUID userId) {
        return service.getUser(userId);
    }

    @PutMapping("/{userId}/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@PathVariable UUID userId, @Valid @RequestBody ChangePassword request, jakarta.servlet.http.HttpServletRequest http, jakarta.servlet.http.HttpServletResponse response) {
        service.changePassword(userId, request);
        sessions.clear(http, response);
    }

    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID userId, jakarta.servlet.http.HttpServletRequest http, jakarta.servlet.http.HttpServletResponse response) {
        deletion.deleteUser(userId);
        auth.revokeAll(userId);
        if (userId.equals(com.eventrelay.servicea.auth.SessionUser.current().id())) sessions.clear(http, response);
    }
}

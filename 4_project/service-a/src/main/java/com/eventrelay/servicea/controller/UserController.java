package com.eventrelay.servicea.controller;
import java.util.UUID;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;

import com.eventrelay.servicea.dto.Requests.*;
import com.eventrelay.servicea.dto.Responses;
import com.eventrelay.servicea.service.RegistrationService;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    private final RegistrationService service;
    private final com.eventrelay.servicea.service.SoftDeletionService deletion;
    private final com.eventrelay.servicea.auth.AuthService auth;
    private final com.eventrelay.servicea.auth.JwtCookies cookies;
    public UserController(RegistrationService service, com.eventrelay.servicea.service.SoftDeletionService deletion,
            com.eventrelay.servicea.auth.AuthService auth, com.eventrelay.servicea.auth.JwtCookies cookies) {
        this.service=service;this.deletion=deletion;this.auth=auth;this.cookies=cookies;
    }

    @PostMapping("")
    @ResponseStatus(HttpStatus.CREATED)
    public Responses.User register(@Valid @RequestBody Signup request, jakarta.servlet.http.HttpServletRequest http, jakarta.servlet.http.HttpServletResponse response) {
        var user = service.registerUser(request);
        var result = auth.login(request.email(), request.password(), http.getRemoteAddr());
        var principal=result.user();
        response.setHeader("Cache-Control","no-store");
        if(principal!=null) cookies.issue(result.refresh(),http,response);
        return user;
    }
    
    @GetMapping("/{userId}")
    public Responses.User get(@PathVariable UUID userId) {
        return service.getUser(userId);
    }

    @PutMapping("/{userId}")
    public Responses.User update(@PathVariable UUID userId, @Valid @RequestBody UpdateName request) {
        return service.updateUser(userId, request);
    }

    @PutMapping("/{userId}/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@PathVariable UUID userId, @Valid @RequestBody ChangePassword request, jakarta.servlet.http.HttpServletRequest http, jakarta.servlet.http.HttpServletResponse response) {
        service.changePassword(userId, request);
        cookies.clear(http,response);
    }

    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID userId, jakarta.servlet.http.HttpServletRequest http, jakarta.servlet.http.HttpServletResponse response) {
        deletion.deleteUser(userId);
        if(userId.equals(com.eventrelay.servicea.auth.JwtUser.current().id())) cookies.clear(http,response);
    }
}

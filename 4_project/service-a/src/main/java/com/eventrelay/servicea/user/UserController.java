package com.eventrelay.servicea.user;

import com.eventrelay.servicea.auth.AuthService;
import com.eventrelay.servicea.auth.JwtCookies;
import com.eventrelay.servicea.auth.JwtUser;
import com.eventrelay.servicea.common.dto.UpdateName;
import com.eventrelay.servicea.user.dto.ChangePassword;
import com.eventrelay.servicea.user.dto.Signup;
import com.eventrelay.servicea.user.dto.UserResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    
    //request dto: Signup, UpdateName, ChangePassword
    //response dto: UserResponse

    private final UserService service;
    private final AuthService auth;
    private final JwtCookies cookies;

    public UserController(UserService service,
            AuthService auth, JwtCookies cookies) {
        this.service = service;
        this.auth = auth;
        this.cookies = cookies;
    }

    //------------------------------------------------

    @PostMapping("")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@Valid @RequestBody Signup request, HttpServletRequest http,
            HttpServletResponse response) {
        var user = service.registerUser(request);
        var result = auth.login(request.email(), request.password(), http.getRemoteAddr());
        var principal = result.user();
        response.setHeader("Cache-Control", "no-store");
        if (principal != null)
            cookies.issue(result.refresh(), http, response);
        return user;
    }

    @GetMapping("/{userId}")
    public UserResponse get(@PathVariable UUID userId) {
        return service.getUser(userId);
    }

    @PutMapping("/{userId}")
    public UserResponse update(@PathVariable UUID userId, @Valid @RequestBody UpdateName request) {
        return service.updateUser(userId, request);
    }

    @PutMapping("/{userId}/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@PathVariable UUID userId, @Valid @RequestBody ChangePassword request,
            HttpServletRequest http, HttpServletResponse response) {
        service.changePassword(userId, request);
        cookies.clear(http, response);
    }

    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID userId, HttpServletRequest http,
            HttpServletResponse response) {
        service.deleteUser(userId);
        if (userId.equals(JwtUser.current().id()))
            cookies.clear(http, response);
    }
}

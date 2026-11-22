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
        var user_response = service.registerUser(request);
        var result = auth.login(request.email(), request.password(), http.getRemoteAddr());
        var principal = result.user();
        response.setHeader("Cache-Control", "no-store");
        if (principal != null)
            cookies.issue(result.refresh(), http, response);
        return user_response;
    }

    @GetMapping("/{userId}")
    public UserResponse get(@PathVariable("userId") UUID user_id) {
        return service.getUser(user_id);
    }

    @PutMapping("/{userId}")
    public UserResponse update(@PathVariable("userId") UUID user_id, @Valid @RequestBody UpdateName request) {
        return service.updateUser(user_id, request);
    }

    @PutMapping("/{userId}/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@PathVariable("userId") UUID user_id, @Valid @RequestBody ChangePassword request,
            HttpServletRequest http, HttpServletResponse response) {
        service.changePassword(user_id, request);
        cookies.clear(http, response);
    }

    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable("userId") UUID user_id, HttpServletRequest http,
            HttpServletResponse response) {
        service.deleteUser(user_id);
        if (user_id.equals(JwtUser.current().id()))
            cookies.clear(http, response);
    }
}

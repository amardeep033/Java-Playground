package com.eventrelay.servicea.auth;

import java.time.*;
import java.util.*;
import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.web.csrf.*;
import org.springframework.web.bind.annotation.*;
import com.eventrelay.servicea.dto.Responses;
import com.eventrelay.servicea.service.RegistrationService;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService auth;
    private final SessionCookies cookies;
    private final RegistrationService users;
    private final JdbcTemplate db;
    public AuthController(AuthService auth,SessionCookies cookies,RegistrationService users,JdbcTemplate db) {
        this.auth=auth;this.cookies=cookies;this.users=users;this.db=db;
    }
    public record Login(@NotBlank @Email @Size(max=320) String email,@NotBlank @Size(max=128) String password) { }
    public record Me(Responses.User user,String role,Instant expiresAt) { }
    @GetMapping("/csrf") public Map<String,String> csrf(CsrfToken token) { return Map.of("token",token.getToken()); }
    @PostMapping("/login") public ResponseEntity<?> login(@Valid @RequestBody Login login,HttpServletRequest request,HttpServletResponse response) {
        var result=auth.login(login.email(),login.password(),request.getRemoteAddr());
        if(result.user()==null) {
            var builder=ResponseEntity.status(result.status());
            if(result.retryAfter()>0) builder.header("Retry-After",Long.toString(result.retryAfter()));
            return builder.body(Map.of("status",result.status(),"detail",result.message()));
        }
        cookies.setSession(result.user(),request,response);
        return ResponseEntity.ok(me(result.user()));
    }
    @GetMapping("/me") public Me me() { return me(SessionUser.current()); }
    private Me me(SessionUser user) { return new Me(users.getUser(user.id()),user.role(),user.expiresAt()); }
    @GetMapping("/login-attempts") public List<Map<String,Object>> attempts() { return auth.attempts(SessionUser.current().id()); }
    @PostMapping("/logout") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpServletRequest request,HttpServletResponse response) { auth.logout(SessionUser.current());cookies.clear(request,response); }
    @PostMapping("/logout-all") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logoutAll(HttpServletRequest request,HttpServletResponse response) { auth.revokeAll(SessionUser.current().id());cookies.clear(request,response); }
    @GetMapping("/admin/users") public List<Map<String,Object>> users() {
        return db.queryForList("SELECT id,name,email,role,created_at FROM service_a.users WHERE deleted_at IS NULL ORDER BY created_at DESC LIMIT 100");
    }
}

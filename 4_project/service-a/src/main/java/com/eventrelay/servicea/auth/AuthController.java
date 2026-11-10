package com.eventrelay.servicea.auth;

import java.time.*;
import java.util.*;
import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import com.eventrelay.servicea.dto.Responses;
import com.eventrelay.servicea.service.RegistrationService;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService auth;
    private final RefreshTokens refreshTokens;
    private final JwtCookies cookies;
    private final RegistrationService users;
    public AuthController(AuthService auth,JwtCookies cookies,RegistrationService users,RefreshTokens refreshTokens) {
        this.refreshTokens=refreshTokens;this.auth=auth;this.cookies=cookies;this.users=users;
    }
    public record Login(@NotBlank @Email @Size(max=320) String email,@NotBlank @Size(max=128) String password) { }
    public record Permissions(boolean manageUsers,boolean createApplications) { }
    public record Me(Responses.User user,String role,Instant expiresAt,Permissions permissions) { }
    @GetMapping("/csrf") public ResponseEntity<Map<String,String>> csrf(org.springframework.security.web.csrf.CsrfToken token) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(Map.of("token",token.getToken(),"headerName",token.getHeaderName()));
    }
    @PostMapping("/login") public ResponseEntity<?> login(@Valid @RequestBody Login login,HttpServletRequest request,HttpServletResponse response) {
        return signIn(login,request,response);
    }
    private ResponseEntity<?> signIn(Login login,HttpServletRequest request,HttpServletResponse response) {
        var result=auth.login(login.email(),login.password(),request.getRemoteAddr());
        if(result.user()==null) {
            var builder=ResponseEntity.status(result.status());
            if(result.retryAfter()>0) builder.header("Retry-After",Long.toString(result.retryAfter()));
            return builder.body(Map.of("status",result.status(),"detail",result.message()));
        }
        var principal=result.user();
        cookies.issue(result.refresh(),request,response);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(me(principal));
    }
    @PostMapping("/refresh") public ResponseEntity<?> refresh(HttpServletRequest request,HttpServletResponse response) {
        var grant=refreshTokens.rotate(cookies.refreshToken(request));
        if(grant==null) {
            cookies.clear(request,response);
            return ResponseEntity.status(401).cacheControl(CacheControl.noStore()).body(Map.of("status",401,"detail","Please sign in again."));
        }
        cookies.issue(grant,request,response);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(me(grant.user()));
    }
    @GetMapping("/me") public Me me() { return me(JwtUser.current()); }
    private Me me(JwtUser user) {
        Responses.User account;
        try { account=users.getUser(user.id()); }
        catch(org.springframework.web.server.ResponseStatusException missing) {
            if(missing.getStatusCode().value()==404) throw new org.springframework.web.server.ResponseStatusException(HttpStatus.UNAUTHORIZED,"Please sign in again.");
            throw missing;
        }
        boolean admin="ADMIN".equals(account.role());
        return new Me(account,account.role(),user.expiresAt(),new Permissions(admin,admin));
    }
    @GetMapping("/login-attempts") public List<Map<String,Object>> attempts(@RequestParam(defaultValue="") String q) { return auth.attempts(JwtUser.current().id(),q); }
    @PostMapping("/logout") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpServletRequest request,HttpServletResponse response) {
        // Revoke the refresh family; copied access JWTs remain valid only until their expiry.
        refreshTokens.logout(cookies.refreshToken(request));
        cookies.clear(request,response);
    }
}

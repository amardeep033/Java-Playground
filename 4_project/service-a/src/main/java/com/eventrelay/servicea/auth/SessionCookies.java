package com.eventrelay.servicea.auth;

import java.time.*;
import jakarta.servlet.http.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.stereotype.Component;

@Component
public class SessionCookies {
    private final JwtTokens tokens;
    private final CookieCsrfTokenRepository csrf;
    private final boolean secure;
    public SessionCookies(JwtTokens tokens,CookieCsrfTokenRepository csrf,@Value("${app.auth.cookie-secure:false}") boolean secure) {
        this.tokens=tokens;this.csrf=csrf;this.secure=secure;
    }
    public void setSession(SessionUser user,HttpServletRequest request,HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE,ResponseCookie.from("ER_SESSION",tokens.issue(user)).httpOnly(true).secure(secure)
                .sameSite("Lax").path("/api/v1").maxAge(Duration.between(Instant.now(),user.expiresAt())).build().toString());
        csrf.saveToken(csrf.generateToken(request),request,response);
    }
    public void clear(HttpServletRequest request,HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE,ResponseCookie.from("ER_SESSION","").httpOnly(true).secure(secure).sameSite("Lax").path("/api/v1").maxAge(0).build().toString());
        csrf.saveToken(null,request,response);
    }
}

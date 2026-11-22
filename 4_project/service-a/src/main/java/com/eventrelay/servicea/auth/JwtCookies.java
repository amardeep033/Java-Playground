package com.eventrelay.servicea.auth;

import jakarta.servlet.http.*;
import java.time.Duration;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.stereotype.Component;

// S5: controller -> issue JWT and set cookie
@Component
public class JwtCookies {
    public static final String NAME = "ER_ACCESS"; //eventrelay_access_token
    public static final String REFRESH_NAME = "ER_REFRESH"; //eventrelay_refresh_token
    private final JwtTokens tokens;
    private final CookieCsrfTokenRepository csrf;
    private final boolean secure;

    public JwtCookies(JwtTokens tokens, CookieCsrfTokenRepository csrf,
            @Value("${app.auth.cookie-secure:false}") boolean secure) {
        this.tokens = tokens;
        this.csrf = csrf;
        this.secure = secure;
    }

    //called from auth controller(login and refresh) and user controller(after signup)
    public void issue(RefreshTokens.Grant grant, HttpServletRequest request, HttpServletResponse response) {
        var authenticated_user = grant.user();
        refreshCookie(response, grant.token(),
                Math.max(0, Duration.between(Instant.now(), grant.expiresAt()).toSeconds()));
        write(response, tokens.issue(authenticated_user), Math.max(0, Duration.between(Instant.now(), authenticated_user.expiresAt()).toSeconds()));
        csrf.saveToken(null, request, response); // Rotate CSRF after sign-in/refresh; clients obtain a fresh masked
                                                 // header before writes.
    }

    public void clear(HttpServletRequest request, HttpServletResponse response) {
        refreshCookie(response, "", 0);
        write(response, "", 0);
        csrf.saveToken(null, request, response);
    }

    public String refreshToken(HttpServletRequest request) {
        if (request.getCookies() != null)
            for (var cookie : request.getCookies())
                if (REFRESH_NAME.equals(cookie.getName()))
                    return cookie.getValue();
        return null;
    }

    private void refreshCookie(HttpServletResponse response, String token, long seconds) {
        response.addHeader(HttpHeaders.SET_COOKIE,
                ResponseCookie.from(REFRESH_NAME, token).httpOnly(true).secure(secure)
                        .sameSite("Lax").path("/api/v1/auth").maxAge(seconds).build().toString());
    }

    private void write(HttpServletResponse response, String token, long seconds) {
        response.addHeader(HttpHeaders.SET_COOKIE, ResponseCookie.from(NAME, token).httpOnly(true).secure(secure)
                .sameSite("Lax").path("/api/v1").maxAge(seconds).build().toString());
    }
}

package com.eventrelay.servicea.auth;

import java.io.IOException;
import java.util.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.web.filter.OncePerRequestFilter;

public class SessionFilter extends OncePerRequestFilter {
    private final JwtTokens tokens;
    private final AuthService auth;
    public SessionFilter(JwtTokens tokens,AuthService auth) { this.tokens=tokens;this.auth=auth; }
    @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain) throws ServletException,IOException {
        // API-key requests never borrow ambient browser credentials or bypass CSRF with a cookie.
        if (!SecurityConfig.keyRequest(request) && request.getCookies()!=null) {
            for(var cookie:request.getCookies()) if (cookie.getName().equals("ER_SESSION")) {
                try {
                    var jwt=tokens.decode(cookie.getValue());
                    var user=auth.session(UUID.fromString(jwt.getId()),UUID.fromString(jwt.getSubject()));
                    if(user!=null) SecurityContextHolder.getContext().setAuthentication(
                            UsernamePasswordAuthenticationToken.authenticated(user,null,List.of(new SimpleGrantedAuthority("ROLE_"+user.role()))));
                } catch (JwtException | IllegalArgumentException ignored) { /* Expired, forged or revoked sessions are anonymous. */ }
                break;
            }
        }
        chain.doFilter(request,response);
    }
}

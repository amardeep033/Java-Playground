package com.eventrelay.servicea.auth;

import java.io.IOException;
import java.util.List;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.web.filter.OncePerRequestFilter;

public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtTokens tokens;
    public JwtAuthenticationFilter(JwtTokens tokens) { this.tokens=tokens; }
    @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain) throws ServletException,IOException {
        // Explicit API-key requests authenticate in the ingestion/monitoring service.
        // A bad API key must never borrow a valid dashboard JWT cookie.
        if (!SecurityConfig.keyRequest(request) && !SecurityConfig.publicAuth(request)) {
            String value=null;
            if(request.getCookies()!=null) for(Cookie cookie:request.getCookies()) {
                if(JwtCookies.NAME.equals(cookie.getName())) { value=cookie.getValue();break; }
            }
            if(value!=null) {
                try {
                    // Stateless verification: signed claims only. No DB session, blacklist,
                    // or user lookup. Business authorization still checks live client roles and app mappings.
                    var user=tokens.authenticate(value);
                    SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                            user,null,List.of(new SimpleGrantedAuthority("ROLE_"+user.role()))));
                } catch (JwtException | IllegalArgumentException invalid) {
                    com.eventrelay.servicea.ingress.IncomingRequestContext.reason(request,com.eventrelay.servicea.ingress.IncomingRequestContext.Reason.INVALID_JWT);
                    response.setHeader("WWW-Authenticate","Bearer error=\"invalid_token\"");
                    SecurityConfig.problem(response,401,"Invalid or expired access token");
                    return;
                }
            }
        }
        chain.doFilter(request,response);
    }
}

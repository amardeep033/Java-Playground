package com.eventrelay.servicea.auth;

import com.eventrelay.servicea.common.config.SecurityConfig;
import com.eventrelay.servicea.ingress.IncomingRequestContext;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.web.filter.OncePerRequestFilter;

// S3: intercept and check jwt
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokens tokens;

    public JwtAuthenticationFilter(JwtTokens tokens) {
        this.tokens=tokens; 
    }

    @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain) throws ServletException,IOException {

        // check the JWT cookie only when the request is neither an API-key request nor a public authentication request(get csrf, sign up, login, refresh, logout)
        if (!SecurityConfig.keyRequest(request) && !SecurityConfig.publicAuth(request)) {

            String value=null;

            //protects from null pointer exception
            if(request.getCookies()!=null) {
                for(Cookie cookie:request.getCookies()) {
                    if(JwtCookies.NAME.equals(cookie.getName())) { 
                        value=cookie.getValue();
                        break; 
                    }
                }
            }
                
            if(value!=null) {
                try {
                    // calls and authenticate and get User(id, role, tokenid, expires at)
                    var authenticated_user=tokens.authenticate(value);
                    SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                            authenticated_user,null,List.of(new SimpleGrantedAuthority("ROLE_"+authenticated_user.role()))));
                } catch (JwtException | IllegalArgumentException invalid) {
                    IncomingRequestContext.reason(request,IncomingRequestContext.Reason.INVALID_JWT);
                    response.setHeader("WWW-Authenticate","Bearer error=\"invalid_token\"");
                    SecurityConfig.problem(response,401,"Invalid or expired access token");
                    return;
                }
            }
        }
        chain.doFilter(request,response);
    }
}

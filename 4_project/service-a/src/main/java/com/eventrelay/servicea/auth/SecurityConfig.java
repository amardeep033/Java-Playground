package com.eventrelay.servicea.auth;

import jakarta.servlet.http.*;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.*;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {
    // Classify explicit API-key calls on event/monitoring routes, not authenticate them.
    // The services below the filter chain still validate the key and its application.
    static boolean keyRequest(HttpServletRequest request) {
        String key=request.getHeader("X-API-Key"),path=request.getRequestURI(),method=request.getMethod();
        if(key==null||key.isBlank()) return false;
        String root="/api/v1/applications/[0-9a-fA-F-]{36}";
        return (method.equals("GET") && path.matches(root+"/(events(?:/[0-9a-fA-F-]{36})?|deliveries(?:/[0-9a-fA-F-]{36})?|activity-summary)"))
                || (method.equals("POST") && path.matches(root+"/(events|deliveries/[0-9a-fA-F-]{36}/replay)"));
    }
    @Bean org.springframework.security.core.userdetails.UserDetailsService noBasicLogin() {
        return username -> { throw new org.springframework.security.core.userdetails.UsernameNotFoundException("Use dashboard login"); };
    }
    @Bean CookieCsrfTokenRepository csrfRepository(@Value("${app.auth.cookie-secure:false}") boolean secure) {
        var repository=new CookieCsrfTokenRepository();
        repository.setCookiePath("/api/v1");
        repository.setCookieCustomizer(cookie->cookie.httpOnly(true).secure(secure).sameSite("Lax"));
        return repository;
    }
    // Public authentication/bootstrap routes must remain usable with an expired cookie.
    static boolean publicAuth(HttpServletRequest request) {
        String path=request.getRequestURI();
        return (request.getMethod().equals("GET") && path.equals("/api/v1/auth/csrf"))
                || (request.getMethod().equals("POST") && java.util.Set.of("/api/v1/users","/api/v1/auth/login","/api/v1/auth/logout","/api/v1/auth/refresh").contains(path));
    }
    @Bean SecurityFilterChain security(HttpSecurity http,JwtTokens tokens,CookieCsrfTokenRepository csrf) throws Exception {
        http.sessionManagement(s->s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .formLogin(f->f.disable()).httpBasic(b->b.disable()).logout(l->l.disable())
            .requestCache(c->c.disable())
            // Cookie-authenticated writes (including login/logout) require a CSRF header.
            // Only explicit API-key event/monitoring routes bypass this check.
            .csrf(c->c.csrfTokenRepository(csrf).ignoringRequestMatchers(SecurityConfig::keyRequest))
            .authorizeHttpRequests(r->r
                .requestMatchers("/error").permitAll()
                .requestMatchers(HttpMethod.POST,"/api/v1/users","/api/v1/auth/login","/api/v1/auth/logout","/api/v1/auth/refresh").permitAll()
                .requestMatchers(HttpMethod.GET,"/api/v1/auth/csrf").permitAll()
                // Allow key-based callers through to service-level key validation.
                .requestMatchers(SecurityConfig::keyRequest).permitAll()
                .anyRequest().authenticated())
            .exceptionHandling(e->e.authenticationEntryPoint((req,res,error)->{ res.setHeader("WWW-Authenticate","Bearer");problem(res,401,"Please sign in to continue."); })
                .accessDeniedHandler((req,res,error)->{
                    com.eventrelay.servicea.ingress.IncomingRequestContext.reason(req,error instanceof org.springframework.security.web.csrf.CsrfException
                            ?com.eventrelay.servicea.ingress.IncomingRequestContext.Reason.CSRF_REJECTED:com.eventrelay.servicea.ingress.IncomingRequestContext.Reason.FORBIDDEN);
                    problem(res,403,"You do not have access to this resource.");
                }))
            .addFilterBefore(new JwtAuthenticationFilter(tokens),UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
    static void problem(HttpServletResponse response,int status,String detail) throws java.io.IOException {
        response.setStatus(status);response.setContentType("application/problem+json");
        response.getWriter().write("{\"status\":"+status+",\"detail\":\""+detail+"\"}");
    }
}

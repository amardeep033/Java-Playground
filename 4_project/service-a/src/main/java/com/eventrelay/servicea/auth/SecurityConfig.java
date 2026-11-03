package com.eventrelay.servicea.auth;

import jakarta.servlet.http.*;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.*;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.*;

@Configuration
public class SecurityConfig {
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
    @Bean CookieCsrfTokenRepository csrfRepository(@org.springframework.beans.factory.annotation.Value("${app.auth.cookie-secure:false}") boolean secure) {
        var repository=CookieCsrfTokenRepository.withHttpOnlyFalse();
        repository.setCookieCustomizer(cookie->cookie.path("/").sameSite("Lax").secure(secure));
        return repository;
    }
    @Bean SecurityFilterChain security(HttpSecurity http,JwtTokens tokens,AuthService auth,CookieCsrfTokenRepository csrf) throws Exception {
        http.sessionManagement(s->s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .formLogin(f->f.disable()).httpBasic(b->b.disable()).logout(l->l.disable())
            .requestCache(c->c.disable())
            .csrf(c->c.csrfTokenRepository(csrf).csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()).ignoringRequestMatchers(SecurityConfig::keyRequest))
            .authorizeHttpRequests(r->r
                .requestMatchers("/error").permitAll()
                .requestMatchers(HttpMethod.GET,"/api/v1/auth/csrf").permitAll()
                .requestMatchers(HttpMethod.POST,"/api/v1/users","/api/v1/auth/login").permitAll()
                .requestMatchers(SecurityConfig::keyRequest).permitAll()
                .requestMatchers("/api/v1/auth/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated())
            .exceptionHandling(e->e.authenticationEntryPoint((req,res,error)->problem(res,401,"Please sign in to continue."))
                .accessDeniedHandler((req,res,error)->problem(res,403,error instanceof CsrfException?"CSRF token missing or invalid. Refresh and try again.":"You do not have access to this resource.")))
            .addFilterBefore(new SessionFilter(tokens,auth),UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
    static void problem(HttpServletResponse response,int status,String detail) throws java.io.IOException {
        response.setStatus(status);response.setContentType("application/problem+json");
        response.getWriter().write("{\"status\":"+status+",\"detail\":\""+detail+"\"}");
    }
}

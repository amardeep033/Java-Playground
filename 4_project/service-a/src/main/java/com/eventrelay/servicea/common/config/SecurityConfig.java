package com.eventrelay.servicea.common.config;

import com.eventrelay.servicea.auth.JwtAuthenticationFilter;
import com.eventrelay.servicea.auth.JwtTokens;
import com.eventrelay.servicea.ingress.IncomingRequestContext;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.*;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfException;

@Configuration
public class SecurityConfig {

    // S1: SecurityFilterChain adds the middleware functionality
    @Bean
    //tokens our own - passed to JwtAuthenticationFilter
    SecurityFilterChain security(HttpSecurity http, JwtTokens tokens, CookieCsrfTokenRepository csrf) throws Exception {
        // stateless, disable defaults, request matcher, exception handling and add filter before
        http.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(f -> f.disable())
                .httpBasic(b -> b.disable())
                .logout(l -> l.disable())
                .requestCache(c -> c.disable())
                .csrf(c -> c.csrfTokenRepository(csrf).ignoringRequestMatchers(SecurityConfig::keyRequest))
                .authorizeHttpRequests(r -> r
                        .requestMatchers("/error").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/users", "/api/v1/auth/login", "/api/v1/auth/logout","/api/v1/auth/refresh").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/auth/csrf").permitAll()
                        .requestMatchers(SecurityConfig::keyRequest).permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(e -> e.authenticationEntryPoint((req, res, error) -> {
                    res.setHeader("WWW-Authenticate", "Bearer");
                    problem(res, 401, "Please sign in to continue.");
                })
                .accessDeniedHandler((req, res, error) -> {
                    IncomingRequestContext.reason(req, error instanceof CsrfException
                            ? IncomingRequestContext.Reason.CSRF_REJECTED
                            : IncomingRequestContext.Reason.FORBIDDEN);
                    problem(res, 403, "You do not have access to this resource.");
                }))
                //JwtAuthenticationFilter: OncePerRequestFilter
                .addFilterBefore(new JwtAuthenticationFilter(tokens), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    //utility fn for regex used in constructor
    public static boolean keyRequest(HttpServletRequest request) {
        String api_key = request.getHeader("X-API-Key"), path = request.getRequestURI(), method = request.getMethod();
        if (api_key == null || api_key.isBlank())
            return false;
        String root = "/api/v1/applications/[0-9a-fA-F-]{36}";
        return (method.equals("GET") && path
                .matches(root + "/(events(?:/[0-9a-fA-F-]{36})?|deliveries(?:/[0-9a-fA-F-]{36})?|activity-summary)"))
                || (method.equals("POST") && path.matches(root + "/(events|deliveries/[0-9a-fA-F-]{36}/replay)"));
    }

    //generic fn used in constructor
    public static void problem(HttpServletResponse response, int status, String detail) throws IOException {
        response.setStatus(status);
        response.setContentType("application/problem+json");
        response.getWriter().write("{\"status\":" + status + ",\"detail\":\"" + detail + "\"}");
    }

    //----------------------------------------------------

    //It prevents Spring Boot's default user setup from being created. It doesn't independently disable Basic login; you already do that in SecurityFilterChain
    @Bean
    UserDetailsService noBasicLogin() {
        return username -> {
            throw new UsernameNotFoundException("Use dashboard login");
        };
    }

    //Generates, saves and loads CSRF tokens using cookies
    @Bean
    CookieCsrfTokenRepository csrfRepository(@Value("${app.auth.cookie-secure:false}") boolean secure) {
        var repository = new CookieCsrfTokenRepository();
        repository.setCookiePath("/api/v1");
        repository.setCookieCustomizer(cookie -> cookie.httpOnly(true).secure(secure).sameSite("Lax"));
        return repository;
    }

    //----------------------------------------------------

    //list of public auths to ignore token -- called from JwtAuthenticationFilter.doFilterInternal
    public static boolean publicAuth(HttpServletRequest request) {
        String path = request.getRequestURI();
        return (request.getMethod().equals("GET") && path.equals("/api/v1/auth/csrf"))
                || (request.getMethod().equals("POST")
                        && Set.of("/api/v1/users", "/api/v1/auth/login", "/api/v1/auth/logout", "/api/v1/auth/refresh")
                                .contains(path));
    }

}

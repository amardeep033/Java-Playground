package com.eventrelay.servicea.auth;

import java.util.*;
import jakarta.servlet.http.*;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.servlet.*;
import org.springframework.web.servlet.config.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@Configuration
public class OwnershipConfig implements WebMvcConfigurer {
    private final JdbcTemplate db;
    public OwnershipConfig(JdbcTemplate db) { this.db=db; }
    @Override public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
            @Override public boolean preHandle(HttpServletRequest request,HttpServletResponse response,Object handler) {
                if(SecurityConfig.keyRequest(request)) return true; // App API authorization remains inside ingestion/monitoring services.
                SessionUser user=SessionUser.current();
                if(user==null) return true; // Public routes; all other routes require authentication in the filter chain.
                var variables=(Map<?,?>)request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
                if(variables==null) return true;
                if(variables.get("userId") instanceof String id && !id.equals(user.id().toString())) {
                    if(!user.admin() || request.getRequestURI().endsWith("/password")) deny();
                }
                if(variables.get("applicationId") instanceof String id && !user.admin()) {
                    Boolean owns=db.queryForObject("SELECT EXISTS(SELECT 1 FROM service_a.applications WHERE id=? AND user_id=? AND deleted_at IS NULL)",Boolean.class,UUID.fromString(id),user.id());
                    if(!Boolean.TRUE.equals(owns)) deny();
                }
                return true;
            }
        }).addPathPatterns("/api/v1/**");
    }
    private static void deny() { throw new ResponseStatusException(HttpStatus.FORBIDDEN,"You do not have access to this resource"); }
}

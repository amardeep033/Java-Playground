package com.eventrelay.servicea.auth;

import java.util.*;
import jakarta.servlet.http.*;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.*;
import org.springframework.web.servlet.config.annotation.*;
import com.eventrelay.servicea.workspace.WorkspaceAccess;

@Configuration
public class OwnershipConfig implements WebMvcConfigurer {
    private final WorkspaceAccess access;
    public OwnershipConfig(WorkspaceAccess access) { this.access=access; }
    @Override public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
            @Override public boolean preHandle(HttpServletRequest request,HttpServletResponse response,Object handler) {
                if(SecurityConfig.keyRequest(request)) return true; // Explicit keys are checked inside ingestion/monitoring.
                JwtUser user=JwtUser.current();if(user==null) return true;
                var variables=(Map<?,?>)request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
                if(variables==null) return true;
                String method=request.getMethod(),path=request.getRequestURI();
                boolean read=method.equals("GET")||method.equals("HEAD");
                if(variables.get("userId") instanceof String id && variables.get("clientId")==null) {
                    UUID target=UUID.fromString(id);
                    if(variables.get("applicationId") instanceof String appId) {
                        // The account prefix is navigation context; live client membership and app mappings decide access.
                        access.member(UUID.fromString(appId));
                    } else access.account(target,false);
                }
                if(variables.get("clientId") instanceof String id) {
                    access.clientRole(UUID.fromString(id));
                }
                if(variables.get("applicationId") instanceof String id) {
                    UUID app=UUID.fromString(id);access.member(app);
                    if(!read && (path.endsWith("/admin")||path.endsWith("/activation")||path.endsWith("/url")||path.endsWith("/subscriptions")||method.equals("DELETE"))) access.admin(app);
                }
                return true;
            }
        }).addPathPatterns("/api/v1/**");
    }
}

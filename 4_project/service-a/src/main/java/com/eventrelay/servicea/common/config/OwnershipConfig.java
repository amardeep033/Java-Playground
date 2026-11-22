package com.eventrelay.servicea.common.config;

import com.eventrelay.servicea.auth.JwtUser;
import com.eventrelay.servicea.auth.access.WorkspaceAccess;
import jakarta.servlet.http.*;
import java.util.*;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.*;
import org.springframework.web.servlet.config.annotation.*;

@Configuration
public class OwnershipConfig implements WebMvcConfigurer {
    private final WorkspaceAccess access;

    public OwnershipConfig(WorkspaceAccess access) {
        this.access = access;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
            @Override
            public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
                if (SecurityConfig.keyRequest(request))
                    return true; // Explicit keys are checked inside ingestion/monitoring.
                JwtUser authenticated_user = JwtUser.current();
                if (authenticated_user == null)
                    return true;
                var variables = (Map<?, ?>) request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
                if (variables == null)
                    return true;
                String method = request.getMethod(), path = request.getRequestURI();
                boolean read = method.equals("GET") || method.equals("HEAD");
                if (variables.get("userId") instanceof String user_id_text && variables.get("clientId") == null) {
                    UUID target_user_id = UUID.fromString(user_id_text);
                    if (variables.get("applicationId") instanceof String app_id_text) {
                        // The account prefix is navigation context; live client membership and app
                        // mappings decide access.
                        access.member(UUID.fromString(app_id_text));
                    } else
                        access.account(target_user_id, false);
                }
                if (variables.get("clientId") instanceof String client_id_text) {
                    access.clientRole(UUID.fromString(client_id_text));
                }
                if (variables.get("applicationId") instanceof String app_id_text) {
                    UUID app_id = UUID.fromString(app_id_text);
                    access.member(app_id);
                    if (!read && (path.endsWith("/admin") || path.endsWith("/activation") || path.endsWith("/url")
                            || path.endsWith("/subscriptions") || method.equals("DELETE")))
                        access.admin(app_id);
                }
                return true;
            }
        }).addPathPatterns("/api/v1/**");
    }
}

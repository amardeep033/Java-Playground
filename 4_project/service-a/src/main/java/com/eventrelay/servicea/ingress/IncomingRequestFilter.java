package com.eventrelay.servicea.ingress;

import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.regex.Pattern;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import static com.eventrelay.servicea.ingress.IncomingRequestContext.*;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE+20)
public class IncomingRequestFilter extends OncePerRequestFilter {
    private static final Pattern PATH=Pattern.compile("/api/v1/applications/([^/]+)/events/?");
    private static final org.slf4j.Logger LOG=org.slf4j.LoggerFactory.getLogger(IncomingRequestFilter.class);
    private final IncomingRequestStore store;
    public IncomingRequestFilter(IncomingRequestStore store) { this.store=store; }
    @Override protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getMethod().equals("POST") || !PATH.matcher(request.getRequestURI()).matches();
    }
    @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain) throws ServletException,IOException {
        var path=PATH.matcher(request.getRequestURI());path.matches();
        String target=path.group(1);
        // Invalid path segments may contain arbitrary text; never persist them as metadata.
        if(!target.matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")) target="INVALID_APPLICATION_ID";
        UUID id=UUID.randomUUID();Instant received=Instant.now();long started=System.nanoTime();
        request.setAttribute(ID,id);
        response.setHeader("X-Request-Id",id.toString());boolean failed=true;
        String key=request.getHeader("Idempotency-Key");boolean hasKey=key!=null&&!key.isBlank();
        String auth=request.getHeader("X-API-Key")!=null?"API_KEY":hasCookie(request)?"DASHBOARD":"NONE";
        try {
            // Commit the parent audit row before event ingestion can reference it.
            store.begin(new IncomingRequestStore.Entry(id,null,target,received,0,102,"PROCESSING",Reason.PROCESSING.name(),Reason.PROCESSING.message,
                    null,null,auth,hasKey,request.getRemoteAddr(),List.of(),null,null,null),request.getHeader("X-API-Key"));
        } catch(RuntimeException unavailable) {
            LOG.warn("Unable to initialize incoming request audit ({})",unavailable.getClass().getSimpleName());
            response.setStatus(503);response.setContentType("application/problem+json");
            response.getWriter().write("{\"status\":503,\"detail\":\"Unable to record request; please retry later\"}");return;
        }
        try { chain.doFilter(request,response);failed=false; }
        finally {
            int status=failed?500:response.getStatus();
            String outcome=status>=500?"ERROR":status>=200&&status<300?"ACCEPTED":"REJECTED";
            Reason reason=failed?Reason.SERVER_ERROR:request.getAttribute(REASON) instanceof Reason r?r:fallback(status);
            UUID event=outcome.equals("ACCEPTED") && request.getAttribute(EVENT) instanceof UUID value?value:null;
            @SuppressWarnings("unchecked")
            List<UUID> skipped=request.getAttribute(SKIPPED) instanceof List<?> values?(List<UUID>)values:List.of();
            try {
                // Store bounded metadata only; never cache bodies or persist credential/header values.
                store.append(new IncomingRequestStore.Entry(id,null,target,received,(System.nanoTime()-started)/1_000_000,status,outcome,
                        reason.name(),reason.message,event,(String)request.getAttribute(TYPE),auth,hasKey,request.getRemoteAddr(),skipped,null,null,(UUID)request.getAttribute(DUPLICATE)));
            } catch(RuntimeException unavailable) {
                // An audit DB failure must not turn an already committed event into a false rejection.
                LOG.warn("Unable to persist incoming request audit ({})",unavailable.getClass().getSimpleName());
            }
        }
    }
    private boolean hasCookie(HttpServletRequest request) {
        return request.getCookies()!=null && Arrays.stream(request.getCookies()).anyMatch(c->c.getName().equals(com.eventrelay.servicea.auth.JwtCookies.NAME));
    }
}

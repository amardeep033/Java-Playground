package com.eventrelay.servicea.ingress;

import com.eventrelay.servicea.ingress.IncomingRequestContext.Reason;
import com.eventrelay.servicea.ingress.dto.IncomingRequestEntry;

import com.eventrelay.servicea.auth.JwtCookies;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import static com.eventrelay.servicea.ingress.IncomingRequestContext.*;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20) //so that it comes before SecurityCheckFilter(auto called before controller - config is only for startup)
public class IncomingRequestFilter extends OncePerRequestFilter {

    private static final Pattern PATH = Pattern.compile("/api/v1/applications/([^/]+)/events/?");
    private static final Logger LOG = LoggerFactory.getLogger(IncomingRequestFilter.class);
    private final IncomingRequestStore store;

    public IncomingRequestFilter(IncomingRequestStore store) {
        this.store = store;
    }

    //-------------------------------------------------------

    //skips the entire filter 
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getMethod().equals("POST") || !PATH.matcher(request.getRequestURI()).matches();
    }

    //called if shouldFilter applies
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        //use regex - get target app id
        var path = PATH.matcher(request.getRequestURI());
        path.matches();
        String requested_app_id = path.group(1);
        if (!requested_app_id.matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")) {
            requested_app_id = "INVALID_APPLICATION_ID";
        }
            
        UUID request_id = UUID.randomUUID();
        Instant received = Instant.now();
        long started = System.nanoTime();
        request.setAttribute(ID, request_id);
        response.setHeader("X-Request-Id", request_id.toString());
        boolean failed = true;
        String idempotency_key = request.getHeader("Idempotency-Key");
        boolean has_key = idempotency_key != null && !idempotency_key.isBlank();
        //`DASHBOARD`, `API_KEY`, `NONE`
        String auth = request.getHeader("X-API-Key") != null ? "API_KEY" : hasCookie(request) ? "DASHBOARD" : "NONE";

        // Create the initial audit record before request processing; return 503 if audit storage fails.
        try {

            // DB write1: incoming_requests
            store.begin(
                    new IncomingRequestEntry(request_id, null, requested_app_id, received, 0, 102, "PROCESSING", Reason.PROCESSING.name(),
                            Reason.PROCESSING.message,
                            null, null, auth, has_key, request.getRemoteAddr(), List.of(), null, null, null),
                    request.getHeader("X-API-Key"));
        } catch (RuntimeException unavailable) {
            LOG.warn("Unable to initialize incoming request audit ({})", unavailable.getClass().getSimpleName());
            response.setStatus(503);
            response.setContentType("application/problem+json");
            response.getWriter().write("{\"status\":503,\"detail\":\"Unable to record request; please retry later\"}");
            return;
        }


        // Continue through security filters and controller, then record the final request outcome in finally.
        try {
            chain.doFilter(request, response);
            failed = false;
        } finally { //coming back to update the final status
            int status = failed ? 500 : response.getStatus();
            String outcome = status >= 500 ? "ERROR" : status >= 200 && status < 300 ? "ACCEPTED" : "REJECTED";
            Reason reason = failed ? Reason.SERVER_ERROR
                    : request.getAttribute(REASON) instanceof Reason r ? r : fallback(status);
            UUID event = outcome.equals("ACCEPTED") && request.getAttribute(EVENT) instanceof UUID value ? value : null;
            @SuppressWarnings("unchecked")
            List<UUID> skipped = request.getAttribute(SKIPPED) instanceof List<?> values ? (List<UUID>) values
                    : List.of();
            try {
                store.append(new IncomingRequestEntry(request_id, null, requested_app_id, received,
                        (System.nanoTime() - started) / 1_000_000, status, outcome,
                        reason.name(), reason.message, event, (String) request.getAttribute(TYPE), auth, has_key,
                        request.getRemoteAddr(), skipped, null, null, (UUID) request.getAttribute(DUPLICATE)));
            } catch (RuntimeException unavailable) {
                LOG.warn("Unable to persist incoming request audit ({})", unavailable.getClass().getSimpleName());
            }
        }
    }

    private boolean hasCookie(HttpServletRequest request) {
        return request.getCookies() != null && Arrays.stream(request.getCookies()).anyMatch(c -> c.getName().equals(JwtCookies.NAME));
    }
}

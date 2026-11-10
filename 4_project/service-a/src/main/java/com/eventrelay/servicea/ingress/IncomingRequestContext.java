package com.eventrelay.servicea.ingress;

import java.util.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.context.request.*;
import org.springframework.web.server.ResponseStatusException;

public final class IncomingRequestContext {
    private IncomingRequestContext() {}
    public static final String REASON="incoming.reason",EVENT="incoming.event",TYPE="incoming.type",SKIPPED="incoming.skipped",ID="incoming.id",DUPLICATE="incoming.duplicate";
    public enum Reason {
        PROCESSING("Request is being processed"), ACCEPTED("Event accepted"), DUPLICATE_ACCEPTED("Existing event returned; no duplicate delivery created"),
        NO_SUBSCRIBERS("Event accepted; no subscribed destinations"), INACTIVE_ENDPOINTS_SKIPPED("Event accepted; inactive endpoints skipped"),
        IDEMPOTENCY_KEY_MISSING("Idempotency-Key is missing or blank"), IDEMPOTENCY_KEY_INVALID("Idempotency-Key exceeds 200 characters"),
        IDEMPOTENCY_CONFLICT("Idempotency-Key was used with a different event"),
        MALFORMED_REQUEST("Malformed or unreadable request body"), INVALID_FIELDS("Request fields failed validation"),
        PAYLOAD_REQUIRED("payload is required"), PAYLOAD_TOO_LARGE("Payload exceeds 1 MB"),
        APPLICATION_INACTIVE("Application is inactive"), EVENT_TYPE_INACTIVE("Event type is inactive"), EVENT_TYPE_NOT_FOUND("Event type not found or deleted"),
        SIGNING_SECRET_MISSING("An active subscribed endpoint needs a signing secret"),
        UNAUTHORIZED("Missing, invalid or revoked credentials"), INVALID_JWT("Invalid or expired dashboard JWT"),
        FORBIDDEN("Caller cannot access this application"), CSRF_REJECTED("Missing or invalid CSRF token"),
        NOT_FOUND("Application or route not found"), UNSUPPORTED_MEDIA_TYPE("Unsupported request content type"),
        CONFLICT("Request conflicts with existing data"), SERVER_ERROR("Server could not complete the request"), REJECTED("Request rejected");
        public final String message;
        Reason(String message) { this.message=message; }
    }
    public static HttpServletRequest request() {
        return RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs ? attrs.getRequest() : null;
    }
    public static void reason(HttpServletRequest request,Reason reason) { if(request!=null) request.setAttribute(REASON,reason); }
    public static void reason(Reason reason) { reason(request(),reason); }
    public static ResponseStatusException reject(HttpStatus status,Reason reason) {
        reason(reason);return new ResponseStatusException(status,reason.message);
    }
    public static UUID id() { return request()!=null?(UUID)request().getAttribute(ID):null; }
    public static void duplicate(UUID source) { if(request()!=null&&source!=null) request().setAttribute(DUPLICATE,source); }
    public static void eventType(String value) { if(request()!=null) request().setAttribute(TYPE,value); }
    public static void skipped(List<UUID> ids) { if(request()!=null) request().setAttribute(SKIPPED,ids); }
    public static void accepted(UUID id,Reason reason) { if(request()!=null) request().setAttribute(EVENT,id);reason(reason); }
    public static Reason fallback(int status) {
        if(status>=500) return Reason.SERVER_ERROR;
        return switch(status) {
            case 400 -> Reason.MALFORMED_REQUEST; case 401 -> Reason.UNAUTHORIZED; case 403 -> Reason.FORBIDDEN;
            case 404 -> Reason.NOT_FOUND; case 409 -> Reason.CONFLICT; case 413 -> Reason.PAYLOAD_TOO_LARGE;
            case 415 -> Reason.UNSUPPORTED_MEDIA_TYPE;
            default -> status>=200 && status<300 ? Reason.ACCEPTED : Reason.REJECTED;
        };
    }
}

package com.eventrelay.servicea.event;

import com.eventrelay.servicea.apikey.ApiKeyService;
import com.eventrelay.servicea.auth.JwtUser;
import com.eventrelay.servicea.auth.access.ResourceAccess;
import com.eventrelay.servicea.common.config.RabbitMqConfig;
import com.eventrelay.servicea.delivery.messaging.DeliveryCommand;
import com.eventrelay.servicea.delivery.outbox.Outbox;
import com.eventrelay.servicea.delivery.persistence.DeliveryProjectionStore;
import com.eventrelay.servicea.endpoint.EndpointDeliveryReader;
import com.eventrelay.servicea.event.dto.EventApi;
import com.eventrelay.servicea.eventtype.EventTypeRepository;
import com.eventrelay.servicea.ingress.IncomingRequestContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import static com.eventrelay.servicea.ingress.IncomingRequestContext.*;

@Service
@Transactional
public class EventIngestionService {
    private final ApiKeyService keys;
    private final ResourceAccess access;
    private final EventTypeRepository types;
    private final EndpointDeliveryReader endpoints;
    private final EventStore store;
    private final DeliveryProjectionStore deliveries;
    private final Outbox outbox;
    private final ObjectMapper mapper;

    public EventIngestionService(ApiKeyService keys, ResourceAccess access, EventTypeRepository types,
            EndpointDeliveryReader endpoints, EventStore store, DeliveryProjectionStore deliveries, Outbox outbox, ObjectMapper mapper) {
        this.keys = keys; this.access = access; this.types = types; this.endpoints = endpoints;
        this.store = store; this.deliveries = deliveries; this.outbox = outbox; this.mapper = mapper;
    }

    public EventApi.Accepted receive(UUID appId, String apiKey, String idempotencyKey, EventApi.IngestRequest request) {
        IncomingRequestContext.eventType(request.eventType());
        authorize(appId, apiKey);
        if(idempotencyKey==null||idempotencyKey.isBlank()) throw reject(HttpStatus.BAD_REQUEST,Reason.IDEMPOTENCY_KEY_MISSING);
        if(idempotencyKey.length()>200) throw reject(HttpStatus.BAD_REQUEST,Reason.IDEMPOTENCY_KEY_INVALID);
        if(request.payload()==null||request.payload().isNull()) throw reject(HttpStatus.BAD_REQUEST,Reason.PAYLOAD_REQUIRED);
        String payload = json(canonical(request.payload()));
        if (payload.getBytes(StandardCharsets.UTF_8).length > 1_000_000) throw reject(HttpStatus.PAYLOAD_TOO_LARGE,Reason.PAYLOAD_TOO_LARGE);
        String hash = hash(request.eventType() + "\n" + payload);
        var app=access.lockedApplication(appId);
        authorize(appId, apiKey); // Re-check revocation while holding the app mutation lock.
        var existing = store.existing(appId, idempotencyKey);
        if (existing.isPresent()) {
            if (!existing.get().requestHash().equals(hash)) throw reject(HttpStatus.CONFLICT,Reason.IDEMPOTENCY_CONFLICT);
            duplicate(existing.get().incomingRequestId());
            accepted(existing.get().receipt().id(),Reason.DUPLICATE_ACCEPTED);
            return existing.get().receipt();
        }
        // Pause only new work; an already accepted idempotent request keeps its receipt.
        if(!app.isActive()) throw reject(HttpStatus.CONFLICT,Reason.APPLICATION_INACTIVE);
        var type = types.findByApplicationIdAndName(appId, request.eventType()).orElseThrow(() -> reject(HttpStatus.NOT_FOUND,Reason.EVENT_TYPE_NOT_FOUND));
        if(!type.isActive()) throw reject(HttpStatus.CONFLICT,Reason.EVENT_TYPE_INACTIVE);
        var targets = endpoints.findSubscribed(appId, type.getId());
        var inactive=endpoints.findInactiveSubscribed(appId,type.getId());
        skipped(inactive);
        if (targets.stream().anyMatch(endpoint -> endpoint.signingSecretEncrypted() == null)) {
            throw reject(HttpStatus.CONFLICT,Reason.SIGNING_SECRET_MISSING);
        }
        UUID eventId = UUID.randomUUID();
        Instant at = Instant.now().truncatedTo(ChronoUnit.MICROS);
        store.save(eventId, appId, type.getId(), type.getName(), idempotencyKey, hash, payload, targets.size(), at);
        var body = mapper.createObjectNode().put("id", eventId.toString()).put("type", type.getName()).put("createdAt", at.toString());
        body.set("data", canonical(request.payload()));
        String encoded = json(body);
        for (var endpoint : targets) {
            UUID deliveryId = UUID.randomUUID();
            UUID messageId = UUID.randomUUID();
            deliveries.delivery(deliveryId, eventId, appId, endpoint.id(), endpoint.url());
            // Immutable dispatch snapshot: later app/endpoint/mapping edits never rewrite accepted work.
            var command = new DeliveryCommand(1, messageId, deliveryId, eventId, appId, endpoint.id(), type.getName(),
                    endpoint.url(), encoded, endpoint.signingSecretEncrypted(), endpoint.signingSecretVersion(),
                    endpoint.rateLimitPerSecond(), endpoint.concurrencyLimit(), endpoint.timeoutMs(), endpoint.maxAttempts(), 1, at);
            command.validate();
            outbox.enqueue(messageId, deliveryId, RabbitMqConfig.DELIVERY_EXCHANGE, "delivery.requested", command);
        }
        accepted(eventId,!inactive.isEmpty()?Reason.INACTIVE_ENDPOINTS_SKIPPED:targets.isEmpty()?Reason.NO_SUBSCRIBERS:Reason.ACCEPTED);
        return new EventApi.Accepted(eventId, type.getName(), targets.size(), at);
    }

    @Transactional(readOnly = true)
    public EventApi.Details details(UUID appId, UUID eventId, String apiKey, boolean includeDeliveries) {
        authorize(appId, apiKey);
        return store.details(appId, eventId, includeDeliveries).orElseThrow(() -> ResourceAccess.notFound("Event"));
    }
    private void authorize(UUID appId, String apiKey) {
        var session = JwtUser.current();
        // Dashboard: only use the JWT cookie principal when X-API-Key is absent.
        // API-key revocation does not affect dashboard workspace access.
        if (apiKey == null && session != null) {
            access.application(appId); // ResourceAccess checks live workspace membership.
            return;
        }

        // Customer server: validate the explicit key. Invalid/revoked keys throw 401;
        // do not catch that failure and retry authentication with the JWT cookie.
        if (!keys.authenticate(apiKey).equals(appId)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "API key belongs to another application");
    }
    public static JsonNode canonical(JsonNode value) {
        if (value.isObject()) {
            ObjectNode result = JsonNodeFactory.instance.objectNode();
            var names = new ArrayList<String>(); value.fieldNames().forEachRemaining(names::add); names.sort(String::compareTo);
            names.forEach(name -> result.set(name, canonical(value.get(name)))); return result;
        }
        if (value.isArray()) { ArrayNode result = JsonNodeFactory.instance.arrayNode(); value.forEach(item -> result.add(canonical(item))); return result; }
        return value;
    }
    private String json(JsonNode value) {
        try { return mapper.writeValueAsString(value); } catch (JsonProcessingException ex) { throw new IllegalStateException("Cannot encode event", ex); }
    }
    private static String hash(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException ex) { throw new IllegalStateException(ex); }
    }
}

package com.eventrelay.servicea.events;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.Instant;
import java.util.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.eventrelay.servicea.config.RabbitMqConfig;
import com.eventrelay.servicea.messaging.*;
import com.eventrelay.servicea.repository.*;
import com.eventrelay.servicea.service.*;

@Service
@Transactional
public class EventIngestionService {
    private final ApiKeyService keys;
    private final ResourceAccess access;
    private final EventTypeRepository types;
    private final EndpointRepository endpoints;
    private final EventStore store;
    private final Outbox outbox;
    private final ObjectMapper mapper;

    public EventIngestionService(ApiKeyService keys, ResourceAccess access, EventTypeRepository types,
            EndpointRepository endpoints, EventStore store, Outbox outbox, ObjectMapper mapper) {
        this.keys = keys; this.access = access; this.types = types; this.endpoints = endpoints;
        this.store = store; this.outbox = outbox; this.mapper = mapper;
    }

    public EventApi.Accepted receive(UUID appId, String apiKey, String idempotencyKey, EventApi.IngestRequest request) {
        authorize(appId, apiKey);
        if (idempotencyKey == null || idempotencyKey.isBlank() || idempotencyKey.length() > 200) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Idempotency-Key must contain 1–200 characters");
        }
        if (request.payload() == null || request.payload().isNull()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "payload is required");
        String payload = json(canonical(request.payload()));
        if (payload.getBytes(StandardCharsets.UTF_8).length > 1_000_000) throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Payload exceeds 1 MB");
        String hash = hash(request.eventType() + "\n" + payload);
        access.lockedApplication(appId);
        authorize(appId, apiKey); // Re-check revocation while holding the app mutation lock.
        var existing = store.existing(appId, idempotencyKey);
        if (existing.isPresent()) {
            if (!existing.get().requestHash().equals(hash)) throw new ResponseStatusException(HttpStatus.CONFLICT, "Idempotency-Key was used with a different event");
            return existing.get().receipt();
        }
        var type = types.findByApplicationIdAndName(appId, request.eventType()).orElseThrow(() -> ResourceAccess.notFound("Event type"));
        var targets = endpoints.findSubscribed(appId, type.getId());
        if (targets.stream().anyMatch(endpoint -> endpoint.getSigningSecretEncrypted() == null)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Generate a signing secret for every enabled subscribed endpoint before sending");
        }
        UUID eventId = UUID.randomUUID();
        Instant at = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        store.save(eventId, appId, type.getId(), type.getName(), idempotencyKey, hash, payload, targets.size(), at);
        var body = mapper.createObjectNode().put("id", eventId.toString()).put("type", type.getName()).put("createdAt", at.toString());
        body.set("data", canonical(request.payload()));
        String encoded = json(body);
        for (var endpoint : targets) {
            UUID deliveryId = UUID.randomUUID();
            UUID messageId = UUID.randomUUID();
            store.delivery(deliveryId, eventId, appId, endpoint.getId(), endpoint.getUrl());
            var command = new DeliveryCommand(1, messageId, deliveryId, eventId, appId, endpoint.getId(), type.getName(),
                    endpoint.getUrl(), encoded, endpoint.getSigningSecretEncrypted(), endpoint.getSigningSecretVersion(),
                    endpoint.getRateLimitPerSecond(), endpoint.getConcurrencyLimit(), endpoint.getTimeoutMs(), endpoint.getMaxAttempts(), 1, at);
            command.validate();
            outbox.enqueue(messageId, deliveryId, RabbitMqConfig.DELIVERY_EXCHANGE, "delivery.requested", command);
        }
        return new EventApi.Accepted(eventId, type.getName(), targets.size(), at);
    }

    @Transactional(readOnly = true)
    public EventApi.Details details(UUID appId, UUID eventId, String apiKey, boolean includeDeliveries) {
        authorize(appId, apiKey);
        return store.details(appId, eventId, includeDeliveries).orElseThrow(() -> ResourceAccess.notFound("Event"));
    }
    private void authorize(UUID appId, String apiKey) {
        var session = com.eventrelay.servicea.auth.SessionUser.current();
        if (apiKey == null && session != null) {
            var owner = access.application(appId).getUserId();
            if (!session.admin() && !owner.equals(session.id())) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"You do not have access to this application");
            return;
        }

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

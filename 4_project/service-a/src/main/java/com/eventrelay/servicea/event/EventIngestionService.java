package com.eventrelay.servicea.event;

import com.eventrelay.servicea.event.dto.EventAccepted;
import com.eventrelay.servicea.event.dto.EventDetails;
import com.eventrelay.servicea.event.dto.IngestRequest;

import com.eventrelay.servicea.common.security.ApplicationAuthorization;
import com.eventrelay.servicea.auth.access.ResourceAccess;
import com.eventrelay.servicea.common.config.RabbitMqConfig;
import com.eventrelay.servicea.delivery.messaging.DeliveryCommand;
import com.eventrelay.servicea.delivery.outbox.Outbox;
import com.eventrelay.servicea.delivery.persistence.DeliveryProjectionStore;
import com.eventrelay.servicea.endpoint.EndpointDeliveryReader;
import com.eventrelay.servicea.eventtype.EventTypeRepository;
import com.eventrelay.servicea.ingress.IncomingRequestContext;
import com.eventrelay.servicea.ingress.IncomingRequestContext.Reason;
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
import static com.eventrelay.servicea.ingress.IncomingRequestContext.*;

@Service
@Transactional
public class EventIngestionService {
    private final ApplicationAuthorization authorization;
    private final ResourceAccess access;
    private final EventTypeRepository types;
    private final EndpointDeliveryReader endpoints;
    private final EventStore store;
    private final DeliveryProjectionStore deliveries;
    private final Outbox outbox;
    private final ObjectMapper mapper;

    public EventIngestionService(ApplicationAuthorization authorization, ResourceAccess access, EventTypeRepository types,
            EndpointDeliveryReader endpoints, EventStore store, DeliveryProjectionStore deliveries, Outbox outbox,
            ObjectMapper mapper) {
        this.authorization = authorization;
        this.access = access;
        this.types = types;
        this.endpoints = endpoints;
        this.store = store;
        this.deliveries = deliveries;
        this.outbox = outbox;
        this.mapper = mapper;
    }

    // -------------------------------------------------------------------

    // create
    public EventAccepted receive(UUID app_id, String api_key, String idempotency_key, IngestRequest request) {

        IncomingRequestContext.eventType(request.eventType());

        authorization.authorize(app_id, api_key); // check1: just before entry

        if (idempotency_key == null || idempotency_key.isBlank())
            throw reject(HttpStatus.BAD_REQUEST, Reason.IDEMPOTENCY_KEY_MISSING);

        if (idempotency_key.length() > 200)
            throw reject(HttpStatus.BAD_REQUEST, Reason.IDEMPOTENCY_KEY_INVALID);

        if (request.payload() == null || request.payload().isNull())
            throw reject(HttpStatus.BAD_REQUEST, Reason.PAYLOAD_REQUIRED);

        String payload = json(canonical(request.payload()));
        if (payload.getBytes(StandardCharsets.UTF_8).length > 1_000_000)
            throw reject(HttpStatus.PAYLOAD_TOO_LARGE, Reason.PAYLOAD_TOO_LARGE);

        String hash = hash(request.eventType() + "\n" + payload);
        var application = access.lockedApplication(app_id);
        authorization.authorize(app_id, api_key); // check2: re-check just before processing
        // db1: check duplicacy
        var existing = store.existing(app_id, idempotency_key);
        if (existing.isPresent()) {
            if (!existing.get().requestHash().equals(hash))
                throw reject(HttpStatus.CONFLICT, Reason.IDEMPOTENCY_CONFLICT); //new but with conflicted idempotency, here hash check
            duplicate(existing.get().incomingRequestId());
            accepted(existing.get().receipt().id(), Reason.DUPLICATE_ACCEPTED); //actual idempotency check, here key check 
            return existing.get().receipt();
        }

        if (!application.isActive())
            throw reject(HttpStatus.CONFLICT, Reason.APPLICATION_INACTIVE);

        var type = types.findByApplicationIdAndName(app_id, request.eventType())
                .orElseThrow(() -> reject(HttpStatus.NOT_FOUND, Reason.EVENT_TYPE_NOT_FOUND));

        if (!type.isActive())
            throw reject(HttpStatus.CONFLICT, Reason.EVENT_TYPE_INACTIVE);

        // find all subscribers
        var targets = endpoints.findSubscribed(app_id, type.getId());
        var inactive = endpoints.findInactiveSubscribed(app_id, type.getId());
        skipped(inactive);
        if (targets.stream().anyMatch(endpoint -> endpoint.signingSecretEncrypted() == null)) {
            throw reject(HttpStatus.CONFLICT, Reason.SIGNING_SECRET_MISSING);
        }

        UUID event_id = UUID.randomUUID();
        Instant at = Instant.now().truncatedTo(ChronoUnit.MICROS);
        // db2: initial save in received_events
        // DB write2: received_events
        store.save(event_id, app_id, type.getId(), type.getName(), idempotency_key, hash, payload, targets.size(), at);
        var body = mapper.createObjectNode().put("id", event_id.toString()).put("type", type.getName()).put("createdAt",
                at.toString());
        body.set("data", canonical(request.payload()));
        String encoded = json(body);

        // for each destination: Insert delivery_projections + Insert outbox_messages
        for (var endpoint : targets) {
            UUID delivery_id = UUID.randomUUID();
            UUID message_id = UUID.randomUUID();
            // DB write3: delivery_projections
            deliveries.delivery(delivery_id, event_id, app_id, endpoint.id(), endpoint.url());

            // prepare the msg using contract and validate
            var command = new DeliveryCommand(1, message_id, delivery_id, event_id, app_id, endpoint.id(), type.getName(),
                    endpoint.url(), encoded, endpoint.signingSecretEncrypted(), endpoint.signingSecretVersion(),
                    endpoint.rateLimitPerSecond(), endpoint.concurrencyLimit(), endpoint.timeoutMs(),
                    endpoint.maxAttempts(), 1, at);
            command.validate();

            // DB write4: outbox_messages
            outbox.enqueue(message_id, delivery_id, RabbitMqConfig.DELIVERY_EXCHANGE, "delivery.requested", command);
        }
        accepted(event_id, !inactive.isEmpty() ? Reason.INACTIVE_ENDPOINTS_SKIPPED
                : targets.isEmpty() ? Reason.NO_SUBSCRIBERS : Reason.ACCEPTED);
        return new EventAccepted(event_id, type.getName(), targets.size(), at);
    }

    private static String hash(String value) {
        try {
            return HexFormat.of()
                    .formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static JsonNode canonical(JsonNode value) {
        if (value.isObject()) {
            ObjectNode result = JsonNodeFactory.instance.objectNode();
            var names = new ArrayList<String>();
            value.fieldNames().forEachRemaining(names::add);
            names.sort(String::compareTo);
            names.forEach(name -> result.set(name, canonical(value.get(name))));
            return result;
        }
        if (value.isArray()) {
            ArrayNode result = JsonNodeFactory.instance.arrayNode();
            value.forEach(item -> result.add(canonical(item)));
            return result;
        }
        return value;
    }

    private String json(JsonNode value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Cannot encode event", ex);
        }
    }

    // -------------------------------------------------------------------

    // get specific
    @Transactional(readOnly = true)
    public EventDetails details(UUID app_id, UUID event_id, String api_key, boolean include_deliveries) {
        authorization.authorize(app_id, api_key);
        // db3: get specific detail
        return store.details(app_id, event_id, include_deliveries).orElseThrow(() -> ResourceAccess.notFound("Event"));
    }

}

// incoming_requests - what was received -- DB write1
// received_events - what was accepted -- DB write2
// outbox_messages - what was send to service b for delivery -- DB write4
// delivery_projections - summary + latest -- DB write3 + DB write6
// delivery_attempt_projections - all attempts -- DB write5

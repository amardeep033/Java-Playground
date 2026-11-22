package com.eventrelay.servicea.monitoring;

import com.eventrelay.servicea.monitoring.dto.DeliveryDetails;
import com.eventrelay.servicea.monitoring.dto.DeliverySummary;
import com.eventrelay.servicea.monitoring.dto.EventSummary;
import com.eventrelay.servicea.monitoring.dto.MonitoringSummary;
import com.eventrelay.servicea.monitoring.dto.ReplayReceipt;

import com.eventrelay.servicea.common.security.ApplicationAuthorization;
import com.eventrelay.servicea.auth.access.ResourceAccess;
import com.eventrelay.servicea.common.config.RabbitMqConfig;
import com.eventrelay.servicea.common.dto.PageResult;
import com.eventrelay.servicea.common.search.ListSearch;
import com.eventrelay.servicea.delivery.messaging.DeliveryCommand;
import com.eventrelay.servicea.delivery.outbox.Outbox;
import com.eventrelay.servicea.endpoint.EndpointDeliveryReader;
import com.eventrelay.servicea.delivery.persistence.DeliveryProjectionStore;
import com.eventrelay.servicea.eventtype.EventTypeRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class MonitoringService {
    private final ApplicationAuthorization authorization;
    private final ResourceAccess access;
    private final MonitoringStore store;
    private final DeliveryProjectionStore deliveries;
    private final EndpointDeliveryReader endpoints;
    private final EventTypeRepository types;
    private final Outbox outbox;

    public MonitoringService(ApplicationAuthorization authorization, ResourceAccess access, MonitoringStore store,
            DeliveryProjectionStore deliveries,
            EndpointDeliveryReader endpoints, EventTypeRepository types, Outbox outbox) {
        this.authorization = authorization;
        this.access = access;
        this.store = store;
        this.deliveries = deliveries;
        this.endpoints = endpoints;
        this.types = types;
        this.outbox = outbox;
    }

    // ------------------------------------------------------------------

    public PageResult<EventSummary> events(UUID app_id, String api_key, String status, String q, int page,
            int size) {
        authorization.authorize(app_id, api_key);
        validate(status, q, page, size, Set.of("PENDING", "SUCCEEDED", "FAILED", "NO_SUBSCRIBERS"));
        return store.events(app_id, status, q, page, size);
    }

    // ------------------------------------------------------------------

    public PageResult<DeliverySummary> deliveries(UUID app_id, String api_key, String status, String q,
            UUID event_id, UUID endpoint_id, boolean include_replayed, int page, int size) {
        authorization.authorize(app_id, api_key);
        validate(status, q, page, size, Set.of("PENDING", "SUCCEEDED", "DEAD"));
        return store.deliveries(app_id, status, q, event_id, endpoint_id, include_replayed, page, size);
    }

    public DeliveryDetails delivery(UUID app_id, UUID delivery_id, String api_key) {
        authorization.authorize(app_id, api_key);
        var delivery = store.delivery(app_id, delivery_id).orElseThrow(() -> ResourceAccess.notFound("Delivery"));
        return new DeliveryDetails(delivery, store.requestBody(delivery_id), store.attempts(delivery_id));
    }

    @Transactional
    public ReplayReceipt replay(UUID app_id, UUID source_delivery_id, String api_key, String idempotency_key) {
        
        // check1: Before locking: reject unauthorized requests early
        authorization.authorize(app_id, api_key);

        if (idempotency_key == null || idempotency_key.isBlank() || idempotency_key.length() > 200)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Idempotency-Key must contain 1–200 characters");

        access.lockedApplication(app_id);

        // check2: After locking: confirm the credentials/access are still valid before replaying the delivery
        authorization.authorize(app_id, api_key);

        var existing = store.replay(app_id, idempotency_key, source_delivery_id);
        if (existing.isPresent())
            return existing.get();

        store.lock(app_id, source_delivery_id);

        var original = store.delivery(app_id, source_delivery_id).orElseThrow(() -> ResourceAccess.notFound("Delivery"));

        if (!original.canReplay())
            throw new ResponseStatusException(HttpStatus.CONFLICT, original.replayReason());

        var endpoint = endpoints.findByIdAndApplicationId(original.endpointId(), app_id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "Endpoint has been deleted"));

        UUID type_id = store.eventTypeId(original.eventId());
        if (!types.findByIdAndApplicationId(type_id, app_id).isPresent())
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Event type has been deleted");

        UUID delivery_id = UUID.randomUUID(), message_id = UUID.randomUUID();
        Instant at = Instant.now().truncatedTo(ChronoUnit.MICROS);
        String body = store.rawBody(source_delivery_id);
        deliveries.delivery(delivery_id, original.eventId(), app_id, endpoint.id(), endpoint.url(), source_delivery_id);
        var command = new DeliveryCommand(1, message_id, delivery_id, original.eventId(), app_id, endpoint.id(),
                original.eventType(), endpoint.url(), body,
                endpoint.signingSecretEncrypted(), endpoint.signingSecretVersion(), endpoint.rateLimitPerSecond(),
                endpoint.concurrencyLimit(), endpoint.timeoutMs(), endpoint.maxAttempts(), 1, at, source_delivery_id);
        command.validate();
        outbox.enqueue(message_id, delivery_id, RabbitMqConfig.DELIVERY_EXCHANGE, "delivery.requested", command);
        store.replayRequest(app_id, idempotency_key, delivery_id, at);
        return new ReplayReceipt(delivery_id, original.eventId(), source_delivery_id, at);
    }

    // ------------------------------------------------------------------

    public MonitoringSummary summary(UUID app_id, String api_key) {
        authorization.authorize(app_id, api_key);
        return store.summary(app_id);
    }

    // ------------------------------------------------------------------

    private void validate(String status, String q, int page, int size, Set<String> states) {
        ListSearch.choices(status, states);
        if (page < 0 || size < 1 || size > 100 || q.length() > 200)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid filters or pagination (page >= 0, size 1–100)");
    }
}

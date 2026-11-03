package com.eventrelay.serviceb.delivery;

import java.time.Instant;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.eventrelay.serviceb.config.RabbitMqConfig;
import com.eventrelay.serviceb.messaging.*;

@Service
public class DeliveryService {
    private final DeliveryStore store;
    private final WebhookSender sender;
    private final EndpointGates gates;
    private final Outbox outbox;
    private final long retryDelay;
    public DeliveryService(DeliveryStore store, WebhookSender sender, EndpointGates gates, Outbox outbox,
                           @Value("${app.retry-delay-ms:30000}") long retryDelay) {
        this.store = store; this.sender = sender; this.gates = gates; this.outbox = outbox; this.retryDelay = retryDelay;
    }
    // The delivery row lock serializes duplicates across workers and instances.
    // ACK happens in the listener only after this transaction has committed.
    @Transactional
    public boolean process(DeliveryCommand incoming) {
        incoming.validate();
        var delivery = store.lock(incoming);
        var snapshot = delivery.command();
        if (!snapshot.eventId().equals(incoming.eventId()) || !snapshot.applicationId().equals(incoming.applicationId())
                || !snapshot.endpointId().equals(incoming.endpointId())) throw new IllegalArgumentException("Conflicting delivery identity");
        if (store.seen(incoming.messageId()) || delivery.status().equals("SUCCEEDED") || delivery.status().equals("DEAD")
                || incoming.attemptNumber() <= delivery.attempts()) {
            store.processed(incoming.messageId(), incoming.deliveryId());
            return delivery.status().equals("DEAD");
        }
        int attempt = delivery.attempts() + 1;
        if (incoming.attemptNumber() != attempt) throw new IllegalArgumentException("Out-of-order attempt");
        WebhookSender.Result result;
        try (var permit = gates.acquire(snapshot.endpointId(), snapshot.rateLimitPerSecond(), snapshot.concurrencyLimit())) {
            result = sender.send(snapshot, attempt);
        } catch (InterruptedException ex) { Thread.currentThread().interrupt(); throw new IllegalStateException("Delivery interrupted", ex); }
        String status = result.succeeded() ? "SUCCEEDED" : result.retryable() && attempt < snapshot.maxAttempts() ? "RETRY_SCHEDULED" : "DEAD";
        Instant nextAttempt = status.equals("RETRY_SCHEDULED") ? Instant.now().plusMillis(retryDelay) : null;
        store.result(delivery, attempt, status, result, nextAttempt);
        UUID updateId = UUID.randomUUID();
        var update = new DeliveryStatusMessage(1, updateId, delivery.id(), snapshot.eventId(), snapshot.applicationId(), snapshot.endpointId(),
                status, attempt, delivery.sequence() + 1, result.httpStatus(), result.error(), nextAttempt, result.completedAt(),
                new DeliveryStatusMessage.Attempt(attempt, result.httpStatus(), result.error(), result.startedAt(), result.completedAt(), result.responseBody(), result.contentType(), result.responseTruncated()));
        outbox.enqueue(updateId, delivery.id(), RabbitMqConfig.STATUS_EXCHANGE, "delivery.updated", update);
        if (status.equals("RETRY_SCHEDULED")) {
            var retry = snapshot.forAttempt(attempt + 1);
            outbox.enqueue(retry.messageId(), delivery.id(), RabbitMqConfig.RETRY_EXCHANGE, "delivery.retry", retry);
        }
        store.processed(incoming.messageId(), incoming.deliveryId());
        return status.equals("DEAD");
    }
}

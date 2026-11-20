package com.eventrelay.servicea.delivery.persistence;

import com.eventrelay.servicea.delivery.messaging.DeliveryStatusMessage;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Delivery writes join the caller's event, replay, status or reconciliation transaction. */
@Repository
@Transactional(propagation = Propagation.MANDATORY)
public class DeliveryProjectionStore {
    private final JdbcTemplate jdbc;

    public DeliveryProjectionStore(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public void delivery(UUID id, UUID eventId, UUID appId, UUID endpointId, String url) {
        delivery(id, eventId, appId, endpointId, url, null);
    }
    public void delivery(UUID id, UUID eventId, UUID appId, UUID endpointId, String url, UUID replayOfId) {
        jdbc.update("INSERT INTO service_a.delivery_projections (id, event_id, application_id, endpoint_id, endpoint_url, replay_of_id) VALUES (?, ?, ?, ?, ?, ?)", id, eventId, appId, endpointId, url, replayOfId);
    }

    private record Projection(long sequence,String status) {}
    public void applyStatus(DeliveryStatusMessage message) {
        message.validate();
        var rows = jdbc.query("SELECT last_sequence,status FROM service_a.delivery_projections WHERE id = ? AND event_id = ? AND application_id = ? AND endpoint_id = ? FOR UPDATE",
                (rs, n) -> new Projection(rs.getLong(1),rs.getString(2)), message.deliveryId(), message.eventId(), message.applicationId(), message.endpointId());
        if (rows.isEmpty()) throw new IllegalArgumentException("Unknown delivery status target");
        int inserted = jdbc.update("INSERT INTO service_a.status_inbox(message_id, delivery_id) VALUES (?, ?) ON CONFLICT DO NOTHING", message.messageId(), message.deliveryId());
        if (inserted == 0) return;
        // A terminal delivery is immutable. Replays have their own delivery ID.
        var current=rows.getFirst();
        if(Set.of("SUCCEEDED","DEAD").contains(current.status()) && !current.status().equals(message.status())) return;
        for (var attempt : message.attempts()) {
            jdbc.update("INSERT INTO service_a.delivery_attempt_projections (delivery_id, attempt_number, http_status, error, started_at, completed_at, response_body, content_type, response_truncated) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) ON CONFLICT DO NOTHING",
                    message.deliveryId(), attempt.attemptNumber(), attempt.httpStatus(), attempt.error(), Timestamp.from(attempt.startedAt()), Timestamp.from(attempt.completedAt()), attempt.responseBody(), attempt.contentType(), attempt.responseTruncated());
        }
        if (message.sequence() <= current.sequence() || Set.of("SUCCEEDED","DEAD").contains(current.status())) return;
        jdbc.update("""
            UPDATE service_a.delivery_projections SET status = ?, attempt_count = ?, last_sequence = ?,
                http_status = ?, last_error = ?, updated_at = ? WHERE id = ?
            """, message.status(), message.attemptCount(), message.sequence(), message.httpStatus(), message.error(),
                Timestamp.from(message.updatedAt()), message.deliveryId());
    }
}

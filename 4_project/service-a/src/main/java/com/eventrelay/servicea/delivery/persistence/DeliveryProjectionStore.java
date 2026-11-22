package com.eventrelay.servicea.delivery.persistence;

import com.eventrelay.servicea.delivery.messaging.DeliveryStatusMessage;
import java.sql.Timestamp;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(propagation = Propagation.MANDATORY) // an existing transaction is required + Join that transaction
public class DeliveryProjectionStore {
    private final JdbcTemplate jdbc;

    public DeliveryProjectionStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // --------------------------------------------

    // DB write3: initial delivery entry
    public void delivery(UUID delivery_id, UUID event_id, UUID app_id, UUID endpoint_id, String url) {
        delivery(delivery_id, event_id, app_id, endpoint_id, url, null);
    }

    // replay_of_id included
    public void delivery(UUID delivery_id, UUID event_id, UUID app_id, UUID endpoint_id, String url,
            UUID replay_of_id) {
        jdbc.update(
                "INSERT INTO service_a.delivery_projections (id, event_id, application_id, endpoint_id, endpoint_url, replay_of_id) VALUES (?, ?, ?, ?, ?, ?)",
                delivery_id, event_id, app_id, endpoint_id, url, replay_of_id);
    }

    // --------------------------------------------

    private record Projection(long sequence, String status) {
    }

    public void applyStatus(DeliveryStatusMessage message) {

        // check if row exist for update
        var rows = jdbc.query(
                "SELECT last_sequence,status FROM service_a.delivery_projections WHERE id = ? AND event_id = ? AND application_id = ? AND endpoint_id = ? FOR UPDATE",
                (rs, n) -> new Projection(rs.getLong(1), rs.getString(2)), message.deliveryId(), message.eventId(),
                message.applicationId(), message.endpointId());
        if (rows.isEmpty())
            throw new IllegalArgumentException("Unknown delivery status target");

        // records which status messages Service A has already processed, preventing duplicate processing incase of crash before acknowledging rabbitmq
        // main protection when reconciliation has already processed the completion
        int inserted = jdbc.update(
                "INSERT INTO service_a.status_inbox(message_id, delivery_id) VALUES (?, ?) ON CONFLICT DO NOTHING",
                message.messageId(), message.deliveryId());
        if (inserted == 0)
            return;

        // if existing row already with SUCCEEDED or DEAD, return 
        var current = rows.getFirst();
        if (Set.of("SUCCEEDED", "DEAD").contains(current.status()) && !current.status().equals(message.status()))
            return;

        for (var attempt : message.attempts()) {
            // DB write5: insert into delivery_attempt_projections
            jdbc.update(
                    "INSERT INTO service_a.delivery_attempt_projections (delivery_id, attempt_number, http_status, error, started_at, completed_at, response_body, content_type, response_truncated) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) ON CONFLICT DO NOTHING",
                    message.deliveryId(), attempt.attemptNumber(), attempt.httpStatus(), attempt.error(),
                    Timestamp.from(attempt.startedAt()), Timestamp.from(attempt.completedAt()), attempt.responseBody(),
                    attempt.contentType(), attempt.responseTruncated());
        }

        // just for defense : so that nothing goes wrong -- dont update if seq in db is greater then incoming
        if (message.sequence() <= current.sequence() || Set.of("SUCCEEDED", "DEAD").contains(current.status()))
            return;

        // DB write6: update delivery_projections
        jdbc.update("""
                UPDATE service_a.delivery_projections SET status = ?, attempt_count = ?, last_sequence = ?,
                    http_status = ?, last_error = ?, updated_at = ? WHERE id = ?
                """, message.status(), message.attemptCount(), message.sequence(), message.httpStatus(),
                message.error(),
                Timestamp.from(message.updatedAt()), message.deliveryId());
    }
}

// inbox first, terminal check second:
// Because status_inbox is effectively a record of messages that Service A has handled, not necessarily messages whose status was applied
// That includes:
// - Messages successfully applied.
// - Messages ignored because they are stale.
// - Messages ignored because they conflict with a terminal state.

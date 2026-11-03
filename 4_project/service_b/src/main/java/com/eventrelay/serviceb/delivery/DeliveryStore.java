package com.eventrelay.serviceb.delivery;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import com.eventrelay.serviceb.messaging.DeliveryCommand;

@Repository
public class DeliveryStore {
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;
    public DeliveryStore(JdbcTemplate jdbc, ObjectMapper mapper) { this.jdbc = jdbc; this.mapper = mapper; }
    public record Delivery(UUID id, DeliveryCommand command, String status, int attempts, long sequence) {}
    public Delivery lock(DeliveryCommand incoming) {
        try {
            jdbc.update("INSERT INTO service_b.deliveries (id, event_id, application_id, endpoint_id, command, replay_of_id) VALUES (?, ?, ?, ?, ?::jsonb, ?) ON CONFLICT DO NOTHING",
                    incoming.deliveryId(), incoming.eventId(), incoming.applicationId(), incoming.endpointId(), mapper.writeValueAsString(incoming), incoming.replayOfId());
        } catch (JsonProcessingException ex) { throw new IllegalArgumentException("Invalid delivery command", ex); }
        var records = jdbc.query("SELECT id, command::text, status, attempt_count, status_sequence FROM service_b.deliveries WHERE id = ? AND deleted_at IS NULL FOR UPDATE",
                (rs, n) -> {
                    try { return new Delivery(rs.getObject("id", UUID.class), mapper.readValue(rs.getString("command"), DeliveryCommand.class), rs.getString("status"), rs.getInt("attempt_count"), rs.getLong("status_sequence")); }
                    catch (JsonProcessingException ex) { throw new IllegalStateException("Invalid stored delivery", ex); }
                }, incoming.deliveryId());
        if (records.isEmpty()) throw new IllegalArgumentException("Conflicting delivery identity");
        return records.getFirst();
    }
    public boolean seen(UUID messageId) { return Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM service_b.delivery_inbox WHERE message_id = ?)", Boolean.class, messageId)); }
    public void processed(UUID messageId, UUID deliveryId) { jdbc.update("INSERT INTO service_b.delivery_inbox(message_id, delivery_id) VALUES (?, ?) ON CONFLICT DO NOTHING", messageId, deliveryId); }
    public void result(Delivery delivery, int attempt, String status, WebhookSender.Result result, Instant nextAttempt) {
        jdbc.update("INSERT INTO service_b.delivery_attempts (id, delivery_id, attempt_number, http_status, error, started_at, completed_at, response_body, content_type, response_truncated, monitoring_exported_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, now())",
                UUID.randomUUID(), delivery.id(), attempt, result.httpStatus(), result.error(), Timestamp.from(result.startedAt()), Timestamp.from(result.completedAt()), result.responseBody(), result.contentType(), result.responseTruncated());
        jdbc.update("UPDATE service_b.deliveries SET status = ?, attempt_count = ?, status_sequence = ?, http_status = ?, last_error = ?, next_attempt_at = ?, updated_at = ? WHERE id = ?",
                status, attempt, delivery.sequence() + 1, result.httpStatus(), result.error(), nextAttempt == null ? null : Timestamp.from(nextAttempt), Timestamp.from(result.completedAt()), delivery.id());
    }
    public Optional<Map<String, Object>> details(UUID id) {
        var rows = jdbc.queryForList("SELECT id, event_id, application_id, endpoint_id, status, attempt_count, http_status, last_error, next_attempt_at, created_at, updated_at, replay_of_id FROM service_b.deliveries WHERE id = ? AND deleted_at IS NULL", id);
        if (rows.isEmpty()) return Optional.empty();
        var result = rows.getFirst();
        result.put("attempts", jdbc.queryForList("SELECT attempt_number, http_status, error, started_at, completed_at, response_body, content_type, response_truncated FROM service_b.delivery_attempts WHERE delivery_id = ? ORDER BY attempt_number", id));
        return Optional.of(result);
    }
}

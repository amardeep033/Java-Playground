package com.eventrelay.serviceb.messaging;

import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.TimeUnit;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class Outbox {
    private static final String TABLE = "service_b.outbox_messages";
    @org.springframework.beans.factory.annotation.Value("${app.outbox.enabled:true}")
    private boolean enabled = true;
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;
    private final RabbitTemplate rabbit;

    public Outbox(JdbcTemplate jdbc, ObjectMapper mapper, RabbitTemplate rabbit) {
        this.jdbc = jdbc; this.mapper = mapper; this.rabbit = rabbit;
    }
    // Caller owns the DB transaction: state and outgoing messages commit together.
    public void enqueue(UUID id, UUID aggregateId, String exchange, String routingKey, Object payload) {
        try {
            jdbc.update("INSERT INTO " + TABLE + " (id, aggregate_id, exchange_name, routing_key, payload) VALUES (?, ?, ?, ?, ?::jsonb) ON CONFLICT (id) DO NOTHING",
                    id, aggregateId, exchange, routingKey, mapper.writeValueAsString(payload));
        } catch (JsonProcessingException ex) { throw new IllegalStateException("Cannot encode outgoing message", ex); }
    }

    @Scheduled(fixedDelayString = "${app.outbox.poll-delay-ms:250}")
    public void publishPending() {
        if (!enabled) return;
        for (int n = 0; n < 20; n++) {
            UUID claim = UUID.randomUUID();
            var rows = jdbc.query("""
                WITH candidate AS (
                    SELECT id FROM service_b.outbox_messages
                    WHERE published_at IS NULL AND available_at <= now()
                      AND (locked_until IS NULL OR locked_until < now())
                    ORDER BY created_at, id FOR UPDATE SKIP LOCKED LIMIT 1
                )
                UPDATE service_b.outbox_messages o SET claim_token = ?, locked_until = now() + interval '30 seconds',
                    attempt_count = o.attempt_count + 1
                FROM candidate c WHERE o.id = c.id
                RETURNING o.id, o.exchange_name, o.routing_key, o.payload::text
                """, (rs, row) -> new Pending(rs.getObject(1, UUID.class), rs.getString(2), rs.getString(3), rs.getString(4)), claim);
            if (rows.isEmpty()) return;
            var pending = rows.getFirst();
            try {
                var properties = new MessageProperties();
                properties.setMessageId(pending.id().toString());
                properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
                properties.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                var correlation = new CorrelationData(pending.id().toString());
                rabbit.send(pending.exchange(), pending.routingKey(), new Message(pending.json().getBytes(java.nio.charset.StandardCharsets.UTF_8), properties), correlation);
                var confirm = correlation.getFuture().get(5, TimeUnit.SECONDS);
                if (!confirm.isAck() || correlation.getReturned() != null) throw new IllegalStateException("Broker did not route and confirm message");
                jdbc.update("UPDATE " + TABLE + " SET published_at = now(), locked_until = NULL, claim_token = NULL, last_error = NULL WHERE id = ? AND claim_token = ?", pending.id(), claim);
            } catch (Exception ex) {
                if (ex instanceof InterruptedException) Thread.currentThread().interrupt();
                jdbc.update("UPDATE " + TABLE + " SET locked_until = NULL, claim_token = NULL, available_at = now() + interval '2 seconds', last_error = ? WHERE id = ? AND claim_token = ?",
                        ex.getClass().getSimpleName(), pending.id(), claim);
                return;
            }
        }
    }
    private record Pending(UUID id, String exchange, String routingKey, String json) {}
}

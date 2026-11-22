package com.eventrelay.servicea.event;

import com.eventrelay.servicea.event.dto.EventAccepted;
import com.eventrelay.servicea.event.dto.EventDeliveryView;
import com.eventrelay.servicea.event.dto.EventDetails;

import com.eventrelay.servicea.ingress.IncomingRequestContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class EventStore {
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;

    public EventStore(JdbcTemplate jdbc, ObjectMapper mapper) {
        this.jdbc = jdbc;
        this.mapper = mapper;
    }

    // --------------------------------------------------------------

    // for duplicacy check
    public Optional<Stored> existing(UUID app_id, String idempotency_key) {
        return jdbc.query(
                "SELECT id,event_type,delivery_count,created_at,request_hash,incoming_request_id FROM service_a.received_events WHERE application_id=? AND idempotency_key=?",
                (rs, n) -> {
                    var receipt = new EventAccepted(rs.getObject("id", UUID.class), rs.getString("event_type"),
                            rs.getInt("delivery_count"), rs.getTimestamp("created_at").toInstant());
                    return new Stored(rs.getString("request_hash"), receipt,
                            rs.getObject("incoming_request_id", UUID.class));
                }, app_id, idempotency_key).stream().findFirst();
    }

    public record Stored(String requestHash, EventAccepted receipt, UUID incomingRequestId) {
        public Stored(String hash, EventAccepted receipt) {
            this(hash, receipt, null);
        }
    }

    // --------------------------------------------------------------

    // DB write2: save in received_events
    public void save(UUID event_id, UUID app_id, UUID type_id, String type, String idempotency_key, String hash, String payload, int count,
            Instant at) {
        jdbc.update(
                """
                        INSERT INTO service_a.received_events
                            (id, application_id, event_type_id, event_type, idempotency_key, request_hash, payload, delivery_count, created_at, incoming_request_id)
                        VALUES (?, ?, ?, ?, ?, ?, ?::jsonb, ?, ?, ?)
                        """,
                event_id, app_id, type_id, type, idempotency_key, hash, payload, count, Timestamp.from(at), IncomingRequestContext.id());
        jdbc.update(
                "UPDATE service_a.incoming_requests SET outcome='ACCEPTED',http_status=202,reason_code='ACCEPTED',reason='Event accepted',event_type=? WHERE id=?",
                type, IncomingRequestContext.id());
    }

    // --------------------------------------------------------------

    // get specific detail
    public Optional<EventDetails> details(UUID app_id, UUID event_id, boolean include_deliveries) {
        var rows = jdbc.query(
                "SELECT id, event_type, payload::text, created_at, incoming_request_id FROM service_a.received_events WHERE application_id = ? AND id = ?",
                (rs, n) -> {
                    try {
                        return new EventDetails(event_id, app_id, rs.getString("event_type"),
                                mapper.readTree(rs.getString("payload")), rs.getTimestamp("created_at").toInstant(),
                                "PENDING", List.of(), rs.getObject("incoming_request_id", UUID.class));
                    } catch (JsonProcessingException ex) {
                        throw new IllegalStateException("Stored event is invalid", ex);
                    }
                }, app_id, event_id);
        if (rows.isEmpty())
            return Optional.empty();
        List<EventDeliveryView> deliveries = include_deliveries ? jdbc.query(
                "SELECT d.id, d.endpoint_id, d.status, d.attempt_count, d.http_status, d.last_error, d.updated_at, coalesce(d.endpoint_url, ep.url) endpoint_url, d.replay_of_id, child.id replay_id FROM service_a.delivery_projections d LEFT JOIN service_a.endpoints ep ON ep.id = d.endpoint_id LEFT JOIN service_a.delivery_projections child ON child.replay_of_id = d.id WHERE d.event_id = ? ORDER BY d.created_at, d.id",
                (rs, n) -> new EventDeliveryView(rs.getObject("id", UUID.class),
                        rs.getObject("endpoint_id", UUID.class),
                        rs.getString("status"), rs.getInt("attempt_count"), rs.getObject("http_status", Integer.class),
                        rs.getString("last_error"),
                        rs.getTimestamp("updated_at").toInstant(), rs.getString("endpoint_url"),
                        rs.getObject("replay_of_id", UUID.class), rs.getObject("replay_id", UUID.class)),
                event_id) : List.of();
        String status = jdbc.queryForObject("""
                SELECT CASE WHEN count(*)=0 THEN 'NO_SUBSCRIBERS'
                    WHEN count(*) FILTER(WHERE d.status='PENDING')>0 THEN 'PENDING'
                    WHEN count(*) FILTER(WHERE d.status='DEAD')>0 THEN 'FAILED'
                    ELSE 'SUCCEEDED' END
                FROM service_a.delivery_projections d WHERE d.event_id=?
                  AND NOT EXISTS(SELECT 1 FROM service_a.delivery_projections child WHERE child.replay_of_id=d.id)
                """, String.class, event_id);
        var event = rows.getFirst();
        return Optional.of(new EventDetails(event_id, app_id, event.eventType(), event.payload(), event.createdAt(), status,
                deliveries, event.incomingRequestId()));
    }

}

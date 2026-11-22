package com.eventrelay.servicea.monitoring;

import com.eventrelay.servicea.monitoring.dto.DeliveryAttempt;
import com.eventrelay.servicea.monitoring.dto.DeliverySummary;
import com.eventrelay.servicea.monitoring.dto.EventSummary;
import com.eventrelay.servicea.monitoring.dto.MonitoringSummary;
import com.eventrelay.servicea.monitoring.dto.ReplayReceipt;

import com.eventrelay.servicea.auth.access.ResourceAccess;
import com.eventrelay.servicea.common.dto.PageResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import java.sql.*;
import java.time.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.*;
import org.springframework.stereotype.Repository;
import org.springframework.web.server.ResponseStatusException;

@Repository
public class MonitoringStore {

    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;

    public MonitoringStore(JdbcTemplate jdbc, ObjectMapper mapper) {
        this.jdbc = jdbc;
        this.mapper = mapper;
    }

    private static final String EVENTS = """
            WITH activity AS (
              SELECT e.id, e.event_type, e.delivery_count, e.created_at,
                CASE WHEN e.delivery_count = 0 THEN 'NO_SUBSCRIBERS'
                  WHEN EXISTS (SELECT 1 FROM service_a.delivery_projections d WHERE d.event_id=e.id
                    AND d.status='PENDING' AND NOT EXISTS(SELECT 1 FROM service_a.delivery_projections child WHERE child.replay_of_id=d.id)) THEN 'PENDING'
                  WHEN EXISTS (SELECT 1 FROM service_a.delivery_projections d WHERE d.event_id=e.id
                    AND d.status='DEAD' AND NOT EXISTS(SELECT 1 FROM service_a.delivery_projections child WHERE child.replay_of_id=d.id)) THEN 'FAILED'
                  ELSE 'SUCCEEDED' END status
              FROM service_a.received_events e WHERE e.application_id=?
            )
            """;

    private static final String DELIVERIES = """
            SELECT d.*, e.event_type, coalesce(d.endpoint_url, ep.url) target_url, child.id replay_id,
                CASE WHEN d.status <> 'DEAD' THEN 'Only permanently failed deliveries can be replayed'
                     WHEN child.id IS NOT NULL THEN 'This delivery already has a replay'
                     WHEN NOT a.is_active THEN 'Activate the application before replaying'
                     WHEN ep.id IS NULL OR ep.deleted_at IS NOT NULL THEN 'Endpoint has been deleted'
                     WHEN NOT ep.is_active THEN 'Activate the endpoint before replaying'
                     WHEN ep.signing_secret_encrypted IS NULL THEN 'Configure an endpoint signing secret first'
                     WHEN t.id IS NULL OR t.deleted_at IS NOT NULL THEN 'Event type has been deleted'
                     WHEN NOT t.is_active THEN 'Activate the event type before replaying'
                     ELSE NULL END replay_reason
            FROM service_a.delivery_projections d
            JOIN service_a.received_events e ON e.id=d.event_id
            JOIN service_a.applications a ON a.id=d.application_id
            LEFT JOIN service_a.endpoints ep ON ep.id=d.endpoint_id
            LEFT JOIN service_a.event_types t ON t.id=e.event_type_id
            LEFT JOIN service_a.delivery_projections child ON child.replay_of_id=d.id
            WHERE d.application_id=?
            """;

    // -----------------------------------------------------------------------

    public PageResult<EventSummary> events(UUID app_id, String status, String q, int page, int size) {
        var args = new ArrayList<Object>();
        args.add(app_id);
        String where = " WHERE true";
        if (!status.isBlank()) {
            where += " AND status IN (" + String.join(",", Collections.nCopies(status.split(",").length, "?")) + ")";
            args.addAll(Arrays.stream(status.split(",")).map(String::trim).toList());
        }
        if (!q.isBlank()) {
            where += " AND (strpos(lower(event_type),lower(?))>0 OR strpos(id::text,lower(?))>0)";
            args.add(q);
            args.add(q);
        }
        long total = jdbc.queryForObject(EVENTS + "SELECT count(*) FROM activity" + where, Long.class, args.toArray());
        args.add(size);
        args.add((long) page * size);
        var items = jdbc.query(
                EVENTS + "SELECT * FROM activity" + where + " ORDER BY created_at DESC, id DESC LIMIT ? OFFSET ?",
                (rs, n) -> new EventSummary(rs.getObject("id", UUID.class), rs.getString("event_type"),
                        rs.getInt("delivery_count"), rs.getString("status"), instant(rs, "created_at")),
                args.toArray());
        return page(items, page, size, total);
    }

    // -----------------------------------------------------------------------

    public PageResult<DeliverySummary> deliveries(UUID app_id, String status, String q, UUID event_id,
            UUID endpoint_id,
            boolean include_replayed, int page, int size) {
        var args = new ArrayList<Object>();
        args.add(app_id);
        String where = "";
        if (!status.isBlank()) {
            where += " AND d.status IN (" + String.join(",", Collections.nCopies(status.split(",").length, "?")) + ")";
            args.addAll(Arrays.stream(status.split(",")).map(String::trim).toList());
        }
        if (!q.isBlank()) {
            where += " AND (strpos(lower(e.event_type),lower(?))>0 OR strpos(d.id::text,lower(?))>0 OR strpos(lower(coalesce(d.endpoint_url,ep.url)),lower(?))>0)";
            args.add(q);
            args.add(q);
            args.add(q);
        }
        if (event_id != null) {
            where += " AND d.event_id=?";
            args.add(event_id);
        }
        if (endpoint_id != null) {
            where += " AND d.endpoint_id=?";
            args.add(endpoint_id);
        }
        if (!include_replayed)
            where += " AND child.id IS NULL";
        long total = jdbc.queryForObject("SELECT count(*) FROM (" + DELIVERIES + where + ") scoped", Long.class,
                args.toArray());
        args.add(size);
        args.add((long) page * size);
        var items = jdbc.query(DELIVERIES + where + " ORDER BY d.created_at DESC,d.id DESC LIMIT ? OFFSET ?",
                this::delivery, args.toArray());
        return page(items, page, size, total);
    }

    // -----------------------------------------------------------------------

    // common
    public Optional<DeliverySummary> delivery(UUID app_id, UUID delivery_id) {
        return jdbc.query(DELIVERIES + " AND d.id=?", this::delivery, app_id, delivery_id).stream().findFirst();
    }

    public JsonNode requestBody(UUID delivery_id) {
        var rows = jdbc.query(
                "SELECT payload->>'body' body FROM service_a.outbox_messages WHERE aggregate_id=? AND routing_key='delivery.requested' ORDER BY created_at LIMIT 1",
                (rs, n) -> rs.getString(1), delivery_id);
        if (rows.isEmpty() || rows.getFirst() == null)
            return null;
        try {
            return mapper.readTree(rows.getFirst());
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Invalid stored request body", ex);
        }
    }

    public List<DeliveryAttempt> attempts(UUID delivery_id) {
        return jdbc.query(
                "SELECT * FROM service_a.delivery_attempt_projections WHERE delivery_id=? ORDER BY attempt_number",
                (rs, n) -> {
                    Instant started = instant(rs, "started_at"), completed = instant(rs, "completed_at");
                    return new DeliveryAttempt(rs.getInt("attempt_number"),
                            rs.getObject("http_status", Integer.class), rs.getString("error"), started, completed,
                            Math.max(0, Duration.between(started, completed).toMillis()), rs.getString("response_body"),
                            rs.getString("content_type"), rs.getBoolean("response_truncated"));
                }, delivery_id);
    }

    // -----------------------------------------------------------------------

    public Optional<ReplayReceipt> replay(UUID app_id, String idempotency_key, UUID source_delivery_id) {
        var rows = jdbc.query(
                "SELECT r.delivery_id,d.replay_of_id,r.created_at FROM service_a.replay_requests r JOIN service_a.delivery_projections d ON d.id=r.delivery_id AND d.application_id=r.application_id WHERE r.application_id=? AND r.idempotency_key=?",
                (rs, n) -> new ReplayReceipt(rs.getObject(1, UUID.class), null,
                        rs.getObject(2, UUID.class), rs.getTimestamp(3).toInstant()),
                app_id, idempotency_key);
        if (rows.isEmpty())
            return Optional.empty();
        var receipt = rows.getFirst();
        if (!source_delivery_id.equals(receipt.replayOfId()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Idempotency-Key was used for another replay");
        UUID event_id = jdbc.queryForObject("SELECT event_id FROM service_a.delivery_projections WHERE id=?",
                UUID.class,
                receipt.id());
        return Optional.of(new ReplayReceipt(receipt.id(), event_id, source_delivery_id, receipt.createdAt()));
    }

    public void lock(UUID app_id, UUID delivery_id) {
        if (jdbc.queryForList(
                "SELECT id FROM service_a.delivery_projections WHERE application_id=? AND id=? FOR UPDATE", app_id,
                delivery_id)
                .isEmpty()) {
            throw ResourceAccess.notFound("Delivery");
        }
    }

    public UUID eventTypeId(UUID event_id) {
        return jdbc.queryForObject("SELECT event_type_id FROM service_a.received_events WHERE id=?", UUID.class,
                event_id);
    }

    public String rawBody(UUID delivery_id) {
        var rows = jdbc.query(
                "SELECT payload->>'body' FROM service_a.outbox_messages WHERE aggregate_id=? AND routing_key='delivery.requested' ORDER BY created_at LIMIT 1",
                (rs, n) -> rs.getString(1), delivery_id);
        if (rows.isEmpty() || rows.getFirst() == null)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Original request body is unavailable");
        return rows.getFirst();
    }

    public void replayRequest(UUID app_id, String idempotency_key, UUID delivery_id, Instant at) {
        jdbc.update(
                "INSERT INTO service_a.replay_requests (application_id,idempotency_key,delivery_id,created_at) VALUES (?,?,?,?)",
                app_id, idempotency_key, delivery_id, Timestamp.from(at));
    }

    // -----------------------------------------------------------------------

    public MonitoringSummary summary(UUID app_id) {
        long events = jdbc.queryForObject("SELECT count(*) FROM service_a.received_events WHERE application_id=?",
                Long.class, app_id);
        return jdbc.queryForObject(
                """
                        SELECT count(*) total,
                            count(*) FILTER(WHERE d.status='SUCCEEDED') succeeded,
                            count(*) FILTER(WHERE d.status='PENDING') pending,
                            count(*) FILTER(WHERE d.status='DEAD' AND NOT EXISTS(SELECT 1 FROM service_a.delivery_projections child WHERE child.replay_of_id=d.id)) failed,
                            count(*) FILTER(WHERE d.replay_of_id IS NOT NULL) replays
                        FROM service_a.delivery_projections d WHERE d.application_id=?
                        """,
                (rs, n) -> new MonitoringSummary(events, rs.getLong("total"), rs.getLong("succeeded"),
                        rs.getLong("pending"), rs.getLong("failed"), rs.getLong("replays")),
                app_id);
    }

    // -----------------------------------------------------------------------

    private DeliverySummary delivery(ResultSet rs, int row) throws SQLException {
        String reason = rs.getString("replay_reason");
        return new DeliverySummary(rs.getObject("id", UUID.class), rs.getObject("event_id", UUID.class),
                rs.getObject("endpoint_id", UUID.class), rs.getString("event_type"), rs.getString("target_url"),
                rs.getString("status"), rs.getInt("attempt_count"), rs.getObject("http_status", Integer.class),
                rs.getString("last_error"), instant(rs, "created_at"), instant(rs, "updated_at"),
                rs.getObject("replay_of_id", UUID.class), rs.getObject("replay_id", UUID.class), reason == null,
                reason);
    }

    private static Instant instant(ResultSet rs, String name) throws SQLException {
        Timestamp value = rs.getTimestamp(name);
        return value == null ? null : value.toInstant();
    }

    private static <T> PageResult<T> page(List<T> items, int page, int size, long total) {
        return new PageResult<>(items, page, size, total, (int) ((total + size - 1) / size));
    }

}

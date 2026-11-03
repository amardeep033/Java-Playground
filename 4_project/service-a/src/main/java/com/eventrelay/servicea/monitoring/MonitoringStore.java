package com.eventrelay.servicea.monitoring;

import java.sql.*;
import java.time.*;
import java.util.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import org.springframework.jdbc.core.*;
import org.springframework.stereotype.Repository;
import com.eventrelay.servicea.dto.Responses.PageResult;

@Repository
public class MonitoringStore {
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;
    public MonitoringStore(JdbcTemplate jdbc, ObjectMapper mapper) { this.jdbc = jdbc; this.mapper = mapper; }
    private static final String EVENTS = """
        WITH activity AS (
          SELECT e.id, e.event_type, e.delivery_count, e.created_at,
            CASE WHEN e.delivery_count = 0 THEN 'NO_SUBSCRIBERS'
              WHEN EXISTS (SELECT 1 FROM service_a.delivery_projections d WHERE d.event_id=e.id AND d.deleted_at IS NULL
                AND d.status IN ('PENDING','RETRY_SCHEDULED') AND NOT EXISTS(SELECT 1 FROM service_a.delivery_projections child WHERE child.replay_of_id=d.id AND child.deleted_at IS NULL)) THEN 'PENDING'
              WHEN EXISTS (SELECT 1 FROM service_a.delivery_projections d WHERE d.event_id=e.id AND d.deleted_at IS NULL
                AND d.status='DEAD' AND NOT EXISTS(SELECT 1 FROM service_a.delivery_projections child WHERE child.replay_of_id=d.id AND child.deleted_at IS NULL)) THEN 'FAILED'
              ELSE 'SUCCEEDED' END status
          FROM service_a.received_events e WHERE e.application_id=? AND e.deleted_at IS NULL
        )
        """;
    private static final String DELIVERIES = """
        SELECT d.*, e.event_type, coalesce(d.endpoint_url, ep.url) target_url, child.id replay_id,
            CASE WHEN d.status <> 'DEAD' THEN 'Only permanently failed deliveries can be replayed'
                 WHEN child.id IS NOT NULL THEN 'This delivery already has a replay'
                 WHEN ep.id IS NULL OR ep.deleted_at IS NOT NULL THEN 'Endpoint has been deleted'
                 WHEN NOT ep.enabled THEN 'Enable the endpoint before replaying'
                 WHEN ep.signing_secret_encrypted IS NULL THEN 'Configure an endpoint signing secret first'
                 WHEN t.id IS NULL OR t.deleted_at IS NOT NULL THEN 'Event type has been deleted'
                 ELSE NULL END replay_reason
        FROM service_a.delivery_projections d
        JOIN service_a.received_events e ON e.id=d.event_id AND e.deleted_at IS NULL
        LEFT JOIN service_a.endpoints ep ON ep.id=d.endpoint_id
        LEFT JOIN service_a.event_types t ON t.id=e.event_type_id
        LEFT JOIN service_a.delivery_projections child ON child.replay_of_id=d.id AND child.deleted_at IS NULL
        WHERE d.application_id=? AND d.deleted_at IS NULL
        """;

    public PageResult<MonitoringApi.EventSummary> events(UUID appId, String status, String q, int page, int size) {
        var args = new ArrayList<Object>(); args.add(appId);
        String where = " WHERE true";
        if (!status.isBlank()) { where += " AND status=?"; args.add(status); }
        if (!q.isBlank()) { where += " AND (strpos(lower(event_type),lower(?))>0 OR strpos(id::text,lower(?))>0)"; args.add(q); args.add(q); }
        long total = jdbc.queryForObject(EVENTS + "SELECT count(*) FROM activity" + where, Long.class, args.toArray());
        args.add(size); args.add((long)page * size);
        var items = jdbc.query(EVENTS + "SELECT * FROM activity" + where + " ORDER BY created_at DESC, id DESC LIMIT ? OFFSET ?",
                (rs,n) -> new MonitoringApi.EventSummary(rs.getObject("id",UUID.class),rs.getString("event_type"),rs.getInt("delivery_count"),rs.getString("status"),instant(rs,"created_at")), args.toArray());
        return page(items,page,size,total);
    }
    public PageResult<MonitoringApi.DeliverySummary> deliveries(UUID appId, String status, String q, UUID eventId, UUID endpointId,
                                                               boolean includeReplayed, int page, int size) {
        var args = new ArrayList<Object>(); args.add(appId);
        String where = "";
        if (!status.isBlank()) { where += " AND d.status=?"; args.add(status); }
        if (!q.isBlank()) { where += " AND (strpos(lower(e.event_type),lower(?))>0 OR strpos(d.id::text,lower(?))>0 OR strpos(lower(coalesce(d.endpoint_url,ep.url)),lower(?))>0)"; args.add(q);args.add(q);args.add(q); }
        if (eventId != null) { where += " AND d.event_id=?"; args.add(eventId); }
        if (endpointId != null) { where += " AND d.endpoint_id=?"; args.add(endpointId); }
        if (!includeReplayed) where += " AND child.id IS NULL";
        long total = jdbc.queryForObject("SELECT count(*) FROM ("+DELIVERIES+where+") scoped", Long.class, args.toArray());
        args.add(size);args.add((long)page * size);
        var items=jdbc.query(DELIVERIES+where+" ORDER BY d.created_at DESC,d.id DESC LIMIT ? OFFSET ?", this::delivery, args.toArray());
        return page(items,page,size,total);
    }
    public Optional<MonitoringApi.DeliverySummary> delivery(UUID appId, UUID id) {
        return jdbc.query(DELIVERIES+" AND d.id=?",this::delivery,appId,id).stream().findFirst();
    }
    public void lock(UUID appId, UUID id) {
        if (jdbc.queryForList("SELECT id FROM service_a.delivery_projections WHERE application_id=? AND id=? AND deleted_at IS NULL FOR UPDATE",appId,id).isEmpty()) {
            throw com.eventrelay.servicea.service.ResourceAccess.notFound("Delivery");
        }
    }
    public JsonNode requestBody(UUID id) {
        var rows=jdbc.query("SELECT payload->>'body' body FROM service_a.outbox_messages WHERE aggregate_id=? AND routing_key='delivery.requested' AND deleted_at IS NULL ORDER BY created_at LIMIT 1",(rs,n)->rs.getString(1),id);
        if (rows.isEmpty() || rows.getFirst()==null) return null;
        try { return mapper.readTree(rows.getFirst()); } catch (JsonProcessingException ex) { throw new IllegalStateException("Invalid stored request body",ex); }
    }
    public String rawBody(UUID id) {
        var rows=jdbc.query("SELECT payload->>'body' FROM service_a.outbox_messages WHERE aggregate_id=? AND routing_key='delivery.requested' AND deleted_at IS NULL ORDER BY created_at LIMIT 1",(rs,n)->rs.getString(1),id);
        if (rows.isEmpty() || rows.getFirst()==null) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT,"Original request body is unavailable");
        return rows.getFirst();
    }
    public UUID eventTypeId(UUID eventId) { return jdbc.queryForObject("SELECT event_type_id FROM service_a.received_events WHERE id=? AND deleted_at IS NULL",UUID.class,eventId); }
    public List<MonitoringApi.Attempt> attempts(UUID id) {
        return jdbc.query("SELECT * FROM service_a.delivery_attempt_projections WHERE delivery_id=? ORDER BY attempt_number",(rs,n)-> {
            Instant started=instant(rs,"started_at"),completed=instant(rs,"completed_at");
            return new MonitoringApi.Attempt(rs.getInt("attempt_number"),rs.getObject("http_status",Integer.class),rs.getString("error"),started,completed,
                    Math.max(0,Duration.between(started,completed).toMillis()),rs.getString("response_body"),rs.getString("content_type"),rs.getBoolean("response_truncated"));
        },id);
    }
    public Optional<MonitoringApi.ReplayReceipt> replay(UUID appId,String key,UUID source) {
        var rows=jdbc.query("SELECT delivery_id,source_delivery_id,created_at FROM service_a.replay_requests WHERE application_id=? AND idempotency_key=?",(rs,n)->new MonitoringApi.ReplayReceipt(rs.getObject(1,UUID.class),null,rs.getObject(2,UUID.class),rs.getTimestamp(3).toInstant()),appId,key);
        if(rows.isEmpty()) return Optional.empty();
        var receipt=rows.getFirst();
        if(!source.equals(receipt.replayOfId())) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT,"Idempotency-Key was used for another replay");
        UUID eventId=jdbc.queryForObject("SELECT event_id FROM service_a.delivery_projections WHERE id=?",UUID.class,receipt.id());
        return Optional.of(new MonitoringApi.ReplayReceipt(receipt.id(),eventId,source,receipt.createdAt()));
    }
    public void replayRequest(UUID appId,String key,UUID source,UUID id,Instant at) {
        jdbc.update("INSERT INTO service_a.replay_requests (application_id,idempotency_key,source_delivery_id,delivery_id,created_at) VALUES (?,?,?,?,?)",appId,key,source,id,Timestamp.from(at));
    }
    public MonitoringApi.Summary summary(UUID appId) {
        long events=jdbc.queryForObject("SELECT count(*) FROM service_a.received_events WHERE application_id=? AND deleted_at IS NULL",Long.class,appId);
        return jdbc.queryForObject("""
            SELECT count(*) total,
                count(*) FILTER(WHERE d.status='SUCCEEDED') succeeded,
                count(*) FILTER(WHERE d.status IN ('PENDING','RETRY_SCHEDULED')) pending,
                count(*) FILTER(WHERE d.status='DEAD' AND NOT EXISTS(SELECT 1 FROM service_a.delivery_projections child WHERE child.replay_of_id=d.id AND child.deleted_at IS NULL)) failed,
                count(*) FILTER(WHERE d.replay_of_id IS NOT NULL) replays
            FROM service_a.delivery_projections d WHERE d.application_id=? AND d.deleted_at IS NULL
            """,(rs,n)->new MonitoringApi.Summary(events,rs.getLong("total"),rs.getLong("succeeded"),rs.getLong("pending"),rs.getLong("failed"),rs.getLong("replays")),appId);
    }
    private MonitoringApi.DeliverySummary delivery(ResultSet rs,int row) throws SQLException {
        String reason=rs.getString("replay_reason");
        return new MonitoringApi.DeliverySummary(rs.getObject("id",UUID.class),rs.getObject("event_id",UUID.class),rs.getObject("endpoint_id",UUID.class),rs.getString("event_type"),rs.getString("target_url"),rs.getString("status"),rs.getInt("attempt_count"),rs.getObject("http_status",Integer.class),rs.getString("last_error"),instant(rs,"next_attempt_at"),instant(rs,"created_at"),instant(rs,"updated_at"),rs.getObject("replay_of_id",UUID.class),rs.getObject("replay_id",UUID.class),reason==null,reason);
    }
    private static Instant instant(ResultSet rs,String name) throws SQLException { Timestamp value=rs.getTimestamp(name);return value==null?null:value.toInstant(); }
    private static <T> PageResult<T> page(List<T> items,int page,int size,long total) { return new PageResult<>(items,page,size,total,(int)((total+size-1)/size)); }
}

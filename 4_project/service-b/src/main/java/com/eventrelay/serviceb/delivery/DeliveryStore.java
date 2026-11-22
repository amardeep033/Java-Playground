package com.eventrelay.serviceb.delivery;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import com.eventrelay.serviceb.messaging.DeliveryCommand;
import com.eventrelay.serviceb.messaging.DeliveryStatusMessage;

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
        var records = jdbc.query("SELECT id, command::text, status, attempt_count, status_sequence FROM service_b.deliveries WHERE id = ? FOR UPDATE",
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
        boolean terminal=Set.of("SUCCEEDED","DEAD").contains(status);
        Timestamp finalized=null;
        if(terminal) {
            // Serialize only the short final DB write, never the HTTP attempt. This makes feed
            // timestamps commit-ordered: an invisible transaction cannot appear behind A's cursor.
            jdbc.execute("SELECT pg_advisory_xact_lock(7319472051)");
            finalized=jdbc.queryForObject("SELECT greatest(clock_timestamp(),coalesce((SELECT finalized_at+interval '1 microsecond' FROM service_b.deliveries WHERE is_final ORDER BY finalized_at DESC,id DESC LIMIT 1),'epoch'::timestamptz))",Timestamp.class);
        }
        jdbc.update("INSERT INTO service_b.delivery_attempts (id, delivery_id, attempt_number, http_status, error, started_at, completed_at, response_body, content_type, response_truncated) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                UUID.randomUUID(),delivery.id(),attempt,result.httpStatus(),result.error(),Timestamp.from(result.startedAt()),Timestamp.from(result.completedAt()),result.responseBody(),result.contentType(),result.responseTruncated());
        jdbc.update("UPDATE service_b.deliveries SET status=?,attempt_count=?,status_sequence=?,http_status=?,last_error=?,next_attempt_at=?,updated_at=?,is_final=?,finalized_at=?,completion_message_id=? WHERE id=?",
                status,attempt,delivery.sequence()+1,result.httpStatus(),result.error(),nextAttempt==null?null:Timestamp.from(nextAttempt),terminal?finalized:Timestamp.from(result.completedAt()),terminal,finalized,terminal?UUID.randomUUID():null,delivery.id());
    }
    public DeliveryStatusMessage completion(UUID id) {
        return jdbc.query("SELECT id,event_id,application_id,endpoint_id,status,attempt_count,status_sequence,http_status,last_error,completion_message_id,finalized_at FROM service_b.deliveries WHERE id=? AND is_final",
            (rs,n)->new DeliveryStatusMessage(2,rs.getObject("completion_message_id",UUID.class),id,rs.getObject("event_id",UUID.class),rs.getObject("application_id",UUID.class),rs.getObject("endpoint_id",UUID.class),rs.getString("status"),rs.getInt("attempt_count"),rs.getLong("status_sequence"),rs.getObject("http_status",Integer.class),rs.getString("last_error"),null,rs.getTimestamp("finalized_at").toInstant(),attempts(id)),id).stream().findFirst().orElseThrow(()->new IllegalArgumentException("No final delivery"));
    }
    private List<DeliveryStatusMessage.Attempt> attempts(UUID id) {
        return jdbc.query("SELECT attempt_number,http_status,error,started_at,completed_at,response_body,content_type,response_truncated FROM service_b.delivery_attempts WHERE delivery_id=? ORDER BY attempt_number",
            (rs,n)->new DeliveryStatusMessage.Attempt(rs.getInt("attempt_number"),rs.getObject("http_status",Integer.class),rs.getString("error"),rs.getTimestamp("started_at").toInstant(),rs.getTimestamp("completed_at").toInstant(),rs.getString("response_body"),rs.getString("content_type"),rs.getBoolean("response_truncated")),id);
    }
    /** Indexed keyset scan of final rows only, including equal-timestamp ties. No OFFSET/full-table scan. */
    public List<DeliveryStatusMessage> completions(Instant after,UUID afterId,int limit) {
        var ids=jdbc.queryForList("SELECT id FROM service_b.deliveries WHERE is_final AND (finalized_at,id)>(?,?) ORDER BY finalized_at,id LIMIT ?",UUID.class,Timestamp.from(after),afterId,limit);
        return ids.stream().map(this::completion).toList();
    }
    public Optional<Map<String, Object>> details(UUID id) {
        var rows = jdbc.queryForList("SELECT id, event_id, application_id, endpoint_id, status, attempt_count, http_status, last_error, next_attempt_at, created_at, updated_at, replay_of_id FROM service_b.deliveries WHERE id = ?", id);
        if (rows.isEmpty()) return Optional.empty();
        var result = rows.getFirst();
        result.put("attempts", jdbc.queryForList("SELECT attempt_number, http_status, error, started_at, completed_at, response_body, content_type, response_truncated FROM service_b.delivery_attempts WHERE delivery_id = ? ORDER BY attempt_number", id));
        return Optional.of(result);
    }
}

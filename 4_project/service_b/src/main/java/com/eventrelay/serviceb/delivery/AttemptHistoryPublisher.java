package com.eventrelay.serviceb.delivery;

import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.time.Instant;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import com.eventrelay.serviceb.config.RabbitMqConfig;
import com.eventrelay.serviceb.messaging.*;

/** Exports pre-upgrade attempts using their recorded metadata, without resending webhooks. */
@Component
public class AttemptHistoryPublisher {
    private final JdbcTemplate jdbc;
    private final Outbox outbox;
    public AttemptHistoryPublisher(JdbcTemplate jdbc,Outbox outbox) { this.jdbc=jdbc;this.outbox=outbox; }
    @Scheduled(fixedDelayString="${app.history.poll-delay-ms:1000}")
    @Transactional
    public void synchronize() {
        var rows=jdbc.query("""
            SELECT a.*,d.event_id,d.application_id,d.endpoint_id,d.status,d.attempt_count,d.status_sequence,
                   d.http_status current_http,d.last_error,d.next_attempt_at,d.updated_at
            FROM service_b.delivery_attempts a JOIN service_b.deliveries d ON d.id=a.delivery_id
            WHERE a.monitoring_exported_at IS NULL AND d.deleted_at IS NULL
            ORDER BY a.started_at,a.id LIMIT 50 FOR UPDATE OF a SKIP LOCKED
            """,(rs,n)-> {
                UUID delivery=rs.getObject("delivery_id",UUID.class);
                int number=rs.getInt("attempt_number");
                UUID message=UUID.nameUUIDFromBytes(("attempt-history-v1:"+delivery+":"+number).getBytes(StandardCharsets.UTF_8));
                var attempt=new DeliveryStatusMessage.Attempt(number,rs.getObject("http_status",Integer.class),rs.getString("error"),
                        instant(rs,"started_at"),instant(rs,"completed_at"),rs.getString("response_body"),rs.getString("content_type"),rs.getBoolean("response_truncated"));
                return new Historical(rs.getObject("id",UUID.class),new DeliveryStatusMessage(1,message,delivery,rs.getObject("event_id",UUID.class),
                        rs.getObject("application_id",UUID.class),rs.getObject("endpoint_id",UUID.class),rs.getString("status"),rs.getInt("attempt_count"),
                        rs.getLong("status_sequence"),rs.getObject("current_http",Integer.class),rs.getString("last_error"),instant(rs,"next_attempt_at"),instant(rs,"updated_at"),attempt));
            });
        for(var row:rows) {
            var update=row.message();
            update.validate();
            outbox.enqueue(update.messageId(),update.deliveryId(),RabbitMqConfig.STATUS_EXCHANGE,"delivery.updated",update);
            jdbc.update("UPDATE service_b.delivery_attempts SET monitoring_exported_at=now() WHERE id=?",row.attemptId());
        }
    }
    private static Instant instant(ResultSet rs,String field)throws SQLException { Timestamp timestamp=rs.getTimestamp(field);return timestamp==null?null:timestamp.toInstant(); }
    private record Historical(UUID attemptId,DeliveryStatusMessage message) {}
}

package com.eventrelay.servicea.delivery.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

//disable complete class - segregate outbox publisher into separate class for no loops
// @ConditionalOnProperty(
//     name = "app.outbox.enabled",
//     havingValue = "true",
//     matchIfMissing = true
// )
@Component
public class Outbox {

    private static final String TABLE = "service_a.outbox_messages";

    @Value("${app.outbox.enabled:true}")
    private boolean enabled;

    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;
    private final RabbitTemplate rabbit;
    private final int batch_size;

    // From a class-design perspective, splitting into OutboxWriter and OutboxPublisher is slightly cleaner because they have distinct responsibilities.
    // Having both JdbcTemplate and RabbitTemplate injected does not meaningfully increase the load. Spring normally creates these as singleton beans. 
    public Outbox(JdbcTemplate jdbc, ObjectMapper mapper, RabbitTemplate rabbit, @Value("${app.outbox.batch-size:20}") int batch_size) {
        if (batch_size < 1) {
            throw new IllegalArgumentException("Outbox batch size must be at least 1");
        }
        this.batch_size = batch_size;
        this.jdbc = jdbc;
        this.mapper = mapper;
        this.rabbit = rabbit;
    }

    // -----------------------------------------------------------------------

    // DB write4: insert into outbox
    public void enqueue(UUID message_id, UUID aggregate_id, String exchange, String routing_key, Object payload) {
        try {
            jdbc.update("INSERT INTO " + TABLE + " (id, aggregate_id, exchange_name, routing_key, payload) VALUES (?, ?, ?, ?, ?::jsonb) ON CONFLICT (id) DO NOTHING",
                    message_id, aggregate_id, exchange, routing_key, mapper.writeValueAsString(payload));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Cannot encode outgoing message", ex);
        }
    }

    // -----------------------------------------------------------------------

    // Spring first looks for a unique TaskScheduler. If multiple exist, it looks for the bean named taskScheduler.
    @Scheduled(fixedDelayString = "${app.outbox.poll-delay-ms:250}"
            // , scheduler = "taskScheduler" //RabbitMqConfig.taskScheduler()
    )
    public void publishPending() {

        if (!enabled)
            return;
        
        // only sends upto batch size -- allows multiple instance of service a -- locks one at a time (limit 1)
        for (int message_count = 0; message_count < batch_size; message_count++) {
            UUID claim = UUID.randomUUID();

            // → Claim pending row
            var rows = jdbc.query(
                            """
                            WITH candidate AS (
                                SELECT id FROM service_a.outbox_messages
                                WHERE published_at IS NULL AND available_at <= now()
                                  AND (locked_until IS NULL OR locked_until < now())
                                ORDER BY created_at, id FOR UPDATE SKIP LOCKED LIMIT 1
                            )
                            UPDATE service_a.outbox_messages o SET claim_token = ?, locked_until = now() + interval '30 seconds',
                                attempt_count = o.attempt_count + 1
                            FROM candidate c WHERE o.id = c.id
                            RETURNING o.id, o.exchange_name, o.routing_key, o.payload::text
                            """,
                    (rs, row) -> new Pending(rs.getObject(1, UUID.class), rs.getString(2), rs.getString(3), rs.getString(4)),claim);

            if (rows.isEmpty())
                return;

            var pending = rows.getFirst();
            try {

                var properties = new MessageProperties();
                properties.setMessageId(pending.id().toString());
                properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
                properties.setDeliveryMode(MessageDeliveryMode.PERSISTENT);

                var correlation = new CorrelationData(pending.id().toString());
                // → Publish persistent message
                rabbit.send(pending.exchange(), pending.routingKey(), new Message(pending.json().getBytes(StandardCharsets.UTF_8), properties), correlation);

                // → Wait for RabbitMQ confirmation -- our timeout at 5 sec -- and can retry, atleast once
                var confirm = correlation.getFuture().get(5, TimeUnit.SECONDS);
                if (!confirm.isAck() || correlation.getReturned() != null)
                    throw new IllegalStateException("Broker did not route and confirm message");

                // → Mark row published
                jdbc.update("UPDATE " + TABLE + " SET published_at = now(), locked_until = NULL, claim_token = NULL, last_error = NULL WHERE id = ? AND claim_token = ?", pending.id(), claim);

            } catch (Exception ex) {

                if (ex instanceof InterruptedException)
                    Thread.currentThread().interrupt();

                // → Free row for retry -- Without available_at, the next scheduler run—or another instance—could immediately reclaim the failing row. It would retry aggressively during a broker outage.
                jdbc.update("UPDATE " + TABLE + " SET locked_until = NULL, claim_token = NULL, available_at = now() + interval '2 seconds', last_error = ? WHERE id = ? AND claim_token = ?",
                        ex.getClass().getSimpleName(), pending.id(), claim);
                return;
            }
        }
    }

    private record Pending(UUID id, String exchange, String routingKey, String json) {
    }
}

// Acquire:
//  Conditions: not published + available + not locked
//  Set: claim token + lock + attempt count

// Success:
//   Set published_at + Clear locked_until + claim_token

// Failure/timeout:
//   Keep published_at empty
//   Clear locked_until and claim_token
//   Set available_at = now + 2 seconds
//   End this scheduled invocation

// Worker crash: (atleast one due to retry)
//   Cleanup doesn't execute + After the 30-second lease expires, another worker can reclaim

// The 5-second timer starts only after send() returns. If processing exceeds 30 seconds, another worker may reclaim and publish the same message. The claim token protects subsequent database updates, but cannot prevent that duplicate publish.
// For this design, safety comes from recoverable claims plus idempotent processing in B, rather than assuming 30 seconds can never be exceeded.

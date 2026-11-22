package com.eventrelay.servicea.delivery.reconciliation;

import com.eventrelay.servicea.delivery.client.DeliveryCompletionClient;
import com.eventrelay.servicea.delivery.messaging.StatusUpdateService;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReconciliationService {
    private final JdbcTemplate db;
    private final DeliveryCompletionClient client;
    private final StatusUpdateService updates;
    private final int batchSize;

    private record Cursor(Instant at, UUID id) {
    }

    public ReconciliationService(JdbcTemplate db, DeliveryCompletionClient client, StatusUpdateService updates,
            @Value("${app.reconciliation.batch-size:20}") int batch_size) {
        if (batch_size < 1 || batch_size > 20)
            throw new IllegalArgumentException("Invalid reconciliation batch size");
        this.db = db;
        this.client = client;
        this.updates = updates;
        this.batchSize = batch_size; // n delivery items
    }

    @Transactional
    public boolean reconcilePage() {

        // reconciliation_cursors -- name, finalized_at, delivery_id, updated_at
        // acquire lock
        var rows = db.query(
                "SELECT finalized_at,delivery_id FROM service_a.reconciliation_cursors WHERE name='service-b-completions' FOR UPDATE SKIP LOCKED",
                (rs, n) -> new Cursor(rs.getTimestamp(1).toInstant(), rs.getObject(2, UUID.class)));
        
        // at a given time only one instance of service a can reconcile
        if (rows.isEmpty())
            return false; // Another A instance already owns this pass.

        var cursor = rows.getFirst();
        // fetch
        var messages = client.fetch(cursor.at(), cursor.id(), batchSize);

        for (var message : messages) {
            message.validate();

            // defensive check via contract: verifies that each completion comes strictly after the current cursor, cursor updated each time in loop
            // Same timestamp --- require incoming delivery_id to be greater than the cursor’s (service B -- ORDER BY finalized_at, id)
            int comparison = message.updatedAt().compareTo(cursor.at());
            if (comparison < 0 || comparison == 0 && message.deliveryId().toString().compareTo(cursor.id().toString()) <= 0)
                throw new IllegalArgumentException("Completion feed is not strictly ordered");

            // apply
            updates.apply(message);

            // After each message is successfully handled, the local cursor moves to that message
            // That final cursor is written to reconciliation_cursors once per page, in the same transaction as the updates.
            cursor = new Cursor(message.updatedAt(), message.deliveryId());
        }

        //release lock
        if (!messages.isEmpty())
            db.update(
                    "UPDATE service_a.reconciliation_cursors SET finalized_at=?,delivery_id=?,updated_at=now() WHERE name='service-b-completions'",
                    Timestamp.from(cursor.at()), cursor.id());

        
        return messages.size() == batchSize;
    }
}

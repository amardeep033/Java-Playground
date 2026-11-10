package com.eventrelay.servicea.reconciliation;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.eventrelay.servicea.messaging.StatusUpdateService;

@Service
public class ReconciliationService {
    private final JdbcTemplate db;
    private final CompletionClient client;
    private final StatusUpdateService updates;
    private final int batchSize;
    private record Cursor(Instant at,UUID id) {}
    public ReconciliationService(JdbcTemplate db,CompletionClient client,StatusUpdateService updates,
            @Value("${app.reconciliation.batch-size:20}") int batchSize) {
        if(batchSize<1||batchSize>20) throw new IllegalArgumentException("Invalid reconciliation batch size");
        this.db=db;this.client=client;this.updates=updates;this.batchSize=batchSize;
    }
    /** All imported results and this page's checkpoint commit together. Failures retry the same page. */
    @Transactional public boolean reconcilePage() {
        var rows=db.query("SELECT finalized_at,delivery_id FROM service_a.reconciliation_cursors WHERE name='service-b-completions' FOR UPDATE SKIP LOCKED",
                (rs,n)->new Cursor(rs.getTimestamp(1).toInstant(),rs.getObject(2,UUID.class)));
        if(rows.isEmpty()) return false; // Another A instance already owns this pass.
        var cursor=rows.getFirst();
        var messages=client.fetch(cursor.at(),cursor.id(),batchSize);
        for(var message:messages) {
            message.validate();
            // UUID comparison follows PostgreSQL unsigned byte ordering, including timestamp ties.
            int comparison=message.updatedAt().compareTo(cursor.at());
            if(comparison<0||comparison==0&&message.deliveryId().toString().compareTo(cursor.id().toString())<=0)
                throw new IllegalArgumentException("Completion feed is not strictly ordered");
            updates.apply(message);
            cursor=new Cursor(message.updatedAt(),message.deliveryId());
        }
        if(!messages.isEmpty()) db.update("UPDATE service_a.reconciliation_cursors SET finalized_at=?,delivery_id=?,updated_at=now() WHERE name='service-b-completions'",
                Timestamp.from(cursor.at()),cursor.id());
        return messages.size()==batchSize;
    }
}

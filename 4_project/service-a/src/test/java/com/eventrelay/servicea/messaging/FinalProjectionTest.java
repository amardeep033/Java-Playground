package com.eventrelay.servicea.messaging;

import java.sql.*;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.*;
import com.eventrelay.servicea.events.EventStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

class FinalProjectionTest {
    JdbcTemplate db=mock(JdbcTemplate.class);
    EventStore store=new EventStore(db,new ObjectMapper());
    UUID delivery=UUID.randomUUID(),event=UUID.randomUUID(),app=UUID.randomUUID(),endpoint=UUID.randomUUID();
    Instant at=Instant.now();
    DeliveryStatusMessage message(String status,long sequence) {
        int code=status.equals("SUCCEEDED")?200:503;
        return new DeliveryStatusMessage(2,UUID.randomUUID(),delivery,event,app,endpoint,status,1,sequence,code,null,null,at,
            List.of(new DeliveryStatusMessage.Attempt(1,code,null,at,at,null,null,false)));
    }
    void current(String status,long sequence) throws Exception {
        when(db.query(anyString(),any(RowMapper.class),eq(delivery),eq(event),eq(app),eq(endpoint))).thenAnswer(invocation->{
            var rs=mock(ResultSet.class);when(rs.getLong(1)).thenReturn(sequence);when(rs.getString(2)).thenReturn(status);
            RowMapper<?> mapper=invocation.getArgument(1);return List.of(mapper.mapRow(rs,0));
        });
        when(db.update(startsWith("INSERT INTO service_a.status_inbox"),any(UUID.class),eq(delivery))).thenReturn(1);
    }
    @Test void conflictingHigherSequenceCannotOverwriteTerminalOutcome() throws Exception {
        current("SUCCEEDED",1);store.applyStatus(message("DEAD",100));
        verify(db,never()).update(startsWith("UPDATE service_a.delivery_projections"),any(Object[].class));
        verify(db,never()).update(startsWith("INSERT INTO service_a.delivery_attempt_projections"),any(Object[].class));
    }
    @Test void finalSnapshotMovesPendingForward() throws Exception {
        current("PENDING",0);store.applyStatus(message("SUCCEEDED",1));
        verify(db).update(startsWith("UPDATE service_a.delivery_projections"),eq("SUCCEEDED"),eq(1),eq(1L),eq(200),isNull(),any(),eq(delivery));
    }
    @Test void retryMessagesAreRejected() {
        assertThrows(IllegalArgumentException.class,()->store.applyStatus(message("RETRY_SCHEDULED",1)));
        verifyNoInteractions(db);
    }
    @Test void malformedAttemptHistoryIsRejected() {
        var m=message("SUCCEEDED",1);
        var invalid=new DeliveryStatusMessage(2,m.messageId(),delivery,event,app,endpoint,"SUCCEEDED",2,2,200,null,null,at,m.attempts());
        assertThrows(IllegalArgumentException.class,invalid::validate);
    }
}

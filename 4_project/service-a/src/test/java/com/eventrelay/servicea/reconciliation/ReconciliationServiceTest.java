package com.eventrelay.servicea.reconciliation;

import java.sql.*;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.*;
import com.eventrelay.servicea.messaging.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

class ReconciliationServiceTest {
    JdbcTemplate db;CompletionClient client;StatusUpdateService updates;ReconciliationService service;
    final Instant at=Instant.parse("2026-01-01T00:00:00Z");
    final UUID id=UUID.fromString("00000000-0000-0000-0000-000000000001");
    @BeforeEach void setup() throws Exception {
        db=mock(JdbcTemplate.class);client=mock(CompletionClient.class);updates=mock(StatusUpdateService.class);
        service=new ReconciliationService(db,client,updates,2);
        when(db.query(anyString(),any(RowMapper.class))).thenAnswer(invocation->{
            ResultSet rs=mock(ResultSet.class);when(rs.getTimestamp(1)).thenReturn(Timestamp.from(at));when(rs.getObject(2,UUID.class)).thenReturn(id);
            RowMapper<?> mapper=invocation.getArgument(1);return List.of(mapper.mapRow(rs,0));
        });
    }
    DeliveryStatusMessage message(UUID delivery,Instant completed) {
        var attempt=new DeliveryStatusMessage.Attempt(1,200,null,at,at,null,null,false);
        return new DeliveryStatusMessage(2,UUID.randomUUID(),delivery,UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),"SUCCEEDED",1,1,200,null,null,completed,List.of(attempt));
    }
    @Test void equalTimestampsUseDeliveryIdAndCheckpointLastRecord() {
        var one=message(UUID.fromString("00000000-0000-0000-0000-000000000002"),at);
        var two=message(UUID.fromString("80000000-0000-0000-0000-000000000000"),at);
        when(client.fetch(at,id,2)).thenReturn(List.of(one,two));
        assertTrue(service.reconcilePage());verify(updates).apply(one);verify(updates).apply(two);
        verify(db).update(startsWith("UPDATE service_a.reconciliation_cursors"),eq(Timestamp.from(at)),eq(two.deliveryId()));
    }
    @Test void unavailableServiceNeverAdvancesCheckpoint() {
        when(client.fetch(at,id,2)).thenThrow(new IllegalStateException());
        assertThrows(IllegalStateException.class,service::reconcilePage);
        verify(db,never()).update(anyString(),any(Timestamp.class),any(UUID.class));verifyNoInteractions(updates);
    }
    @Test void applyingFailureNeverAdvancesCheckpoint() {
        var message=message(UUID.randomUUID(),at.plusSeconds(1));when(client.fetch(at,id,2)).thenReturn(List.of(message));
        doThrow(new IllegalArgumentException()).when(updates).apply(message);
        assertThrows(IllegalArgumentException.class,service::reconcilePage);verify(db,never()).update(anyString(),any(Timestamp.class),any(UUID.class));
    }
    @Test void invalidOrBackwardFeedNeverAdvancesCheckpoint() {
        when(client.fetch(at,id,2)).thenReturn(List.of(message(id,at.minusSeconds(1))));
        assertThrows(IllegalArgumentException.class,service::reconcilePage);verifyNoInteractions(updates);verify(db,never()).update(anyString(),any(Timestamp.class),any(UUID.class));
    }
    @Test void anotherWorkerOwningCursorSkipsRestCall() {
        when(db.query(anyString(),any(RowMapper.class))).thenReturn(List.of());
        assertFalse(service.reconcilePage());verifyNoInteractions(client,updates);
    }
}

package com.eventrelay.servicea.monitoring;

import java.time.Instant;
import java.util.*;
import com.eventrelay.servicea.entity.*;
import com.eventrelay.servicea.events.EventStore;
import com.eventrelay.servicea.messaging.*;
import com.eventrelay.servicea.repository.*;
import com.eventrelay.servicea.service.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MonitoringServiceTest {
    @Mock ApiKeyService keys; @Mock ResourceAccess access; @Mock MonitoringStore store;
    @Mock EventStore events; @Mock EndpointRepository endpoints; @Mock EventTypeRepository types; @Mock Outbox outbox;
    UUID app=UUID.randomUUID(),source=UUID.randomUUID(),event=UUID.randomUUID(),endpointId=UUID.randomUUID(),typeId=UUID.randomUUID();
    MonitoringService service;
    @BeforeEach void setup(){ service=new MonitoringService(keys,access,store,events,endpoints,types,outbox); }
    @Test void replayRetryReturnsTheOriginalReceiptWithoutPublishingAgain(){
        when(keys.authenticate("key")).thenReturn(app);
        var receipt=new MonitoringApi.ReplayReceipt(UUID.randomUUID(),event,source,Instant.now());
        when(store.replay(app,"request",source)).thenReturn(Optional.of(receipt));
        assertEquals(receipt,service.replay(app,source,"key","request"));
        verifyNoInteractions(events,endpoints,types,outbox);
    }
    @Test void successfulOrPendingDeliveryCannotBeReplayed(){
        when(keys.authenticate("key")).thenReturn(app);
        when(store.delivery(app,source)).thenReturn(Optional.of(delivery(false,"Only permanently failed deliveries can be replayed")));
        var error=assertThrows(ResponseStatusException.class,()->service.replay(app,source,"key","request"));
        assertEquals(409,error.getStatusCode().value()); verifyNoInteractions(outbox,events);
    }
    @Test void replayUsesNewDeliveryIdentityAndCurrentEndpointSnapshot(){
        when(keys.authenticate("key")).thenReturn(app);
        when(store.delivery(app,source)).thenReturn(Optional.of(delivery(true,null)));
        var endpoint=new Endpoint(app,"https://example.com/hook",null,Set.of());
        ReflectionTestUtils.setField(endpoint,"id",endpointId); endpoint.rotateSigningSecret("current-encrypted");endpoint.configure(true,25,3,12000,4);
        when(endpoints.findByIdAndApplicationId(endpointId,app)).thenReturn(Optional.of(endpoint));
        when(store.eventTypeId(event)).thenReturn(typeId);
        when(types.findByIdAndApplicationId(typeId,app)).thenReturn(Optional.of(new EventType(app,"order.created",null)));
        when(store.rawBody(source)).thenReturn("{\"original\":true}");
        var receipt=service.replay(app,source,"key","request");
        assertNotEquals(source,receipt.id());assertEquals(source,receipt.replayOfId());assertEquals(event,receipt.eventId());
        var command=ArgumentCaptor.forClass(DeliveryCommand.class);
        verify(outbox).enqueue(any(),eq(receipt.id()),eq("event-relay.delivery"),eq("delivery.requested"),command.capture());
        assertEquals(source,command.getValue().replayOfId());assertEquals(1,command.getValue().attemptNumber());
        assertEquals("current-encrypted",command.getValue().signingSecretEncrypted());assertEquals(12000,command.getValue().timeoutMs());
        assertEquals("{\"original\":true}",command.getValue().body());
        verify(events).delivery(receipt.id(),event,app,endpointId,endpoint.getUrl(),source);
        verify(store).replayRequest(eq(app),eq("request"),eq(receipt.id()),any());
    }
    @Test void anotherAppsKeyCannotReadMonitoring(){
        when(keys.authenticate("key")).thenReturn(UUID.randomUUID());
        assertEquals(403,assertThrows(ResponseStatusException.class,()->service.summary(app,"key")).getStatusCode().value());
        verifyNoInteractions(store);
    }
    @Test void invalidPaginationIsRejectedBeforeDatabaseAccess(){
        when(keys.authenticate("key")).thenReturn(app);
        assertEquals(400,assertThrows(ResponseStatusException.class,()->service.events(app,"key","","",-1,20)).getStatusCode().value());
        assertEquals(400,assertThrows(ResponseStatusException.class,()->service.events(app,"key","","",0,101)).getStatusCode().value());
        verifyNoInteractions(store);
    }
    @Test void replayRequiresAnIdempotencyKey(){
        when(keys.authenticate("key")).thenReturn(app);
        assertEquals(400,assertThrows(ResponseStatusException.class,()->service.replay(app,source,"key",null)).getStatusCode().value());
        verifyNoInteractions(store,outbox);
    }
    private MonitoringApi.DeliverySummary delivery(boolean replay,String reason){
        return new MonitoringApi.DeliverySummary(source,event,endpointId,"order.created","https://example.com/hook","DEAD",2,503,"HTTP 503",Instant.now(),Instant.now(),null,null,replay,reason);
    }
}

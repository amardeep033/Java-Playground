package com.eventrelay.serviceb.delivery;

import java.time.Instant;
import java.util.UUID;
import com.eventrelay.serviceb.messaging.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeliveryServiceTest {
    @Mock DeliveryStore store;
    @Mock WebhookSender sender;
    @Mock Outbox outbox;
    DeliveryService service;
    DeliveryCommand command;
    @BeforeEach void setUp() {
        service = new DeliveryService(store, sender, new EndpointGates(), outbox, 1000);
        command = new DeliveryCommand(1, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "order.created", "https://example.com/hook", "{}", "encrypted", 1, 10, 2, 10000, 3, 1, Instant.now());
    }
    @Test void duplicateSucceededDeliveryNeverCallsTheEndpointAgain() {
        when(store.lock(command)).thenReturn(new DeliveryStore.Delivery(command.deliveryId(), command, "SUCCEEDED", 1, 1));
        assertFalse(service.process(command));
        verifyNoInteractions(sender, outbox);
        verify(store).processed(command.messageId(), command.deliveryId());
    }
    @Test void transientFailureCreatesOnlyAnInternalRetry() throws Exception {
        when(store.lock(command)).thenReturn(new DeliveryStore.Delivery(command.deliveryId(), command, "PENDING", 0, 0));
        var result = new WebhookSender.Result(503, "HTTP 503", true, Instant.now(), Instant.now());
        when(sender.send(command, 1)).thenReturn(result);
        assertFalse(service.process(command));
        verify(store).result(any(), eq(1), eq("RETRY_SCHEDULED"), eq(result), notNull());
        var retry = ArgumentCaptor.forClass(DeliveryCommand.class);
        verify(outbox).enqueue(any(), eq(command.deliveryId()), eq("event-relay.retry"), eq("delivery.retry"), retry.capture());
        assertEquals(2, retry.getValue().attemptNumber());
        assertNotEquals(command.messageId(), retry.getValue().messageId());
        assertEquals(command.deliveryId(), retry.getValue().deliveryId());
        verify(outbox,never()).enqueue(any(),any(),eq("event-relay.status"),any(),any());
    }
    @Test void retryExhaustionIsDeadWithoutAnotherRetry() throws Exception {
        var last = command.forAttempt(3);
        when(store.lock(last)).thenReturn(new DeliveryStore.Delivery(command.deliveryId(), command, "RETRY_SCHEDULED", 2, 2));
        var result = new WebhookSender.Result(503, "HTTP 503", true, Instant.now(), Instant.now());
        when(sender.send(command, 3)).thenReturn(result);
        when(store.completion(command.deliveryId())).thenReturn(completion("DEAD",3,503,"HTTP 503"));
        assertTrue(service.process(last));
        verify(store).result(any(), eq(3), eq("DEAD"), eq(result), isNull());
        verify(outbox, never()).enqueue(any(), any(), eq("event-relay.retry"), any(), any());
    }
    @Test void permanentFailureDoesNotRetry() throws Exception {
        when(store.lock(command)).thenReturn(new DeliveryStore.Delivery(command.deliveryId(), command, "PENDING", 0, 0));
        when(sender.send(command, 1)).thenReturn(new WebhookSender.Result(400, "HTTP 400", false, Instant.now(), Instant.now()));
        when(store.completion(command.deliveryId())).thenReturn(completion("DEAD",1,400,"HTTP 400"));
        assertTrue(service.process(command));
        verify(outbox, never()).enqueue(any(), any(), eq("event-relay.retry"), any(), any());
    }
    @Test void futureAttemptIsRejectedBeforeSending() {
        var future = command.forAttempt(3);
        when(store.lock(future)).thenReturn(new DeliveryStore.Delivery(command.deliveryId(), command, "PENDING", 0, 0));
        assertThrows(IllegalArgumentException.class, () -> service.process(future));
        verifyNoInteractions(sender, outbox);
    }
    @Test void successPublishesAStatusWithoutARetry() throws Exception {
        when(store.lock(command)).thenReturn(new DeliveryStore.Delivery(command.deliveryId(), command, "PENDING", 0, 0));
        var result = new WebhookSender.Result(204, null, false, Instant.now(), Instant.now());
        when(sender.send(command, 1)).thenReturn(result);
        var completion=completion("SUCCEEDED",1,204,null);
        when(store.completion(command.deliveryId())).thenReturn(completion);
        assertFalse(service.process(command));
        verify(outbox).enqueue(eq(completion.messageId()),eq(command.deliveryId()),eq("event-relay.status"),eq("delivery.completed"),eq(completion));
        verify(store).result(any(), eq(1), eq("SUCCEEDED"), eq(result), isNull());
        verify(outbox, never()).enqueue(any(), any(), eq("event-relay.retry"), any(), any());
    }
    @Test void retryClassificationMatchesDeliveryPolicy() {
        for (int code : new int[]{408, 425, 429, 500, 502, 503, 504}) assertTrue(WebhookSender.retryable(code));
        for (int code : new int[]{200, 204, 301, 302, 400, 401, 403, 404, 422}) assertFalse(WebhookSender.retryable(code));
    }
    private DeliveryStatusMessage completion(String status,int count,int http,String error) {
        var now=Instant.now();
        var attempts=java.util.stream.IntStream.rangeClosed(1,count).mapToObj(i->new DeliveryStatusMessage.Attempt(i,i==count?http:503,i==count?error:"HTTP 503",now,now,null,null,false)).toList();
        return new DeliveryStatusMessage(2,UUID.randomUUID(),command.deliveryId(),command.eventId(),command.applicationId(),command.endpointId(),status,count,count,http,error,null,now,attempts);
    }
}

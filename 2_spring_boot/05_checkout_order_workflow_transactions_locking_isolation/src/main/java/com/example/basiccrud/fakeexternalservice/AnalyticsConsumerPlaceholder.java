package com.example.basiccrud.fakeexternalservice;

public class AnalyticsConsumerPlaceholder {

    public void consumeOrderEvent(String eventId, String eventType, String orderNumber) {
        // Placeholder only: this class is not called by the checkout flow.
        //
        // Position in architecture:
        //
        // Checkout transaction writes OutboxEvent
        //        ↓
        // KafkaOutboxPublisherPlaceholder publishes event to broker
        //        ↓
        // AnalyticsConsumerPlaceholder receives event later
        //
        // This is different from payment:
        // - payment is directly needed before checkout can return final SUCCESS
        // - analytics is a downstream side effect and checkout should not wait for it
        //
        // Consumer idempotency:
        // Outbox publishing is commonly at-least-once.
        // The same event may arrive more than once.
        // Use eventId/orderNumber to ignore duplicates.
        //
        // Example events:
        // - ORDER_PENDING
        // - ORDER_SUCCESS
        // - ORDER_FAILED
        System.out.println("[ANALYTICS PLACEHOLDER] eventId=" + eventId
                + " type=" + eventType
                + " order=" + orderNumber);
    }
}

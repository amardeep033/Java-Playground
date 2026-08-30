package com.example.basiccrud.fakeexternalservice;

public class KafkaOutboxPublisherPlaceholder {

    public void publishPendingOutboxEvents() {
        // Placeholder only: this class is not called by the checkout flow.
        //
        // Real idea:
        //
        // outbox_events table
        //        ↓
        // read rows where status = PENDING
        //        ↓
        // publish to Kafka/RabbitMQ/SNS
        //        ↓
        // mark row as SENT
        //
        // Important failure case:
        //
        // publish succeeds
        //        ↓
        // app crashes before marking SENT
        //        ↓
        // same outbox row may be published again after restart
        //
        // Therefore:
        //
        // 1. Publisher should be retry-safe.
        // 2. Consumers must be idempotent.
        // 3. Event should contain a stable event/order id so duplicate delivery can be ignored.
        //
        // Delivery semantics:
        // This placeholder is not exactly-once.
        // It is closer to at-least-once publishing because duplicates are possible.
        // If you mark SENT before publishing, you risk at-most-once behavior and can lose events.
        System.out.println("[OUTBOX PLACEHOLDER] read PENDING events, publish, then mark SENT");
    }
}

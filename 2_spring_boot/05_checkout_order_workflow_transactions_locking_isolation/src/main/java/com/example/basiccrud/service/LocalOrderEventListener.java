package com.example.basiccrud.service;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class LocalOrderEventListener {

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void afterOrderTransactionCommit(OrderCreatedLocalEvent event) {
        System.out.println("[LOCAL EVENT AFTER_COMMIT] refresh cache/send non-critical email for " + event.orderNumber());
    }
}

// @TransactionalEventListener(AFTER_COMMIT)
// - good for local, non-critical side effects
// - example: cache refresh, best-effort email, in-process notification

// Outbox
// - better for durable cross-service events
// - event is stored in DB and can be retried by a separate publisher

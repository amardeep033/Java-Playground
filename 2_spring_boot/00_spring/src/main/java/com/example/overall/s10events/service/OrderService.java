package com.example.overall.s10events.service;

import com.example.overall.s10events.event.OrderCreatedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {
    private final ApplicationEventPublisher publisher;

    public OrderService(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    @Transactional // If commented, L3 will not print by default because @TransactionalEventListener has no transaction commit phase to wait for.
    public void placeOrder() {
        System.out.println("S1. Order Placed");

        // Event = a small object describing something that already happened -- after orderPlaced, not "placing order".
        // Publisher does not call Email/Audit services directly; it only announces OrderCreatedEvent.
        // By default, non-async @EventListener methods run synchronously before publishEvent(...) returns.
        // @TransactionalEventListener methods wait for the transaction phase, such as AFTER_COMMIT.
        publisher.publishEvent(new OrderCreatedEvent("ORD-1", "keyboard"));

        System.out.println("S2. Returning from OrderService");
    }
}

// What all can be an event?
// Any ordinary Java object can be a Spring event: record, class, enum-backed object, or even a simple String.
// Prefer a small domain-specific object such as OrderCreatedEvent because it gives listeners clear, typed data.
// Use past-tense names because events describe facts that already happened, not commands to do something.

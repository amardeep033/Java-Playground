package com.example.overall.s10events.listener;

import com.example.overall.s10events.event.OrderCreatedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class OrderEventListener {

    // @EventListener says: call this method when an OrderCreatedEvent is published.
    // This same class can listen to different event types too; event listening is method-level, not class-level.
    // Listener methods can exist in any Spring bean; you do not need one listener class per event type.

    // @Order controls order only among listeners for the same event. Lower number runs first.
    // Without @Order, do not rely on listener execution order.
    // Do not overuse ordering: if listener B truly depends on listener A, that dependency may belong in explicit service code instead.

    @EventListener
    @Order(2)
    public void notifyImmediately(OrderCreatedEvent event) {
        System.out.println("L2. notifyImmediately");
    }

    @EventListener
    @Async // Runs on an async executor, so publishEvent/order flow does not wait for this method to finish. @EnableAsync must be enabled in config.
    @Order(3)
    public void auditImmediately(OrderCreatedEvent event) {
        System.out.println("L1. auditImmediately");
    }

    @Order(1) // Still prints after L1/L2 because @Order only orders listeners within the same timing group; this one waits for AFTER_COMMIT first.
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void afterCommit(OrderCreatedEvent event) {
        System.out.println("L3. afterCommit " + event.orderId());
    }
}


// What if @Order(1) is async and @Order(2) is sync?
// @Order can control the order in which Spring invokes/submits listener methods,
// but the async listener returns control quickly and continues on another thread.
// So the sync listener may finish before the async listener's real work finishes.


// Async does not mean "after commit"; async only changes the thread/blocking behavior.
// event listener -- immediate + blocking
// async event listener -- immediate + non-blocking
// transactional event listener -- transaction phase such as after commit + blocking
// async transactional event listener -- transaction phase such as after commit + non-blocking

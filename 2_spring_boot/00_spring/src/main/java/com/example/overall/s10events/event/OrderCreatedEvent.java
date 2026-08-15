package com.example.overall.s10events.event;

// Spring events can be ordinary objects. No special base class is required.
// A Java record is a compact class for carrying data; Java generates constructor/accessors/equals/hashCode/toString.
// Record fields are final, so it works well for simple immutable event payloads.
public record OrderCreatedEvent(String orderId, String itemName) {
    public OrderCreatedEvent {
        System.out.println("X. Order created");
    }
}

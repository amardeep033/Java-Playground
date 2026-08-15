package com.example.overall.s7aopandproxies.service;

import org.springframework.stereotype.Service;

// Target bean: Spring wraps this object in a proxy because LoggingAspect has matching advice.
@Service
public class OrderService {

    public int calculateTotal(String itemName) {
        // When called from Main using the Spring bean reference, this is reached through proxy -> target.
        // When called from another OrderService method, this is reached directly as target -> target.
        System.out.println("S0. calculateTotal");
        return itemName.length() * 100;
    }

    // When we write without using this:
    // Java still treats calculateTotal(itemName) as this.calculateTotal(itemName).
    // So this internal call is also self-invocation and does not go back through the proxy.
    public void placeOrderE(String itemName) {
        // This method itself is reached through proxy -> target when called from OrderController.
        // The nested calculateTotal call below is direct target -> target.
        System.out.println("S1. placeOrderE");
        calculateTotal(itemName);
    }

    // When we write with using this:
    // This makes the self-invocation explicit, but behavior is the same as calling calculateTotal(itemName).
    public void placeOrderI(String itemName) {
        // This method itself is reached through proxy -> target when called from OrderController.
        // The nested this.calculateTotal call below is direct target -> target.
        System.out.println("S2. placeOrderI");
        this.calculateTotal(itemName);
    }
}

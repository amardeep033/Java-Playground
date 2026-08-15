package com.example.overall.s0withoutspring;

public class OrderService {
    private final LoggerService logger;

    // Constructor injection: the dependency is supplied from outside the class.
    public OrderService(LoggerService logger) {
        this.logger = logger;
    }

    void placeOrder() {
        System.out.println("Without Spring: order placed");
        logger.log("Order created");
    }
}

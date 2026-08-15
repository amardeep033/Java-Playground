package com.example.overall.s1manualconfig;

// This class is exactly the same as the one in the example without Spring.
public class OrderService {
    private final LoggerService logger;

    // Constructor injection: the dependency is supplied from outside the class.
    public OrderService(LoggerService logger) {
        this.logger = logger;
    }

    void placeOrder() {
        System.out.println("With Spring: order placed");
        logger.log("Order created");
    }
}

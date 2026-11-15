package com.example.overall.s2autoconfig;

import org.springframework.stereotype.Component;

@Component
public class OrderService {
    private final LoggerService logger;

    public OrderService(LoggerService logger) {
        this.logger = logger;
    }

    void placeOrder() {
        System.out.println("With Spring Component: order placed");
        logger.log("Order created");
    }
}

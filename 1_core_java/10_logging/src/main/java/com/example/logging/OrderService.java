package com.example.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OrderService {
    /*
     * One static final logger per class is the normal style.
     * This log will show OrderService as the source instead of LoggingApplication.
     */
    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    public void placeOrder(String orderId) {
        log.info("EEEE orderId={}", orderId);
    }

    public void failOrder(String orderId) {
        throw new IllegalStateException("Cannot place order " + orderId);
    }
}

package com.example.overall.s3diresolution;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class OrderService {
    private final LoggerService defaultLogger;
    private final LoggerService auditLogger;

    // @Autowired
    public OrderService(LoggerService defaultLogger) {
        this.defaultLogger = defaultLogger;
        this.auditLogger = defaultLogger;
    }

    @Autowired
    public OrderService(
            LoggerService defaultLogger,
            @Qualifier("xyz") LoggerService auditLogger) {
        this.defaultLogger = defaultLogger;
        this.auditLogger = auditLogger;
    }

    void placeOrder() {
        defaultLogger.log("Default");
        auditLogger.log("Audit");
    }
}
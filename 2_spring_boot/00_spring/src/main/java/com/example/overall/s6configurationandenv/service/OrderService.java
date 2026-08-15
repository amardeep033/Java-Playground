package com.example.overall.s6configurationandenv.service;

import com.example.overall.s6configurationandenv.logger.LoggerService;

// AppConfig registers this class explicitly with @Bean, so @Component is not needed here.
// It contains no dev/prod check because Spring injects the LoggerService selected by @Profile.
public class OrderService {
    private final LoggerService loggerService;

    public OrderService(LoggerService loggerService) {
        this.loggerService = loggerService;
    }

    public void placeOrder() {
        System.out.println("Order placed");
        loggerService.log("Order created");
    }
}

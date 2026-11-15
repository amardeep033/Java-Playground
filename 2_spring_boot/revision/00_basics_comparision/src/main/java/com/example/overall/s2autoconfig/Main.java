package com.example.overall.s2autoconfig;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// SPRING BOOT: component scanning + auto-configuration.
@SpringBootApplication
public class Main {
    public static void main(String[] args) {
        // Same lookup and service call as plain Spring; Boot creates the context.
        try (var context = SpringApplication.run(Main.class, args)) {
        // try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class)) {
            OrderService orderService = context.getBean(OrderService.class);
            orderService.placeOrder();
        }
    }
}

// Three ways to trigger a service method
// Approach                | Who calls the service?            | When?
// ------------------------+-----------------------------------+---------------------------------------------------
// getBean() + method call | Your main()                       | After startup, when you explicitly call the method
// CommandLineRunner       | Your run() method, called by Boot | During startup, after context refresh
// @EventListener          | Your listener, called by Spring   | When the matching event is published
// getBean() only retrieves the bean; it does not execute the service method.

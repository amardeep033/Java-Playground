package com.example.overall.s2autoconfig;

import org.springframework.stereotype.Component;

// @Component tells Spring to create and manage OrderService as a bean.
// In Spring applications, service-layer classes are usually marked with @Service, which is a specialized form of @Component.
@Component
public class OrderService {
    private final LoggerService logger;

    // Constructor injection is still the DI mechanism.
    // Spring sees LoggerService in the constructor and injects the matching FileLogger bean.
    public OrderService(LoggerService logger) {
        this.logger = logger;
    }

    // Note: if a class has one constructor, Spring can use it without @Autowired.
    // If a class has multiple constructors, use @Autowired to tell Spring which constructor should be used for injection.
    // If Spring cannot clearly choose a constructor, the application fails during context startup.
    // @Autowired basically says: "Spring, resolve and inject the dependency here."
    // It does not say: "Spring, pick any implementation."

    void placeOrder() {
        System.out.println("With Spring Component: order placed");
        logger.log("Order created");
    }
}


// These are common specialized forms of @Component that express the role of a class.
// @Component: create and manage this Java object as a bean.
//    ↓ These specialized annotations will become more useful when we study MVC and persistence.
// @Service: marks a service-layer class that holds business logic.
// @Repository: marks a persistence/data-access class and can participate in persistence exception translation when that infrastructure is enabled.
// @Controller: marks an MVC controller that handles web requests.

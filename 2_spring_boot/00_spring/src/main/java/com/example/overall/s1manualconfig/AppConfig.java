package com.example.overall.s1manualconfig;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// In configuration, we state which objects should become beans and how they should be wired.
// This is better than putting wiring in main when a large project has many dependencies.
// We can still argue that the wiring is manual here because we are explicitly declaring each bean.
// In Spring Boot, the same setup becomes easier through auto-configuration and sensible defaults.
@Configuration
public class AppConfig {

    // @ is used to write an annotation in Java.
    // Here, @Bean tells Spring to register the returned object as a bean.
    // We have two application objects: loggerService and orderService(loggerService), so we declare two bean methods.
    // We are not putting business logic here; we are only describing object creation and relationships.

    @Bean
    // LoggerService logger = new FileLogger();
    LoggerService loggerService() {
        return new FileLogger();
    }

    @Bean
    // OrderService orderService = new OrderService(logger);
    OrderService orderService(LoggerService loggerService) {
        return new OrderService(loggerService);
    }
}

// Now object creation becomes application-wide wiring code; the object graph is externalized from business/application code into container configuration.
// @Configuration: a class that can explicitly declare bean wiring.
// @Bean: a method inside configuration that returns one Spring-managed object.
// @Component: a class-level annotation used later for automatic detection through component scanning.
// If multiple beans match the same interface, Spring needs a clear choice through configuration, @Primary, or @Qualifier.

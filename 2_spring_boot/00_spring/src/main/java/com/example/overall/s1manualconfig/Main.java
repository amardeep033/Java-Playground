package com.example.overall.s1manualconfig;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

// Spring is a framework that manages objects and their relationships.
// It also provides infrastructure for areas like database access, transactions, web MVC, and AOP.
// "Spring is a framework that provides infrastructure such as dependency injection, bean management, MVC, transactions, and AOP. At its core, the ApplicationContext manages application objects, called beans, and wires their dependencies."
public class Main {
    public static void main(String[] args) {

        // ApplicationContext -> Spring container -> beans -> objects used by the application.
        // AppConfig keeps all relationships and mappings in one place, so Spring can manage object creation, DI, relationships, and lifecycle.
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class)) {

            // OrderService orderService = new OrderService(logger);
            // We did not create the logger object, and we did not pass it manually.
            // getBean asks the container for the managed bean; by default, non-lazy singleton beans are created when the context starts.
            OrderService orderService = context.getBean(OrderService.class);
            orderService.placeOrder();
        }
        
    }
}

package com.example.overall.s4mvcarchitecture;

import com.example.overall.s4mvcarchitecture.config.AppConfig;
import com.example.overall.s4mvcarchitecture.controller.OrderController;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class Main {
    public static void main(String[] args) {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class)) {
            OrderController controller = context.getBean(OrderController.class);

            System.out.println("A. Request received");
            String response = controller.placeOrder("Book");
            System.out.println("F: Response: " + response);
        }
    }
}

// MVC means Model-View-Controller. In a REST API, there is usually no server-rendered View; the response body is the representation returned to the client. In casual discussion, people sometimes call the entire Controller -> Service -> Repository layered flow "MVC," although MVC and layered architecture are distinct patterns.

// 1. Client
// 2. Router (API endpoint mapping) -- DispatcherServlet receives the request, and HandlerMapping finds the matching controller method
// 3. Controller (HTTP concerns only: request parsing, validation trigger, and response creation) -- @Controller
// 4. Service (application flow and business logic orchestration) -- @Service
// 5. Repository (Data Access Layer: hides storage operations) -- @Repository
// 6. Database

// Model is not a separate step in the flow; it is the data that flows through the layers, and it may change shape at layer boundaries.
// DTO at the Controller boundary: represents request or response data and protects the API contract from internal models.
// Domain model in the Service/business layer: represents business concepts, state, and rules.
// Persistence Entity in the Repository layer: represents how data is stored, often using JPA annotations such as @Entity.

// In a real project, a feature-first package structure could look like this:
// order/
// ├── controller/
// │   └── OrderController.java
// ├── dto/
// │   ├── CreateOrderRequest.java
// │   └── OrderResponse.java
// ├── service/
// │   └── OrderService.java
// ├── domain/
// │   └── Order.java
// ├── repository/
// │   └── OrderRepository.java
// └── entity/
//     └── OrderEntity.java

// Why not mark everything with @Component? All three stereotypes are components, but the specialized names communicate architectural intent and can add layer-specific behavior.
// @Service -- primarily gives semantic and architectural meaning to a service-layer component.
// @Repository -- marks a persistence component and enables translation of supported persistence exceptions into Spring's DataAccessException hierarchy.
// @Controller -- Spring MVC recognizes it as a web controller, allowing request mappings such as @GetMapping to be handled there.


// Layered Architecture -- structural pattern: organizes code by responsibility, commonly Controller -> Service -> Repository -> Database, with each layer calling the layer below it.
// DDD -- design approach: models complex business behavior using a shared domain language, bounded contexts, entities, value objects, aggregates, and domain services.
// Clean Architecture -- dependency-oriented structural pattern: dependencies point inward, so infrastructure depends on application/domain code, never the reverse. It can complement DDD, but neither requires the other.
//         ┌─────────────────────────────┐
//         │   Frameworks & Drivers      │  (Web framework, DB implementation, UI, external APIs)
//         │  ┌───────────────────────┐  │
//         │  │  Interface Adapters   │  │  (Presenters, Gateways, Controllers)
//         │  │  ┌─────────────────┐  │  │
//         │  │  │  Application    │  │  │  (Use Cases / Application Services)
//         │  │  │  ┌────────────┐ │  │  │
//         │  │  │  │  Domain    │ │  │  │  (Entities, business rules — the core)
//         │  │  │  └────────────┘ │  │  │
//         │  │  └─────────────────┘  │  │
//         │  └───────────────────────┘  │
//         └─────────────────────────────┘

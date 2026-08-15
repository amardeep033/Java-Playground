package com.example.overall.s5beanlifecycle;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

// In this example, Spring creates only one OrderService for the whole ApplicationContext.
// We can place any number of orders through that same service, and every order uses the same long-lived connection owned by the service.
// The order-specific data must not be shared, so every call to placeOrder asks Spring for a new prototype OrderDraft.
// In a web application, RequestOrderContext would create separate state for each request, while ShoppingSession would keep state across requests from the same session.

// | Scope     | Detail                                        | Real-world use                              | Thread behavior                                                    | Used in this example                                          |
// |-----------|-----------------------------------------------|---------------------------------------------|--------------------------------------------------------------------|---------------------------------------------------------------|
// | singleton | One instance per ApplicationContext; default | Stateless services, clients, shared resources | Many threads may use the same object; keep it stateless/thread-safe | OrderService                                                  |
// | prototype | New instance for every container lookup       | Short-lived objects needing Spring DI       | Not one per thread; each lookup is new, but callers can still share it | OrderDraft, requested through ObjectProvider                  |
// | request   | One instance per active HTTP request           | Request ID, tenant, or request context      | Bound to the request, not the thread; used only within that request | RequestOrderContext; metadata only because there is no request |
// | session   | One instance per active HTTP session           | Shopping cart or user-session state         | Requests in the same session may run concurrently; make mutable state thread-safe | ShoppingSession; metadata only because there is no session |

// If singleton beans are shared by many requests and threads, why shouldn't we store request-specific mutable data inside a singleton service?
// Concurrent requests use the same object, so mutable fields can cause race conditions or leak one user's data into another request. Keep singleton services stateless and keep request data in method-local variables or request-scoped objects.

public class Main {
    public static void main(String[] args) {
        System.out.println("1. Creating ApplicationContext");

        try (AnnotationConfigApplicationContext context =
                     new AnnotationConfigApplicationContext(AppConfig.class)) {
            System.out.println("4. ApplicationContext is ready");

            OrderService firstReference = context.getBean(OrderService.class);
            OrderService secondReference = context.getBean(OrderService.class);
            System.out.println("5. Singleton: same OrderService object = "
                    + (firstReference == secondReference));

            firstReference.placeOrder("Book");
            secondReference.placeOrder("Pen");

            String requestScope = context.getBeanFactory()
                    .getBeanDefinition("scopedTarget.requestOrderContext")
                    .getScope();
            String sessionScope = context.getBeanFactory()
                    .getBeanDefinition("scopedTarget.shoppingSession")
                    .getScope();

            System.out.println("8. Web scope metadata: " + requestScope + ", " + sessionScope);
            System.out.println("9. Closing ApplicationContext");
        }
    }
}

// Bean lifecycle for a singleton managed by the ApplicationContext:
// 1. Spring creates the bean by calling its constructor.
// 2. Spring injects its dependencies.
// 3. Spring calls its @PostConstruct method once.
// 4. The bean is ready and can be used by the application.
// 5. When the context closes, Spring calls its @PreDestroy method once.

// @PostConstruct is useful for initialization that must happen after dependency injection, such as validating configuration, warming a cache, or opening a client/resource.
// @PreDestroy is useful for cleanup, such as closing a client/resource, stopping a worker thread, flushing buffered data, or releasing a connection pool.
// Do not manually call these annotated methods for normal singleton beans; Spring calls them as part of the bean lifecycle.

// Spring manages the complete lifecycle of singleton beans, including @PreDestroy.
// For prototype beans, Spring creates and initializes each instance but does not automatically call @PreDestroy; the code using a resource-owning prototype must clean it up.
// Request and session destruction are managed by the web context when the request or session ends.

// This plain-Spring example has no web server, so requestOrderContext and shoppingSession are declared only to show their real scope metadata.
// Resolving either scoped target here would fail because no HTTP request or session is active.

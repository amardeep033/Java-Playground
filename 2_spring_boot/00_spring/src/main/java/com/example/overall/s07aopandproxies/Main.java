package com.example.overall.s7aopandproxies;

import com.example.overall.s7aopandproxies.caller.OrderController;
import com.example.overall.s7aopandproxies.config.AppConfig;
import com.example.overall.s7aopandproxies.service.OrderService;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

// AOP = Aspect-Oriented Programming. While OOP organizes code into classes, AOP organizes code into aspects.
// AOP is used to separate cross-cutting concerns such as logging, security, transactions, and caching from business logic.
// Spring gives built-in proxy-based features such as @Transactional, @Cacheable, @Async, and @PreAuthorize.
// You usually add the annotation and enable the feature; Spring creates the proxy instead of you writing wrapper classes.
// Manual proxy means writing one wrapper around each required class and duplicating wrapper methods for each required function.

// AOP (caller/controller)
//  │
//  ├── Proxy   → intercepts the call
//  ├── Advice  → what extra behavior to execute (Log something, Check authorization, Start a transaction, Measure execution time, Cache a result)
//  ├── Pointcut → which methods to apply it to
//  └── Target  → actual business object

public class Main {
    public static void main(String[] args) {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class)) {
            OrderController orderController = context.getBean(OrderController.class);
            OrderService orderService = context.getBean(OrderService.class);

            // prints false because no pointcut/advice in LoggingAspect matches OrderController methods.
            // @Controller only registers a bean; it does not automatically make that bean an AOP proxy.
            System.out.println("M1. orderController " + AopUtils.isAopProxy(orderController));

            // prints true because LoggingAspect has pointcuts matching OrderService methods.
            // Spring therefore returns a proxy object for OrderService instead of the raw target object.
            System.out.println("M2. orderService " + AopUtils.isAopProxy(orderService));

            // Below are three things:
            // 1. placeOrderFromOutside -- external caller -> proxy -> target. Advice on placeOrderE runs.
            // 2. placeOrderWithSelfInvocation -- external caller enters placeOrderI through proxy, but inner calculateTotal call uses this and skips proxy.
            // 3. calculateTotal from Main -- direct external call to calculateTotal goes through proxy, so @AfterReturning advice runs.

            System.out.println();
            orderController.placeOrderFromOutside();

            System.out.println();
            orderController.placeOrderWithSelfInvocation();

            System.out.println();
            orderService.calculateTotal("main_item");
        }
    }
}

// Common Spring annotations/features that can cause a bean to be proxied:
// @Transactional → transaction proxy
// @Async → async proxy
// @Cacheable, @CachePut, @CacheEvict → caching proxy
// @Retryable → retry proxy
// @PreAuthorize, @Secured → security proxy
// @Aspect + pointcuts like @Around, @Before → AOP proxy

// Does using @Transactional or @Around directly convert into proxy or do we need @Aspect and @EnableAspectJAutoProxy(proxyTargetClass = true)?
// @Transactional is handled by Spring's transaction infrastructure when transaction management is enabled.
// @Around is only advice metadata; it must live inside an @Aspect bean, and Spring AOP must be enabled for that advice to create proxies.
// The difference between directly using @Transactional vs @Around with @Aspect vs using after @EnableAspectJAutoProxy(proxyTargetClass = true):
// @Transactional = ready-made transaction advice; @Around inside @Aspect = your custom advice; @EnableAspectJAutoProxy = enables custom @Aspect proxy processing.
// Also to know which all will become proxy do System.out.println(bean.getClass()); and it will print something like com.example.OrderController$$SpringCGLIB$$0
// $$SpringCGLIB$$0 means: Spring created a CGLIB class-based proxy, usually a generated subclass around the target bean.

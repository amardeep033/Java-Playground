package com.example.overall.s7aopandproxies.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

// proxyTargetClass = true asks Spring for class-based proxies, so OrderService can be proxied without an interface.
// Q1. What are different based proxies?
// 1. JDK dynamic proxy: used when you proxy through an interface; the proxy implements that interface.
// 2. CGLIB proxy: used when class-based proxying is needed; the proxy is a generated subclass of the target class.

@Configuration
@ComponentScan("com.example.overall.s7aopandproxies")
// If we use @Transactional or @Aspect, do we still need this?
// For custom @Aspect advice in plain Spring, yes: @EnableAspectJAutoProxy turns on annotation-driven AOP proxy creation.
// For @Transactional, you normally enable transaction management with @EnableTransactionManagement or Boot auto-configuration.
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class AppConfig {
}

// Q2. What does it mean by without an interface? -- add an example as comment here
// With interface:
//   interface OrderOperations { void placeOrderE(String itemName); }
//   class OrderService implements OrderOperations { ... }
//   Spring can create a JDK proxy that implements OrderOperations.
// Without interface:
//   class OrderService { void placeOrderE(String itemName) { ... } }
//   There is no interface to implement, so Spring uses a CGLIB subclass proxy when proxyTargetClass = true.

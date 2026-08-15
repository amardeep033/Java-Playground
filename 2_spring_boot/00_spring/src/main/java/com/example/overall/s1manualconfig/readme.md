# s1manualconfig

## Goal

This example shows dependency injection with plain Spring Framework using explicit `@Bean` methods.

```text
DI without Spring = Main creates and wires objects manually.
DI with Spring    = ApplicationContext reads AppConfig and wires beans.
```

## How To Run

From `00_spring/`:

```bash
mvn -q compile exec:java -Dexec.mainClass=com.example.overall.s1manualconfig.Main
```

Expected output:

```text
With Spring: order placed
File log: Order created
```

## What Problem Spring Solves Here

`OrderService` still uses constructor injection:

```java
public OrderService(LoggerService logger) {
    this.logger = logger;
}
```

Without Spring, `Main` must assemble the object graph manually:

```java
LoggerService logger = new FileLogger();
OrderService orderService = new OrderService(logger);
```

In this version, the wiring moves into `AppConfig`:

```java
@Bean
LoggerService loggerService() {
    return new FileLogger();
}

@Bean
OrderService orderService(LoggerService loggerService) {
    return new OrderService(loggerService);
}
```

Spring calls these methods, registers the returned objects as beans, and injects `LoggerService` into `OrderService`.

## Important Annotations

| Annotation | Meaning |
| --- | --- |
| `@Configuration` | Marks a class that can contain bean definitions. |
| `@Bean` | Registers the object returned by a method as a Spring-managed bean. |

## How Spring Wires It

1. `Main` creates `AnnotationConfigApplicationContext`.
2. The context reads `AppConfig.class`.
3. Spring calls `loggerService()` and registers the returned `FileLogger` object as a `LoggerService` bean.
4. Spring sees that `orderService(LoggerService loggerService)` needs a `LoggerService`.
5. Spring calls `orderService(...)`, passes the `LoggerService` bean into it, and registers the returned `OrderService` object as a bean.
6. `Main` calls `context.getBean(OrderService.class)` to get the managed `OrderService` object.

## Mental Model

```text
Main
  -> starts ApplicationContext
      -> reads AppConfig
          -> creates beans from @Bean methods
              -> injects dependencies
```

## Interview Q&A

| Question | Strong short answer |
| --- | --- |
| If both examples use DI, what does Spring add? | Spring moves wiring from application code into container configuration and manages the object graph centrally. |
| What is a Spring bean? | An object managed by the Spring container. It may be instantiated directly by Spring or returned from a factory method such as an `@Bean` method. |
| Does `getBean()` always create a new object? | No. For default non-lazy singleton beans, Spring creates the bean when the context starts and `getBean()` returns the managed instance. |
| Is `AppConfig` business logic? | No. It is configuration code that describes object creation and relationships. |
| Scenario: You need to switch from file logging to database logging. What changes here? | Change the `LoggerService` bean method to return `DatabaseLogger`; `OrderService` stays unchanged. |
| Scenario: You add a constructor parameter to `OrderService`. What must happen? | A matching bean must exist, and the `@Bean` method must accept/pass that dependency. |

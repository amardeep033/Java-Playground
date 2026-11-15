# s2autoconfig

## Goal

This example shows dependency injection with plain Spring Framework using `@Component` and `@ComponentScan`.

```text
s1manualconfig = beans declared explicitly with @Bean methods.
s2autoconfig   = beans discovered automatically from @Component classes.
```

## How To Run

From `00_spring/`:

```bash
mvn -q compile exec:java -Dexec.mainClass=com.example.overall.s2autoconfig.Main
```

Expected output:

```text
With Spring Component: order placed
File log: Order created
```

## What Problem Spring Solves Here

`OrderService` still uses constructor injection:

```java
public OrderService(LoggerService logger) {
    this.logger = logger;
}
```

Instead of declaring each bean in `AppConfig`, concrete classes mark themselves:

```java
@Component
public class FileLogger implements LoggerService {
}

@Component
public class OrderService {
}
```

`AppConfig` only tells Spring where to start scanning:

```java
@Configuration
@ComponentScan
public class AppConfig {
}
```

Spring scans the package of `AppConfig`, discovers component classes, registers them as beans, and injects `FileLogger` into `OrderService`.

## Important Annotations

| Annotation | Meaning |
| --- | --- |
| `@Configuration` | Marks a class that can act as a Spring configuration entry point. |
| `@ComponentScan` | Tells Spring to scan the package of the config class and its subpackages. |
| `@Component` | Marks a class as a candidate for discovery and bean registration. |
| `@Service` | Specialized form of `@Component` for service-layer classes. |
| `@Repository` | Specialized form of `@Component` for data-access classes. |
| `@Controller` | Specialized form of `@Component` for MVC controllers. |

## How Spring Wires It

1. `Main` creates `AnnotationConfigApplicationContext`.
2. The context reads `AppConfig.class`.
3. `@ComponentScan` tells Spring to scan `com.example.overall.s2autoconfig` and its subpackages.
4. Spring finds `FileLogger` because it has `@Component`.
5. Spring finds `OrderService` because it has `@Component`.
6. Spring sees that `OrderService` needs a `LoggerService`.
7. Spring injects the discovered `FileLogger` bean because it implements `LoggerService`.
8. `Main` calls `context.getBean(OrderService.class)` to get the managed `OrderService` object.

## Mental Model

```text
Main
  -> starts ApplicationContext
      -> reads AppConfig
          -> scans for @Component classes
              -> creates beans
                  -> injects dependencies
```

## `@Bean` vs `@Component`

| Topic | `@Bean` | `@Component` |
| --- | --- | --- |
| Where used | On a method inside a config class | On the class itself |
| Registration style | Explicit factory-method registration | Class-level auto-discovery |
| Best for | Third-party classes or very explicit wiring | Your own application classes |
| Example | `LoggerService loggerService() { return new FileLogger(); }` | `@Component class FileLogger implements LoggerService` |

## Interview Q&A

| Question | Strong short answer |
| --- | --- |
| What does `@Component` do? | It marks a class as a candidate for component scanning, so Spring can discover and register it as a bean. |
| What does `@ComponentScan` do? | It tells Spring which package tree to scan. Without an explicit base package, it scans from the package of the configuration class. |
| Why is `AppConfig` still needed? | Plain Spring needs a configuration entry point so it knows where component scanning starts. |
| Can an interface be a component by itself? | No. Spring needs a concrete implementation class to instantiate. |
| What happens if two beans implement `LoggerService`? | Spring cannot choose by type alone and throws `NoUniqueBeanDefinitionException` unless one bean is marked primary or selected with a qualifier. |
| Scenario: `FileLogger` is missing `@Component`. What happens? | Spring cannot register it as a bean through scanning, so `OrderService` dependency resolution fails. |
| Scenario: You move `FileLogger` outside the scanned package. What happens? | Spring will not discover it unless the scan base package is expanded. |
| Scenario: You have `FileLogger` and `DbLogger`. How do you choose one? | Use `@Primary`, `@Qualifier`, or explicit configuration. |

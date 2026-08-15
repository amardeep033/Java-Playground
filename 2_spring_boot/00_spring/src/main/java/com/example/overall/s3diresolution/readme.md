# s3diresolution

## Goal

This example shows dependency resolution with plain Spring Framework using `@Autowired`, `@Primary`, and `@Qualifier`.

```text
s2autoconfig   = one LoggerService implementation is discovered and injected.
s3diresolution = multiple LoggerService implementations exist, so Spring needs a clear choice.
```

It continues the same order/logging example from `s2autoconfig`. MVC comes later in `s4mvcarchitecture`.

## How To Run

From `00_spring/`:

```bash
mvn -q compile exec:java -Dexec.mainClass=com.example.overall.s3diresolution.Main
```

Expected output:

```text
With Spring DI resolution: order placed
@Primary selected FileLogger
File log: Order created
@Qualifier selected DatabaseLogger
Database log: Order audit saved
```

## What Problem Spring Solves Here

`OrderService` has two constructors:

```java
public OrderService(LoggerService defaultLogger) {
    this.defaultLogger = defaultLogger;
    this.auditLogger = defaultLogger;
}
```

This could be useful for a simple/manual setup, but it is not the constructor we want Spring to use in this example.

The Spring-managed constructor needs two `LoggerService` dependencies:

```java
@Autowired
public OrderService(
        LoggerService defaultLogger,
        @Qualifier("databaseLogger") LoggerService auditLogger) {
    this.defaultLogger = defaultLogger;
    this.auditLogger = auditLogger;
}
```

Spring scans three implementations:

```java
@Component
@Primary
public class FileLogger implements LoggerService {
}

@Component
public class ConsoleLogger implements LoggerService {
}

@Component("databaseLogger")
public class DatabaseLogger implements LoggerService {
}
```

For `defaultLogger`, Spring uses `FileLogger` because it is marked `@Primary`.

For `auditLogger`, Spring uses `DatabaseLogger` because the constructor parameter has `@Qualifier("databaseLogger")`.

## Important Annotations

| Annotation | Meaning |
| --- | --- |
| `@Autowired` | Tells Spring which constructor to use when a class has multiple constructors. |
| `@Primary` | Marks one bean as the default choice when multiple beans match the same type. |
| `@Qualifier` | Selects a specific bean by name when type alone is not enough. |
| `@Component` | Marks a class as a candidate for component scanning and bean registration. |
| `@ComponentScan` | Tells Spring where to scan for component classes. |

## How Spring Wires It

1. `Main` creates `AnnotationConfigApplicationContext`.
2. The context reads `AppConfig.class`.
3. `@ComponentScan` tells Spring to scan `com.example.overall.s3diresolution`.
4. Spring finds `OrderService`, `FileLogger`, `ConsoleLogger`, and `DatabaseLogger`.
5. Spring sees multiple constructors in `OrderService`.
6. `@Autowired` tells Spring to use the two-argument constructor.
7. For `defaultLogger`, three beans match `LoggerService`, so Spring injects `FileLogger` because it has `@Primary`.
8. For `auditLogger`, `@Qualifier("databaseLogger")` tells Spring to inject the bean named `databaseLogger`.
9. `Main` calls `context.getBean(OrderService.class)` to get the managed `OrderService` object.

## Mental Model

```text
Main
  -> starts ApplicationContext
      -> reads AppConfig
          -> scans @Component classes
              -> sees multiple LoggerService beans
                  -> @Autowired chooses the constructor
                      -> @Primary chooses the default logger
                      -> @Qualifier chooses the exact audit logger
```

## Same Line As `s2`

| Version | Logger setup | What Spring learns |
| --- | --- | --- |
| `s2autoconfig` | `LoggerService -> FileLogger` | One matching bean is simple to inject. |
| `s3diresolution` | `LoggerService -> FileLogger`, `ConsoleLogger`, `DatabaseLogger` | Multiple matching beans need `@Primary` or `@Qualifier`. |

## Interview Q&A

| Question | Strong short answer |
| --- | --- |
| What does `@Autowired` do here? | It tells Spring which constructor to use because `OrderService` has multiple constructors. |
| Is `@Autowired` required on a single constructor? | No. If a Spring bean has only one constructor, Spring can use it automatically. |
| Why can multiple implementations cause a problem? | If Spring finds multiple beans for the same interface, it cannot choose by type alone. |
| What does `@Primary` do? | It marks one bean as the default when multiple beans match the required type. |
| What does `@Qualifier` do? | It selects a specific bean by name and overrides the default choice. |
| Which wins: `@Primary` or `@Qualifier`? | `@Qualifier` wins because it asks for a specific bean. |
| Scenario: remove `@Autowired` from the two-argument constructor. What happens? | Spring has multiple constructors and no clear constructor choice, so context startup fails. |
| Scenario: remove `@Primary` from `FileLogger`. What happens? | The unqualified `LoggerService defaultLogger` dependency becomes ambiguous and context startup fails. |
| Scenario: remove `@Qualifier("databaseLogger")`. What happens? | Spring injects the primary `FileLogger` for both `LoggerService` dependencies. |
| Scenario: change `@Qualifier("databaseLogger")` to a wrong name. What happens? | Spring cannot find that named bean and context startup fails. |

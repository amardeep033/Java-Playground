# s6configurationandenv

## Goal

Compare two equivalent configuration chains using the same small `OrderService -> LoggerService` example:

```text
application-dev.properties -> @Profile("dev")  -> @Value
app-prod.yaml (explicit)   -> @Profile("prod") -> @ConfigurationProperties
```

Both profiles inherit shared defaults from `application.properties`.

## How To Run

From `00_spring/`:

```bash
# dev
mvn -q compile exec:java -Dexec.mainClass=com.example.overall.s6configurationandenv.Main -Dexec.args="--spring.profiles.active=dev"

# prod
mvn -q compile exec:java -Dexec.mainClass=com.example.overall.s6configurationandenv.Main -Dexec.args="--spring.profiles.active=prod"
```

Other profile activation methods include:

```bash
# JVM system property
mvn -q compile exec:java -Dexec.mainClass=com.example.overall.s6configurationandenv.Main -Dspring.profiles.active=dev

# OS environment variable
SPRING_PROFILES_ACTIVE=prod mvn -q compile exec:java -Dexec.mainClass=com.example.overall.s6configurationandenv.Main
```

The runnable examples use the command-line argument so the selected profile is explicit in each command.

## Configuration Matrix

| Key | `application.properties` | `application-dev.properties` | `app-prod.yaml` | Effective dev | Effective prod |
| --- | --- | --- | --- | --- | --- |
| `logger.app-name` | `Learning Orders` | Not defined | Not defined | `Learning Orders` | `Learning Orders` |
| `logger.level` | `INFO` | `DEBUG` | `ERROR` | `DEBUG` | `ERROR` |
| `logger.destination` | Not defined | `CONSOLE` | `FILE` | `CONSOLE` | `FILE` |
| `logger.format` | Not defined | Not defined | Not defined | `PLAIN` from `@Value` fallback | `PLAIN` from Java field default |

- **Inherited default:** `app-name` exists only in the base file, so both profiles use it.
- **Overridden default:** `level` starts as `INFO`, then the active profile replaces it.
- **Profile-only value:** `destination` comes directly from the active profile file.
- **Code default:** `format` is absent from every file, so each binding style supplies `PLAIN` in code.

## Mental Model

```text
--spring.profiles.active=dev
        |
        +-> application.properties -----> app-name=Learning Orders, level=INFO
        +-> application-dev.properties -> level=DEBUG overrides INFO, destination=CONSOLE
        |
        v
@Profile("dev") -> DevLogger -> individual @Value fields -> OrderService
```

```text
--spring.profiles.active=prod
        |
        +-> application.properties -> app-name=Learning Orders, level=INFO
        +-> application.properties imports app-prod.yaml
        +-> app-prod.yaml activates only for prod -> level=ERROR overrides INFO, destination=FILE
        |
        v
@Profile("prod") -> ProdLoggerProperties -> ProdLogger -> OrderService
```

The active profile affects both configuration values and bean registration at the same time:

```text
spring.profiles.active=dev
          |
          +-------------------------+
          v                         v
application-dev.properties    @Profile("dev")
          v                         v
logger.level=DEBUG             DevLogger registered
          |                         |
          +------------+------------+
                       v
                  OrderService
```

The property branch answers, "What values should the application use?" The bean branch answers, "Which implementation should exist?" `OrderService` stays unchanged and receives the result of both decisions.

## Value Comparison

| Topic | `@Value` in dev | `@ConfigurationProperties` in prod |
| --- | --- | --- |
| Binding style | Inject each property separately | Bind the complete `logger.*` group |
| Target | Constructor parameters in `DevLogger` | Typed `ProdLoggerProperties` object |
| Best fit | One or two unrelated values | Several related application settings |
| Type safety | Conversion happens per injection point | Configuration is grouped in one typed class |
| Dependency | Spring Framework | Spring Boot configuration binder |

## Spring Versus Boot

This folder intentionally crosses the boundary slightly so the two binding styles can be compared honestly:

| Feature | Comes from |
| --- | --- |
| `@Configuration`, `@Bean`, `@Value`, `Environment`, `@Profile` | Spring Framework |
| `SpringApplication`, automatic `application*.properties/yaml` loading | Spring Boot |
| `@ConfigurationProperties` binding | Spring Boot |

Stages `s0` through `s5` start plain Spring directly. This stage uses a minimal, non-web `SpringApplication` because automatic profile-file loading and real `@ConfigurationProperties` are Boot conveniences. No XML is needed in the normal Boot configuration model.

## Why The Names Matter

Spring Boot automatically searches the classpath root for:

```text
application.properties / application.yaml
application-{active-profile}.properties / application-{active-profile}.yaml
```

That is why the shared and dev files need no explicit path. `app-prod.yaml` intentionally breaks the convention, so `application.properties` imports it with `spring.config.import`. Its internal `spring.config.activate.on-profile: prod` condition prevents it from affecting dev.

This automatic naming convention belongs to Spring Boot's config-data loader. A plain `AnnotationConfigApplicationContext` would not load these files merely because they have conventional names; it would still need explicit property-source registration.

| Name | Special meaning? |
| --- | --- |
| `application.properties` / `application.yaml` | Yes, Boot base-configuration convention. |
| `application-dev.properties` | Yes, Boot profile-file pattern; `dev` itself is only a chosen profile name. |
| `app-prod.yaml` | No, so `spring.config.import` loads it explicitly. |
| `DevEnvironmentConfig` | No, it is an ordinary Java class name. |
| `@Profile` | Yes, a Spring annotation controlling conditional bean registration. |
| `spring.profiles.active` | Yes, the property that selects active profiles. |

Avoid this inside business code:

```java
if (environment.equals("prod")) {
    // choose production implementation
}
```

Use `@Profile` to register the correct implementation before business code runs.

## Where Profiles Are Useful

Common examples:

- Fake external API client
- Local email or SMS implementation
- Local filesystem versus cloud storage
- Embedded or local database configuration
- Mock payment provider
- Development-only debugging tools
- Local substitutes for infrastructure such as Kafka or Redis

A common progression is:

```text
dev     -> fake/local implementation
test    -> fake or controlled test implementation
staging -> real integration in a production-like environment
prod    -> real integration
```

`@Profile` is a deployment and configuration tool, not a testing strategy. A fake implementation keeps local development safe and fast, but integration tests and staging must still exercise the real implementation before production.

Configuration does not have to be hardcoded in a file. Command-line arguments, JVM system properties, and OS environment variables can override file values, for example:

```bash
mvn -q compile exec:java -Dexec.mainClass=com.example.overall.s6configurationandenv.Main -Dexec.args="--spring.profiles.active=dev --logger.level=TRACE"
```

## Interview Q&A

| Question | Strong short answer |
| --- | --- |
| How does Spring Boot select a profile from the command line? | Pass `--spring.profiles.active=dev` or another profile name to `SpringApplication.run`. |
| What does `@Profile("dev")` do? | It registers that bean or configuration only when the `dev` profile is active. |
| Why not write `if (environment.equals("prod"))`? | That mixes environment selection into application logic; `@Profile` lets the container choose the implementation during startup. |
| Which file is always loaded? | `application.properties`, which contains values shared by all profiles. |
| Which file overrides the base values for dev? | `application-dev.properties` when the `dev` profile is active. |
| Can properties and YAML provide the same keys? | Yes. Both formats contribute properties to the same Spring `Environment`. |
| How is nonstandard `app-prod.yaml` loaded? | `application.properties` imports it with `spring.config.import`, and Boot parses its YAML automatically. |
| Why does `app-prod.yaml` not affect dev? | It contains `spring.config.activate.on-profile: prod`, so Boot activates that document only for prod. |
| Does plain Spring automatically load `application.properties`? | No. The conventional automatic loading shown here is Spring Boot behavior. |
| What does `@Value` do? | It resolves and injects one property at a particular field, parameter, or method. |
| How do you provide a default with `@Value`? | Use `${property:default}`, such as `${logger.format:PLAIN}`. |
| What does `@ConfigurationProperties` do? | Spring Boot binds a related prefix such as `logger.*` into one typed object. |
| Why prefer `@ConfigurationProperties` for many related values? | It groups configuration, supports relaxed binding and type conversion, and is easier to validate and test. |
| Can `@Value` and `@ConfigurationProperties` see overridden values? | Yes. Both consume the final value resolved by the ordered property sources. |
| Why use `@Bean` instead of `@Component`? | Use `@Bean` for explicit construction or classes you cannot annotate, especially third-party clients. |
| Is `DevEnvironmentConfig` a special class name? | No. Its behavior comes from `@Configuration` and `@Profile("dev")`, not its name. |
| What happens if neither dev nor prod is active? | No `LoggerService` implementation is registered, so `OrderService` creation fails. |
| Should secrets be committed in these files? | No. Use environment variables or a dedicated secret manager. |

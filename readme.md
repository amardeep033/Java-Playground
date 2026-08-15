# Java Playground

# 1_core_java

| Folder | Topics |
|---|---|
| `00_overall/` | WORA, JVM vs JRE vs JDK, compiling/running Java, Java vs Spring vs Spring Boot, Maven vs Gradle |
| `01_oops_basics/` | Classes, objects, packages, access modifiers, constructors, inheritance, encapsulation, abstraction, polymorphism, interfaces, abstract classes |
| `02_datatype/` | Primitive and non-primitive data types, wrapper classes, arrays, strings |
| `03_generics_wildcards/` | Why generics exist, generic classes, generic methods, generic interfaces, bounds, wildcards, PECS, type erasure |
| `04_exception_handling/` | Exception hierarchy, checked vs unchecked exceptions, `throw` vs `throws`, custom exceptions, null pointer handling, exception chaining, multi-catch, try-with-resources |
| `05_java8_features/` | Functional interfaces, anonymous classes vs lambdas, lambda types, method references, streams, Optional |
| `06_io_json_cli/` | File IO, NIO `Path`/`Files`, Jackson JSON parsing, CLI input/output |
| `07_mem_jvm/` | References, pass-by-value, copying/cloning, `final`, object lifecycle, memory and JVM fundamentals |
| `08_threads/` | Thread basics, `ExecutorService`, `Future`, `CompletableFuture`, `synchronized`, `volatile`, `AtomicInteger`, blocking queues, concurrent maps, locks, race conditions, deadlock |
| `09_network/` | TCP sockets, REST client/server basics, gRPC client/server basics, protobuf |
| `10_logging/` | SLF4J, Logback, logging configuration, levels, appenders, additivity |
| `11_testing_benchmarking/` | JUnit mental model, Mockito mental model, JMH benchmarking |

# 2_spring_boot

## 00_spring

| Folder | Topics |
|---|---|
| `s00withoutspring/` | Plain Java wiring before Spring |
| `s01manualconfig/` | Manual Spring configuration with `@Configuration` and `@Bean` |
| `s02autoconfig/` | Component scanning and automatic bean discovery |
| `s03diresolution/` | Dependency injection resolution, multiple implementations, `@Primary`, `@Qualifier` |
| `s04mvcarchitecture/` | Controller, service, repository, model, layered architecture |
| `s05beanlifecycle/` | Bean creation, initialization, destruction, scopes, lifecycle callbacks |
| `s06configurationandenv/` | Configuration, environment, properties, YAML, profiles |
| `s07aopandproxies/` | AOP, proxies, aspects, advice, pointcuts, self-invocation concepts |
| `s08validation/` | DTO validation with `@Valid`, validation annotations, validation error formatting |
| `s09transactions/` | `@Transactional`, transaction boundaries, propagation, rollback behavior |
| `s10events/` | Application events, publishers, listeners, `@EventListener` |

## spring_boot

| Milestone | Build                                         | Depth         |
| --------- | --------------------------------------------- | ------------- |
| `01`      | Basic CRUD REST API                           | **Deep**      |
| `02`      | DTOs + validation + global exception handling | **Deep**      |
| `03`      | PostgreSQL + JPA/Hibernate                    | **Very Deep** |
| `04`      | Relationships + query optimization + N+1      | **Very Deep** |
| `05`      | Checkout/order workflow + transactions        | **Very Deep** |
| `06`      | Concurrent operations + locking + isolation   | **Very Deep** |
| `07`      | Unit + integration + Testcontainers           | **Deep**      |
| `08`      | JWT authentication + RBAC                     | **Very Deep** |
| `09`      | Async processing + executors                  | **Deep**      |
| `10`      | Logging + metrics + tracing                   | **Deep**      |
| `11`      | gRPC internal service                         | **Medium**    |
| `12`      | OpenAPI documentation                         | **Shallow**   |

# 3_tools

| Folder | Topics |
|---|---|
| `01_postgresql/` | Tables, indexes, transactions, isolation levels, `EXPLAIN ANALYZE` |
| `02_redis/` | Caching, TTL, pub/sub, distributed locks |
| `03_kafka/` | Producers, consumers, topics, partitions, offsets, consumer groups, DLQ |
| `04_rabbitmq/` | Exchanges, queues, routing keys, acknowledgements, retries |
| `05_opentelemetry/` | Traces, metrics, logs, context propagation |
| `06_docker/` | Images, containers, volumes, networks, Docker Compose |
| `07_nginx/` | Reverse proxy, load balancing basics, TLS termination |
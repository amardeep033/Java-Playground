## Phase 1.1 — `1_core_java/`

| # | Folder | Topics |
|---|---|---|
|00|`00_overall/`|WORA, JVM vs JRE vs JDK, how to compile/run Java, Java vs Spring vs Spring Boot, Maven vs Gradle|
|01|`01_variables/`|Local, instance, static, final, why Java has no true global variables|
|01|`02_methods/`|`public static void main(String[] args)`, static vs instance, overloading, varargs|
|01|`03_classes_objects_packages/`|Class, object, `new` keyword, object lifecycle, folder structure, access modifiers, package, import, built-in vs user-defined packages|
|01|`04_constructors/`|Default, parameterized, copy, private, overloading|
|01|`05_oops/`|Inheritance, encapsulation, abstraction, polymorphism, runtime polymorphism, Interfaces, abstract class|
|02|`06_datatypes/`|Primitive vs non-primitive, wrapper types like `Integer`, arrays like `int[]`, `String`|
|02|`07_strings/`|String literal/string pool vs `new` keyword/heap, `StringBuilder` vs `StringBuffer` vs `StringTokenizer`|
|02|`08_collections/`|Why the Collections Framework exists, Collection interface vs Collections utility class, List, LinkedList, Vector, Stack, Set, Queue, Map, HashMap, ConcurrentHashMap, concurrent collections|
|03|`09_generics_wildcards/`|generics, wildcards|
|04|`10_exception_handling/`|Checked vs runtime, custom exceptions, `NullPointerException`, exception chaining, try/catch/finally vs `finalize()` — **owns this topic**|
|05|`11_java8_features/`|Lambdas, Streams, Optional, method references|

## Phase 1.2 — `1_core_java/`

| # | Folder | Topics |
|---|---|---|
|06|`12_io_json_cli/`|File IO, NIO, JSON parsing, small CLI |
|07|`13_memory_management/`|References, pass-by-value, object lifecycle, `equals` vs `==`, `hashCode`, `clone`, mutable/immutable, `final`, weak/soft references, **memory leaks** (moved from JVM)|
|07|`14_jvm/`|Class loader, bytecode, heap, stack, garbage collection/GC, metaspace, JIT, escape analysis|
|08|`15_multithreading/`|Thread, `ExecutorService`, `CompletableFuture`, Thread vs ExecutorService vs CompletableFuture comparison, `synchronized`, `volatile`|
|09|`16_networking/`|TCP, HTTP, HTTPS, sockets, serialization, Java HTTP Client, `URLConnection`, REST/gRPC client basics|
|10|`17_logging/`|SLF4J, Logback, MDC, structured/JSON logging, correlation IDs|
|11|`18_testing/`|JUnit, Mockito, benchmarking|

## Phase 2.1 — `2_spring_boot/`

| # | Folder | Topics |
|---|---|---|
|00|`00_overall/`|Spring architecture|
|01|`01_ioc_di/`|IoC, Dependency Injection — **owns this topic**|
|02|`02_beans/`|Bean lifecycle, bean scope|
|03|`03_annotations/`|`@Component`, `@Service`, `@Repository`|
|04|`04_configuration/`|Configuration, profiles, properties|
|05|`05_rest_api/`|Controllers, request mapping; links to `1_core_java/09_exception_handling` via `@ControllerAdvice`|
|06|`06_validation/`|Validation API|
|07|`07_transactions/`|`@Transactional`|

## Phase 2.2 — `2_spring_boot/`

| # | Folder | Topics |
|---|---|---|
|00|`00_overall/`|Boot architecture, standard project layout|
|01|`01_rest_crud/`|CRUD APIs|
|02|`02_database/`|Spring Data JPA, Hibernate|
|03|`03_configuration_logging/`|`application.yml`; links to `1_core_java/17_logging`|
|04|`04_testing/`|Spring/Boot integration testing only — links to `1_core_java/16_testing`|
|05|`05_grpc/`|gRPC with Spring|
|06|`06_swagger/`|OpenAPI|
|07|`07_actuator/`|Monitoring|
|08|`08_security_jwt/`|Spring Security + JWT + OAuth2 — **owns security for the whole roadmap**|

## Phase 3 — `3_tools/`

| # | Folder | Topics |
|---|---|---|
|01|`01_postgresql/`|Indexes, transactions, `EXPLAIN ANALYZE`, isolation levels — every Java backend interview expects this|
|02|`02_redis/`|Caching, pub/sub, distributed locks|
|03|`03_kafka/`|Producer, consumer, offsets, DLQ|
|04|`04_rabbitmq/`|Exchange, queue, routing|
|05|`05_opentelemetry/`|Tracing, metrics, logs (fast pass — already run this in prod on Rust)|
|06|`06_docker/`|Compose, networking, images|
|07|`07_nginx/`|Reverse proxy|

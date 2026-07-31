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
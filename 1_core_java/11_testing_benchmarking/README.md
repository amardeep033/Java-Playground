# Java Testing And Benchmarking

Goal: finish the Core Java mental model for testing and benchmarking, then move on to Spring. This is intentionally small because Spring Boot testing will revisit the same ideas with framework-specific tools.

## Study Order

| No. | File | What To Learn |
| --- | --- | --- |
| 1 | README | Unit vs integration vs E2E, folder structure, Maven test commands, tags, interview QA. |
| 2 | `src/test/java/com/example/testing/S01JunitMentalModelTest.java` | JUnit lifecycle, `@Test`, `assertThrows`, parameterized tests, assertion cheatsheet. |
| 3 | `src/test/java/com/example/testing/S02MockitoMentalModelTest.java` | Mockito mock, stub, fake, spy, `@Mock`, `@InjectMocks`, dependency injection. |
| 4 | `src/jmh/java/com/example/testing/S03JmhMentalModel.java` | JMH basics, `@Benchmark`, warm-up/JIT mental model. |

## Final Scope

| No. | Area | Keep It To |
| --- | --- | --- |
| 01 | Testing fundamentals | Unit vs integration vs E2E, test pyramid, test isolation. |
| 02 | JUnit | `@Test`, Arrange/Act/Assert, assertions, `assertThrows`, lifecycle, parameterized tests. |
| 03 | Mockito | Mock, stub, fake, spy, `@Mock`, `@InjectMocks`, `when/thenReturn`, `verify`, DI and testability. |
| 04 | Test quality | Test isolation and coverage does not equal correctness. |
| 05 | Benchmarking | `System.nanoTime` limitations, JMH, `@Benchmark`, warm-up, measurement, fork. |

## Run

```bash
mvn test
mvn test -Dgroups=unit
mvn -q -Dtest=S01JunitMentalModelTest test
mvn -q -Dtest=S02MockitoMentalModelTest test
mvn -q -DskipTests package
java -jar target/benchmarks.jar S03JmhMentalModel -wi 1 -i 1 -f 1
```

Coverage report:

```text
target/site/jacoco/index.html
```

## Folder Structure

| Path | Meaning |
| --- | --- |
| `src/test/java` | JUnit and Mockito test code. |
| `src/test/resources/mockito-extensions/org.mockito.plugins.MockMaker` | Uses Mockito subclass mock maker so this simple module does not need inline agent attachment. |
| `src/jmh/java` | JMH benchmark code. |
| `pom.xml` | Maven dependencies and plugins. |
| `target/surefire-reports` | Maven test result files after `mvn test`. |
| `target/site/jacoco` | Coverage report after tests run. |

The `S01` source is written in A/B study order, but it does not use `@TestMethodOrder` or `@Order`. For this Core Java pass, those annotations are intentionally skipped.

## Unit vs Integration vs E2E

| Type | Meaning | Core Java Example | Spring Boot Later |
| --- | --- | --- | --- |
| Unit | Test one class/method in isolation. Dependencies are real only if simple, or mocked if external/heavy. | JUnit + Mockito in this folder. | Service tests with mocked repositories/clients. |
| Integration | Test multiple real components together. | Later only as concept here. | DB integration, HTTP integration, Spring context integration, Testcontainers. |
| E2E | Test full user/API flow through the real app boundary. | Not needed in Core Java folder. | Full app flow through real HTTP/API/browser style test. |

Interview line:

```text
Unit tests are fast and isolated. Integration tests prove real components work together. E2E tests give broad confidence but are slower and more brittle.
```

## Test Pyramid

```text
          E2E
     few, slow, broad

       Integration
   medium count, real wiring

          Unit
 many, fast, narrow feedback
```

## S01 JUnit Cheatsheet

### A. Test Structure And Lifecycle

| No. | API | Mental Model |
| --- | --- | --- |
| A1 | `@BeforeAll` | Runs once before all tests in the class. |
| A2 | `@BeforeEach` | Runs before every test invocation; prepare fresh state. |
| A3 | `@Test` | Normal test method. Think Arrange -> Act -> Assert. |
| A4 | `@AfterEach` | Runs after every test invocation; cleanup. |
| A5 | `@AfterAll` | Runs once after all tests in the class. |
| A6 | `assertThrows` | Expected exception test; returns exception for message checks. |
| A7 | `@ParameterizedTest` + `@ValueSource` | Same test logic with many single values. |
| A8 | `@ParameterizedTest` + `@CsvSource` | Same test logic with multiple columns per case. |
| A9 | `@ParameterizedTest` + `@MethodSource` | Same test logic with values produced by Java code. |

### B. Assertions

| No. | Assertion | Meaning |
| --- | --- | --- |
| B1 | `assertEquals(expected, actual)` | Logical equality using `equals()`. Most common assertion. |
| B2 | `assertNotEquals(unexpected, actual)` | Values should differ. |
| B3 | `assertTrue(condition)` | Boolean condition should be true. |
| B4 | `assertFalse(condition)` | Boolean condition should be false. |
| B5 | `assertNull(value)` | Expected no value. |
| B6 | `assertSame(expected, actual)` vs `assertEquals(...)` | Same object reference vs equal value. |
| B7 | `Assertions.assertThrows(...)` vs static `assertThrows(...)` | Same API; static import is shorter. |

### `assertSame` vs `assertEquals`

| API | Checks | Example Use |
| --- | --- | --- |
| `assertEquals(a, b)` | Logical equality. For strings, records, DTOs, values. | `"AAAA"` equals `new String("AAAA")`. |
| `assertSame(a, b)` | Identity: both references point to exact same object. | Singleton, cached object, same instance returned. |

Interview line:

```text
I normally use assertEquals for business values. I use assertSame only when object identity itself is the contract.
```

### `assertThrows` Interview Point

```java
IllegalArgumentException ex = assertThrows(
        IllegalArgumentException.class,
        () -> service.callBadInput()
);
assertEquals("bad input", ex.getMessage());
```

Interview line:

```text
assertThrows proves the expected exception type and returns the exception, so I can verify the message only when the message is part of the contract.
```

## S02 Mockito Cheatsheet

| Concept | Meaning | Code Shape |
| --- | --- | --- |
| Mock | Object with no real behavior unless stubbed. | `OrderRepository repo = mock(OrderRepository.class)` |
| Stub | Controlled canned response. | `when(repo.findNameById("1")).thenReturn(Optional.of("AAAA"))` |
| Verify | Check interaction happened. | `verify(repo).save("2", "BBBB")` |
| Fake | Small working implementation, often in-memory. | `InMemoryOrderRepository` |
| Spy | Wrap real object, verify or override selected methods. | `NameFormatter formatter = spy(new NameFormatter())` |
| `@Mock` | Mockito-created dependency field. | `@Mock private OrderRepository repository;` |
| `@InjectMocks` | Create class under test and inject mocks into constructor/fields. | `@InjectMocks private OrderService orderService;` |

## Interface + Dependency Injection

```text
OrderService
    depends on OrderRepository interface

In production:
    OrderService -> Real database repository

In unit test:
    OrderService -> Mockito mock or in-memory fake
```

Interview line:

```text
Dependency injection makes testing easier because the class under test receives dependencies from outside. In a unit test I can pass a mock/fake instead of a real database, HTTP client, message broker, or payment gateway.
```

## What To Mock

| Mock It? | Example | Why |
| --- | --- | --- |
| Yes | DB repository, HTTP client, payment gateway, email sender, Kafka producer | Slow, external, nondeterministic, or side-effecting. |
| Usually no | Pure formatter, simple calculator, value object, record | Real object is fast and gives more confidence. |
| Avoid | The class being tested | Then you are testing the mock, not the behavior. |

## Test Quality

| Topic | Mental Model |
| --- | --- |
| Test isolation | Each test should pass alone and inside the full suite. |
| Shared mutable state | Avoid it unless it is reset before every test. |
| Static state | Common cause of "passes alone, fails in suite". |
| Leftover files | Clean up temp/output files after tests. |
| Time/randomness | Inject/control clock and random values when possible. |
| Threads/races | Timing-based tests can become flaky. |
| External dependencies | Mock or fake them in unit tests. |

Coverage interview line:

```text
Coverage tells me which code executed, not whether the assertions were meaningful. Coverage is useful for finding blind spots, but coverage is not correctness.
```

Tiny example:

```java
if (amount > 1000) {
    reject();
}
```

You can execute the line and still miss the important business case if the assertion is weak.

## S03 JMH Cheatsheet

| API | Meaning |
| --- | --- |
| `@Benchmark` | Method JMH measures. |
| `@Warmup` | Runs before measurement so JVM/JIT can warm up. |
| `@Measurement` | Actual measured iterations. |
| `@Fork` | Runs benchmark in a separate JVM process. |
| `@BenchmarkMode(Mode.AverageTime)` | Reports average time per operation. |
| `@OutputTimeUnit(TimeUnit.NANOSECONDS)` | Output unit. |

Unit test timing:

```java
long start = System.nanoTime();
method();
long end = System.nanoTime();
```

This can be useful for a rough smell check, but it is not a serious Java benchmark.

Interview line:

```text
System.nanoTime around one call ignores warm-up, JIT compilation, dead-code elimination, GC, CPU noise, and fork isolation. JMH exists to handle those JVM microbenchmark problems.
```

## Interview QA

| Question | Strong Answer |
| --- | --- |
| Unit vs integration vs E2E? | Unit is isolated and fast; integration checks real components together; E2E checks the full app flow and is slower/broader. |
| Why do we mock? | To isolate the class under test and control external, slow, random, or side-effecting dependencies. |
| Why does interface + DI matter for Mockito? | If the service receives an interface dependency from outside, tests can inject a Mockito mock instead of a real implementation. |
| Mock vs stub vs fake vs spy? | Mock verifies interactions, stub returns canned data, fake is a lightweight working implementation, spy wraps a real object. |
| When should you not mock? | Do not mock pure/simple logic or the class being tested. Prefer real objects when they are fast and deterministic. |
| What does `@BeforeEach` solve? | Fresh state per test, reducing hidden order dependency. |
| Why can a test pass alone but fail in suite? | Shared mutable state, order dependency, leftover files, static state, time, random data, network, or thread timing. |
| How do you test expected exceptions? | Use `assertThrows`, then check message/details if they are part of the contract. |
| `assertSame` vs `assertEquals`? | `assertEquals` checks value equality; `assertSame` checks exact same object reference. |
| How do you test 20 inputs? | Use parameterized tests: `@ValueSource`, `@CsvSource`, or `@MethodSource`. |
| Why is 100% coverage not enough? | Coverage says code ran, not that assertions were meaningful or all business cases were covered. |
| Why is JMH preferred over `System.nanoTime()`? | JMH handles warm-up, JIT effects, forks, repeated measurements, and common JVM benchmark traps. |
| Will Spring Boot testing repeat this? | Yes. Spring adds `@SpringBootTest`, MockMvc, repository tests, Testcontainers, etc., but JUnit/Mockito mental models remain the base. |

## Later In Spring Boot

| Topic | Status |
| --- | --- |
| DB integration tests | Later |
| HTTP integration tests | Later |
| Spring context integration tests | Later |
| Testcontainers | Later |
| `@SpringBootTest` | Later |
| MockMvc / `@WebMvcTest` | Later |

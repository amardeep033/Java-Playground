# Java 8 Features

Study order:

| No. | File | Topic |
| --- | --- | --- |
| 1 | `S01FunctionalInterfaceAnonymousLambda.java` | Functional interface, anonymous class, and lambda expression using a pricing-rule example. |
| 2 | `S02CustomAndLambdaTypes.java` | Custom functional interfaces and types of lambda expressions. |
| 3 | `S03MethodReferences.java` | Four types of method references. |
| 4 | `S04Streams.java` | Stream source, intermediate operations, terminal operations, and collectors. |
| 5 | `S05Optional.java` | Optional creation, reading values, and functional operations. |

## Rough Study Map

```text
Java 8
|
|-- 1. Functional Interfaces
|      |
|      |-- Contract: exactly one abstract method
|      |-- Enables lambdas and method references
|
|-- 2. Anonymous Class (Pre-Java 8)
|      |
|      |-- Old way to implement a functional interface inline
|
|-- 3. Lambda Expression (Java 8)
|      |
|      |-- New compact way to implement a functional interface
|      |
|      |-- Standard functional interfaces
|             |-- Predicate   (T -> boolean)
|             |-- Function    (T -> R)
|             |-- Consumer    (T -> void)
|             |-- Supplier    (() -> T)
|
|-- 4. Method References
|      |
|      |-- Short form of a lambda
|      |-- Static method
|      |-- Instance method of a specific object
|      |-- Instance method of an arbitrary object
|      |-- Constructor reference
|
|-- 5. Streams
|      |
|      |-- Source
|      |      |-- Collection.stream()
|      |
|      |-- Intermediate Operations (Lazy)
|      |      |-- filter()      -> Predicate
|      |      |-- map()         -> Function
|      |      |-- flatMap()
|      |      |-- sorted()
|      |      |-- distinct()
|      |      |-- limit()
|      |      |-- skip()
|      |      |-- peek()
|      |
|      |-- Terminal Operations
|      |      |-- collect()
|      |      |-- forEach()     -> Consumer
|      |      |-- reduce()
|      |      |-- count()
|      |      |-- findFirst()
|      |      |-- findAny()
|      |      |-- min()/max()
|      |      |-- any/all/noneMatch()
|      |
|      |-- Collectors
|             |-- toList()
|             |-- toSet()
|             |-- toMap()
|             |-- groupingBy()
|             |-- partitioningBy()
|             |-- joining()
|
|-- 6. Optional
       |
       |-- Creation
       |      |-- of()
       |      |-- ofNullable()
       |      |-- empty()
       |
       |-- Read Value
       |      |-- get()
       |      |-- orElse()
       |      |-- orElseGet() -> Supplier
       |      |-- orElseThrow()
       |
       |-- Functional Operations
              |-- map()       -> Function
              |-- filter()    -> Predicate
              |-- ifPresent() -> Consumer
```

## 1. Functional Interface, Anonymous Class, Lambda

Logical example: checkout pricing rules.

| Concept | Meaning |
| --- | --- |
| Functional interface | Interface with exactly one abstract method. |
| Anonymous class | Pre-Java 8 inline implementation of an interface. |
| Lambda expression | Java 8 compact implementation of a functional interface. |
| Target type | The functional interface type Java uses to understand the lambda. |

Example flow:

```java
@FunctionalInterface
interface PricingRule {
    double finalPrice(Order order);
}
```

Anonymous class:

```java
PricingRule festivalRule = new PricingRule() {
    @Override
    public double finalPrice(Order order) {
        return order.price - (order.price * 0.10);
    }
};
```

Lambda:

```java
PricingRule loyaltyRule = order -> order.price - (order.price * 0.05);
```

Why this matters:

| Before Java 8 | Java 8 |
| --- | --- |
| Anonymous classes had more boilerplate. | Lambdas focus on the actual behavior. |
| Code was harder to scan. | Small logic becomes easy to read. |
| The interface contract was still required. | The same contract is still required. |

## 2. Custom Functional Interfaces And Lambda Types

Custom functional interfaces are useful when the method name should match business meaning.

Examples:

| Interface | Abstract Method | Meaning |
| --- | --- | --- |
| `TaxCalculator` | `tax(double amount)` | Calculate tax. |
| `InvoiceCalculator` | `total(int price, int quantity)` | Calculate invoice total. |
| `RiskScorer` | `score(LoanApplication application)` | Calculate application score. |

Types of lambda expressions:

| Type | Example |
| --- | --- |
| No parameter | `() -> "INV-1001"` |
| One parameter | `amount -> amount + 100` |
| Two parameters | `(price, quantity) -> price * quantity` |
| Expression body | `app -> app.creditScore >= 700` |
| Block body | `app -> { statements; }` |

Standard functional interfaces:

| Interface | Shape | Common Use |
| --- | --- | --- |
| `Predicate<T>` | `T -> boolean` | Conditions, `filter()` |
| `Function<T, R>` | `T -> R` | Transformations, `map()` |
| `Consumer<T>` | `T -> void` | Side effects, `forEach()` |
| `Supplier<T>` | `() -> T` | Lazy/default value creation |

## 3. Method References

A method reference is a short form of a lambda that only calls an existing method.

| Type | Method Reference | Equivalent Lambda |
| --- | --- | --- |
| Static method | `Integer::sum` | `(a, b) -> Integer.sum(a, b)` |
| Instance method, specific object | `reportPrinter::print` | `message -> reportPrinter.print(message)` |
| Instance method, arbitrary object | `String::length` | `text -> text.length()` |
| Constructor reference | `ArrayList::new` | `() -> new ArrayList<>()` |

Use a method reference when it is shorter and equally readable. Keep the lambda when it contains extra logic.

## 4. Streams

A stream is a pipeline for processing data from a source.

```text
Source -> Intermediate Operations -> Terminal Operation
```

Source:

| Source | Example |
| --- | --- |
| Collection stream | `orders.stream()` |

Intermediate operations are lazy:

| Operation | Functional Interface | Purpose |
| --- | --- | --- |
| `filter()` | `Predicate` | Keep matching elements. |
| `map()` | `Function` | Transform each element. |
| `flatMap()` | Function returning stream | Flatten nested streams. |
| `sorted()` | Comparator optional | Sort elements. |
| `distinct()` | Equality | Remove duplicates. |
| `limit()` | Number | Keep first N elements. |
| `skip()` | Number | Ignore first N elements. |
| `peek()` | `Consumer` | Inspect/debug elements in a pipeline. |

Terminal operations start the pipeline:

| Operation | Purpose |
| --- | --- |
| `collect()` | Convert stream into collection, map, string, or grouped result. |
| `forEach()` | Run a `Consumer` for each element. |
| `reduce()` | Combine many values into one. |
| `count()` | Count elements. |
| `findFirst()` | Return first element as `Optional`. |
| `findAny()` | Return any element as `Optional`. |
| `min()` / `max()` | Return smallest/largest element as `Optional`. |
| `anyMatch()` | True if any element matches. |
| `allMatch()` | True if all elements match. |
| `noneMatch()` | True if no element matches. |

Collectors:

| Collector | Purpose |
| --- | --- |
| `toList()` | Collect into a `List`. |
| `toSet()` | Collect into a `Set`. |
| `toMap()` | Collect into a `Map`. |
| `groupingBy()` | Group elements by a key. |
| `partitioningBy()` | Split elements into `true` and `false` groups. |
| `joining()` | Join strings into one string. |

### Stream Reference Notes

Intermediate operations are lazy and return another stream:

| Intermediate Operation | Functional Role | Meaning |
| --- | --- | --- |
| `filter()` | `Predicate` | Keeps elements that match a condition. |
| `map()` | `Function` | Transforms each element into another value. |
| `flatMap()` | `Function` returning a stream | Expands nested values and flattens them into one stream. |
| `peek()` | `Consumer` | Performs an action while elements pass through, mostly for debugging. |
| `sorted()` | `Comparator` optional | Sorts stream elements. |
| `distinct()` | Equality | Removes duplicate values. |
| `limit()` | Count | Keeps only the first N elements. |
| `skip()` | Count | Skips the first N elements. |

Terminal operations start the pipeline and produce a final result or side effect:

| Terminal Operation | Meaning |
| --- | --- |
| `forEach()` | Performs an action on each element. |
| `collect()` | Collects elements into a final container or result. |
| `count()` | Counts elements. |
| `findFirst()` | Returns the first element as an `Optional`. |
| `findAny()` | Returns any element as an `Optional`. |
| `reduce()` | Combines all elements into one value. |
| `min()` / `max()` | Finds the smallest or largest element. |
| `anyMatch()` / `allMatch()` / `noneMatch()` | Checks whether elements match a predicate. |

Collector results:

| Collector | Purpose | Result |
| --- | --- | --- |
| `toList()` | Collect into a list. | `List<T>` |
| `toSet()` | Collect into a set. | `Set<T>` |
| `toMap()` | Collect into a map. | `Map<K,V>` |
| `groupingBy()` | Group elements by a key. | `Map<K,List<T>>` |
| `partitioningBy()` | Split by a true/false condition. | `Map<Boolean,List<T>>` |
| `joining()` | Join strings together. | `String` |

Consumer vs Collector:

| Feature | `Consumer<T>` | `Collector<T, A, R>` |
| --- | --- | --- |
| Purpose | Performs an action on one element. | Collects many elements into a final result. |
| Main method | `accept(T t)` | Uses `supplier()`, `accumulator()`, `combiner()`, and `finisher()`. |
| Returns | `void` | Produces a result such as `List`, `Set`, `Map`, or `String`. |
| Used by | `forEach()`, `peek()` | `collect()` |

Collector internals:

```text
collect()
    |
    |-- supplier()      -> Supplier<A>
    |-- accumulator()   -> BiConsumer<A, T>
    |-- combiner()      -> BinaryOperator<A>
    |-- finisher()      -> Function<A, R>
```

`reduce()` vs `collect()`:

| `reduce()` | `collect()` |
| --- | --- |
| Produces one value. | Produces a collection or container. |
| Good for sum, max, min, product. | Good for `List`, `Set`, `Map`, grouping, and joining. |
| Best for immutable-style accumulation. | Designed for mutable accumulation. |

`flatMap()` vs `reduce()`:

| `flatMap()` | `reduce()` |
| --- | --- |
| Expands and flattens nested values. | Combines values. |
| Returns another stream. | Returns one final value. |
| Element count usually stays similar or increases. | Element count becomes exactly one result. |

Laziness example:

```java
List<Integer> nums = List.of(1, 2, 3);

nums.stream()
        .filter(n -> {
            System.out.println(n);
            return n % 2 == 0;
        });
```

This pipeline does nothing because there is no terminal operation.

Do not use `map()` when the goal is filtering:

```java
nums.stream()
        .map(n -> {
            if (n % 2 == 0) {
                return n;
            }
            return null;
        });
```

This produces null values for numbers that do not match. Use `filter()` to keep or remove elements.

### Stream API Mental Model

```text
Java 8
|
|-- Lambda
|      |-- implementation of behavior
|
|-- Functional Interfaces
|      |-- Predicate
|      |-- Function
|      |-- Consumer
|      |-- Supplier
|      |-- Comparator
|
|-- Stream API
       |-- filter(Predicate)
       |-- map(Function)
       |-- sorted(Comparator)
       |-- forEach(Consumer)
       |-- reduce(...)
       |-- collect(Collector)
```

## 5. Optional

`Optional` represents a value that may be present or absent.

Creation:

| Method | Use |
| --- | --- |
| `of()` | Use when the value definitely exists. Throws if value is `null`. |
| `ofNullable()` | Use when the value may be `null`. |
| `empty()` | Create an empty `Optional`. |

Read value:

| Method | Use |
| --- | --- |
| `get()` | Gets value directly. Avoid unless presence is checked. |
| `orElse()` | Return value or eager fallback. |
| `orElseGet()` | Return value or lazy fallback from a `Supplier`. |
| `orElseThrow()` | Return value or throw an exception. |

Functional operations:

| Method | Functional Interface | Purpose |
| --- | --- | --- |
| `map()` | `Function` | Transform value if present. |
| `filter()` | `Predicate` | Keep value only if condition matches. |
| `ifPresent()` | `Consumer` | Run action only if value exists. |

## Interview Questions

| No. | Question | Answer |
| --- | --- | --- |
| Q1 | What is a functional interface? | An interface with exactly one abstract method. |
| Q2 | Can a functional interface have default or static methods? | Yes. It can have any number of default/static methods, but only one abstract method. |
| Q3 | Why use `@FunctionalInterface` if it is optional? | It protects the contract. If another abstract method is added later, compilation fails. |
| Q4 | Why does a lambda need a functional interface? | A lambda has no standalone type. Java needs the target functional interface to know parameter and return types. |
| Q5 | What must match between a lambda and the functional interface method? | Parameter count/types and return type must match the single abstract method. |
| Q6 | What is the difference between an anonymous class and a lambda? | Both can implement a functional interface, but an anonymous class creates an inline class-like implementation while a lambda focuses only on behavior. |
| Q7 | When should you create a custom functional interface instead of using `Function`? | Use a custom interface when the method name carries business meaning, such as `tax()` or `score()`. |
| Q8 | Why does `TaxCalculator.tax()` read better than `Function.apply()`? | `tax()` explains the domain operation, while `apply()` is generic. |
| Q9 | Name common lambda forms. | No parameter, one parameter, multiple parameters, expression body, and block body. |
| Q10 | Can a `Predicate` have multiple statements inside the lambda body? | Yes, if it uses a block body and finally returns a boolean. |
| Q11 | Which built-in interface represents `T -> boolean`? | `Predicate<T>`. |
| Q12 | Which built-in interface represents `T -> R`? | `Function<T, R>`. |
| Q13 | Which built-in interface represents `T -> void`? | `Consumer<T>`. |
| Q14 | Which built-in interface represents `() -> T`? | `Supplier<T>`. |
| Q15 | What is a method reference? | A short form of a lambda that only calls an existing method or constructor. |
| Q16 | When should you not use a method reference? | When the lambda contains extra logic, multiple statements, logging plus transformation, or any behavior beyond one method call. |
| Q17 | Name four method-reference forms. | Static method, instance method of a particular object, instance method of an arbitrary object, and constructor reference. |
| Q18 | What is `String::length` equivalent to? | `s -> s.length()`. The stream or caller supplies the actual String object. |
| Q19 | What is `reportPrinter::print` equivalent to? | `message -> reportPrinter.print(message)`, where `reportPrinter` is one specific object. |
| Q20 | Are streams a replacement for every loop? | No. Streams are useful when they make the data pipeline clearer; loops can be better for complex step-by-step logic. |
| Q21 | What are the three parts of a stream pipeline? | Source, intermediate operations, and terminal operation. |
| Q22 | Are intermediate stream operations lazy? | Yes. They do not execute until a terminal operation is called. |
| Q23 | What happens if a stream has only intermediate operations and no terminal operation? | Nothing executes. The pipeline is only described, not run. |
| Q24 | Can a stream be reused after a terminal operation? | No. Once a terminal operation runs, that stream is consumed. Create a new stream. |
| Q25 | Why is filtering early often preferred? | Fewer elements continue to later operations, which can reduce work and improve readability. |
| Q26 | Which functional interface does `filter()` use? | `Predicate`, because it keeps elements based on true/false. |
| Q27 | Which functional interface does `map()` use? | `Function`, because it transforms each element. |
| Q28 | Which functional interface does `peek()` use? | `Consumer`, because it performs an action and returns no result. |
| Q29 | Is `distinct()` a filter? | No. It is an intermediate operation, but it does not take a `Predicate`; it removes duplicates using equality. |
| Q30 | Are `skip()` and `limit()` filters? | No. They are intermediate operations based on element position/count, not a boolean predicate. |
| Q31 | What is the difference between `map()` and `filter()`? | `map()` transforms values; `filter()` keeps or removes values. Do not return `null` from `map()` to simulate filtering. |
| Q32 | What does `flatMap()` do? | It maps each element to a stream and then flattens those streams into one stream. |
| Q33 | Does `flatMap()` flatten all nested levels automatically? | No. It flattens one level. For deeper nesting, flatten each level intentionally. |
| Q34 | What is the difference between `flatMap()` and `reduce()`? | `flatMap()` expands/flattens into another stream; `reduce()` combines elements into one final value. |
| Q35 | What is the difference between `reduce()` and `collect()`? | `reduce()` produces one value; `collect()` produces a container/result such as `List`, `Set`, `Map`, grouping, or joined string. |
| Q36 | Why avoid external mutation inside stream pipelines? | It creates side effects, makes code harder to reason about, and can break with parallel streams. Prefer `collect()`. |
| Q37 | What is the difference between `Consumer` and `Collector`? | A `Consumer` acts on one element and returns void; a `Collector` describes how to accumulate many elements into a result. |
| Q38 | Which functional interface does `groupingBy()` use for its classifier? | `Function`, because each element is transformed into a grouping key. |
| Q39 | Which functional interface does `partitioningBy()` use? | `Predicate`, because it splits elements into `true` and `false` groups. |
| Q40 | What does `Optional.of(null)` do? | It throws `NullPointerException` immediately. |
| Q41 | When should you use `Optional.ofNullable()`? | When the value may be `null`; null becomes `Optional.empty()`. |
| Q42 | Why avoid `Optional.get()`? | It throws when the optional is empty and often recreates null-check style code. |
| Q43 | What is the difference between `orElse()` and `orElseGet()`? | `orElse()` evaluates the fallback eagerly; `orElseGet()` uses a `Supplier` and creates the fallback only when needed. |
| Q44 | Which functional interface does `orElseGet()` use? | `Supplier<T>`. |
| Q45 | What does `Optional.map()` do? | It transforms the contained value if present; if empty, it stays empty. |
| Q46 | What does `Optional.filter()` do? | It keeps the value only if the predicate is true; otherwise it returns empty. |
| Q47 | What does `Optional.ifPresent()` use? | `Consumer`, because it runs an action only when a value exists and returns nothing. |

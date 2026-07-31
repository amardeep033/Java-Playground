# Java Generics and Wildcards

Quick reference for the runnable examples in `src/`.

## Run

Run one section:

```bash
javac src/S01WhyGenerics.java
java -cp src S01WhyGenerics
```

Compile all sections into a temporary folder:

```bash
javac -d /tmp/java-generics-wildcards-demo src/*.java
java -cp /tmp/java-generics-wildcards-demo S13TypeErasure
```

## Files

| Topic | File |
| --- | --- |
| Placeholder | `src/Main.java` |
| Why Generics | `src/S01WhyGenerics.java` |
| Generic Class | `src/S02GenericClass.java` |
| Generic Method | `src/S03GenericMethod.java` |
| Multiple Generic Types | `src/S04MultipleGenericTypes.java` |
| Generic Interface | `src/S05GenericInterface.java` |
| Bounded Generic Type | `src/S06BoundedGenericType.java` |
| Multiple Bounds | `src/S07MultipleBounds.java` |
| Invariance | `src/S08Invariance.java` |
| Unbounded Wildcard | `src/S09UnboundedWildcard.java` |
| Upper Bounded Wildcard | `src/S10UpperBoundedWildcard.java` |
| Lower Bounded Wildcard | `src/S11LowerBoundedWildcard.java` |
| PECS | `src/S12Pecs.java` |
| Type Erasure | `src/S13TypeErasure.java` |
| Generic Restrictions | `src/S14GenericRestrictions.java` |
| `<T>` vs `?` | `src/S15TypeParameterVsWildcard.java` |
| `List<Object>` vs `List<?>` | `src/S16ObjectListVsWildcardList.java` |

## Cheat Sheet

| Syntax | Meaning | Read | Write |
| --- | --- | --- | --- |
| `List<T>` | Exact known type | `T` | `T` |
| `List<?>` | Unknown type | `Object` | No, except `null` |
| `List<? extends Number>` | `Number` or subclass | `Number` | No, except `null` |
| `List<? super Integer>` | `Integer` or superclass | `Object` | `Integer` |
| `<T>` | Named type parameter | Depends on usage | Depends on usage |
| `<T extends Number>` | T must be `Number` or subclass | `Number` methods available | `T` |
| `<T extends Number & Comparable<T>>` | Multiple bounds | `Number` methods and `compareTo` | `T` |

## Scenarios

| Scenario | Use |
| --- | --- |
| Store and return one exact type | `class Box<T>` |
| Method return type depends on input type | `<T> T first(List<T> items)` |
| Accept any list only for printing/logging | `List<?>` |
| Read numbers from integer/double/number lists | `List<? extends Number>` |
| Add integers into integer/number/object lists | `List<? super Integer>` |
| Copy from a source list to a destination list | `? extends` for source, `? super` for destination |
| Restrict generic type to numeric types | `<T extends Number>` |
| Restrict generic type to numeric and comparable types | `<T extends Number & Comparable<T>>` |
| Create repository-style APIs | `interface Repository<T>` |
| Need runtime generic type checks | Remember type erasure |

## Interview Questions

| No. | Question | Answer |
| --- | --- | --- |
| Q1 | Why do generics exist? | Generics provide compile-time type safety and reduce manual casting. |
| Q2 | Why is `List<Integer> list = new ArrayList<Number>();` not allowed? | Java generics are invariant. `Integer` is a `Number`, but `List<Integer>` is not a `List<Number>`. |
| Q3 | What is the difference between `<T>` and `?`? | `<T>` declares a named type parameter that can be reused or related across parameters/return values. `?` means an unknown type when you do not need to name or relate the exact type. |
| Q4 | What is the difference between `List<Object>` and `List<?>`? | `List<Object>` accepts only lists declared as `List<Object>` and allows adding any object. `List<?>` accepts many list types, such as `List<String>` and `List<Integer>`, but does not allow adding values except `null`. |
| Q5 | When should I use `? extends T`? | Use it when the collection produces values for your method to read. |
| Q6 | When should I use `? super T`? | Use it when the collection consumes values that your method writes. |
| Q7 | What is PECS? | Producer Extends, Consumer Super. |
| Q8 | How are Java generics implemented? | Java uses type erasure. Generic checks happen at compile time, then most generic type information is removed from runtime bytecode. |
| Q9 | Why is `new T()` not allowed? | `T` is erased at runtime, so Java does not know which constructor to call. |
| Q10 | Why is `new T[10]` not allowed? | Arrays need a concrete runtime component type, but `T` is erased. |
| Q11 | Why is `obj instanceof List<String>` not allowed? | `List<String>` and `List<Integer>` both erase to `List` at runtime. |
| Q12 | What runtime check is allowed instead of `obj instanceof List<String>`? | Use `obj instanceof List<?>` to check whether the object is some kind of `List`. |

## Assignment Examples

Allowed:

```java
List<Object> a = new ArrayList<Object>();
List<?> b = new ArrayList<String>();
```

Not allowed:

```java
// List<Object> c = new ArrayList<String>();
// List<Number> d = new ArrayList<Integer>();
```

## Rust Mapping

| Java | Rust mindset |
| --- | --- |
| `<T>` | `<T>` |
| `T extends Trait` | `T: Trait` |
| `<T extends A & B>` | `T: A + B` |
| `List<T>` | `Vec<T>` |
| `List<? extends Animal>` | `&[T] where T: Animal` or `&[impl Animal]` |
| `List<? super Dog>` | No direct equivalent; redesign using generics, traits, or trait objects |
| Interface | Trait |
| Interface reference | `Box<dyn Trait>` or `&dyn Trait` depending on ownership |
| `List<Animal>` | Similar mindset to `Vec<Box<dyn Animal>>` |
| Type erasure | Rust usually uses monomorphization |

Key Rust-to-Java memory hook: Java object variables already hold references, so `List<Animal>` stores references to `Animal` objects or subtype objects.

## Memorize

1. Generics provide compile-time type safety.
2. Java generics are invariant.
3. `? extends T` means producer/read.
4. `? super T` means consumer/write.
5. PECS means Producer Extends, Consumer Super.
6. Java generics use type erasure.
7. `<T>` names a type; `?` leaves the type unknown.

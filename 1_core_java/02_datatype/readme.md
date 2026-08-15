# 1. Java Data Types

## Contents

| No. | Folder | Topics |
| --- | ------ | ------ |
|02|`06_datatypes/`|Primitive vs non-primitive, wrapper types like `Integer`, arrays like `int[]`, `String`|
|02|`07_strings/`|String literal/string pool vs `new` keyword/heap, `StringBuilder` vs `StringBuffer` vs `StringTokenizer`|
|02|`08_collections/`|Why the Collections Framework exists, Collection interface vs Collections utility class, List, LinkedList, Vector, Stack, Set, Queue, Map, HashMap, ConcurrentHashMap, concurrent collections|
## Type Hierarchy

```text
Java Data Types
├── 1. Primitive Data Types (direct values, not objects, cannot be null)
│   ├── 1.1 byte (8-bit integer)
│   ├── 1.2 short (16-bit integer)
│   ├── 1.3 int (32-bit integer, common default whole number)
│   ├── 1.4 long (64-bit integer, use L suffix)
│   ├── 1.5 float (32-bit decimal, use f suffix)
│   ├── 1.6 double (64-bit decimal, common default decimal)
│   ├── 1.7 char (16-bit Unicode character)
│   └── 1.8 boolean (true/false value)
│
└── 2. Reference Types (object references, can be null, ultimately inherit from Object)
    ├── 2.1 Object (root class of all class types; arrays also extend Object)
    │
    ├── 2.2 CharSequence ℹ️ (interface for readable character sequences)
    │   ├── 2.2.1 String 💧 (immutable; literal -> String Pool; new String() -> new heap object)
    │   ├── 2.2.2 StringBuilder 💧 (mutable, not thread-safe)
    │   ├── 2.2.3 StringBuffer 💧🧵 (mutable, synchronized; use only when thread-safe mutable strings are required)
    │   └── 2.2.4 CharBuffer (mutable character buffer, niche)
    │
    ├── 2.3 Arrays ⭐❓ (objects; fixed length; indexed; covariant; Cloneable; Serializable)
    │   ├── 2.3.1 Primitive Arrays ⭐ (store primitive values directly)
    │   │   ├── 2.3.1.1 int[] (array of int values)
    │   │   ├── 2.3.1.2 long[] (array of long values)
    │   │   ├── 2.3.1.3 double[] (array of double values)
    │   │   ├── 2.3.1.4 char[] (array of char values)
    │   │   └── 2.3.1.5 boolean[] (array of boolean values)
    │   └── 2.3.2 Reference Arrays ❓ (store object references)
    │       ├── 2.3.2.1 String[] (array of String references)
    │       ├── 2.3.2.2 Integer[] (array of Integer wrapper references)
    │       └── 2.3.2.3 Object[] (array of Object references)
    │
    ├── 2.4 Wrapper Classes ❓ (object form of primitives; autoboxing/unboxing supported)
    │   ├── 2.4.1 Byte (wrapper for byte)
    │   ├── 2.4.2 Short (wrapper for short)
    │   ├── 2.4.3 Integer (wrapper for int)
    │   ├── 2.4.4 Long (wrapper for long)
    │   ├── 2.4.5 Float (wrapper for float)
    │   ├── 2.4.6 Double (wrapper for double)
    │   ├── 2.4.7 Character (wrapper for char)
    │   └── 2.4.8 Boolean (wrapper for boolean)
    │
    ├── 2.5 User-defined Types (types created by programmer)
    │   ├── 2.5.1 class
    │   ├── 2.5.2 interface
    │   ├── 2.5.3 enum
    │   └── 2.5.4 record
    │
    ├── 2.6 Iterable ℹ️ (interface, supports iterator/for-each)
    │   └── 2.6.1 Collection ℹ️ (interface, group of object references)
    │       ├── 2.6.1.1 List ℹ️ (ordered, index-based, allows duplicates)
    │       │   ├── 2.6.1.1.1 ArrayList ⭐ (growable array, fast indexed access)
    │       │   ├── 2.6.1.1.2 LinkedList 🌟 (node-based list, also implements Deque)
    │       │   └── 2.6.1.1.3 Vector ⚠️🧵 (synchronized growable array, legacy)
    │       │       └── 2.6.1.1.3.1 Stack ⚠️ (extends Vector; LIFO, synchronized, legacy)
    │       ├── 2.6.1.2 Queue ℹ️ (FIFO by default, used for processing order)
    │       │   ├── 2.6.1.2.1 PriorityQueue ⭐⭐⭐ (priority/natural order, not insertion order)
    │       │   ├── 2.6.1.2.2 ConcurrentLinkedQueue ⭐⭐⭐⭐🧵 (thread-safe non-blocking queue)
    │       │   └── 2.6.1.2.3 Deque ℹ️ (double-ended queue, add/remove both sides)
    │       │       ├── 2.6.1.2.3.1 ArrayDeque ⭐ (preferred stack/queue implementation)
    │       │       └── 2.6.1.2.3.2 LinkedList 🌟 (Deque implementation using linked nodes)
    │       └── 2.6.1.3 Set ℹ️ (unique values, no duplicates)
    │           ├── 2.6.1.3.1 HashSet ⭐ (unique values, no order guarantee)
    │           ├── 2.6.1.3.2 LinkedHashSet ⭐⭐ (unique values, insertion order)
    │           └── 2.6.1.3.3 SortedSet ℹ️ (interface, unique values in sorted order)
    │               └── 2.6.1.3.3.1 NavigableSet ℹ️ (interface, navigation methods like lower/higher)
    │                   └── 2.6.1.3.3.1.1 TreeSet ⭐⭐⭐ (sorted unique values, tree-based)
    │
    └── 2.7 Map ℹ️ (key-value pair, not child of Collection/Iterable)
        ├── 2.7.1 HashMap ⭐ (fast key lookup, no order guarantee)
        ├── 2.7.2 LinkedHashMap ⭐⭐ (fast key lookup, insertion order)
        ├── 2.7.3 Hashtable ⚠️ (legacy synchronized map; generally replaced by ConcurrentHashMap)
        ├── 2.7.4 ConcurrentHashMap ⭐⭐⭐⭐🧵 (thread-safe map, preferred for concurrency)
        └── 2.7.5 SortedMap ℹ️ (interface, keys kept sorted)
            └── 2.7.5.1 NavigableMap ℹ️ (interface, navigation methods like lowerKey/higherKey)
                └── 2.7.5.1.1 TreeMap ⭐⭐⭐ (sorted by key, tree-based)
```

## Why Collections Framework Exists

Java Collections Framework gives common interfaces and implementations for storing,
searching, sorting, iterating, and processing groups of object references.

| Problem | Framework Support |
| ------- | ----------------- |
| Need a common contract | `Collection`, `List`, `Set`, `Queue`, `Map` |
| Need ready-made data structures | `ArrayList`, `HashSet`, `HashMap`, `TreeMap` |
| Need utility algorithms | `Collections.sort()`, `Collections.reverse()`, `Collections.max()` |
| Need concurrent structures | `ConcurrentHashMap`, `ConcurrentLinkedQueue` |

## Modern Recommendations

| Need                  | Recommended         |
| --------------------- | ------------------- |
| General List          | `ArrayList`         |
| Stack                 | `ArrayDeque`        |
| Queue                 | `ArrayDeque`        |
| Unique values         | `HashSet`           |
| Ordered unique values | `LinkedHashSet`     |
| Sorted unique values  | `TreeSet`           |
| Key-value             | `HashMap`           |
| Thread-safe key-value | `ConcurrentHashMap` |
| Sorted key-value      | `TreeMap`           |

## Interview Checks

| Question                                                                      | Short Answer                                                                                                                                                                                  |
| ----------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Why can `List<int>` not compile but `List<Integer>` can?                      | Java generics work with reference types only. `int` is primitive; `Integer` is a wrapper object.                                                                                              |
| What is the difference between primitive and wrapper classes?                 | Primitive stores direct value and cannot be `null`; wrapper is an object, can be `null`, and is used in collections/generics.                                                                 |
| What is autoboxing and where can it hurt performance?                         | Automatic primitive-to-wrapper conversion, like `int` to `Integer`. It can create extra objects and slow tight loops.                                                                         |
| What is unboxing and when can it fail?                                        | Wrapper-to-primitive conversion. It throws `NullPointerException` if the wrapper is `null`.                                                                                                   |
| Why is `Integer` immutable?                                                   | Like `String`, wrapper objects are immutable so they are thread-safe, cacheable, and safe to share.                                                                                           |
| Why are wrapper classes immutable?                                            | Safe sharing, caching, thread-safety, and predictable behavior as values.                                                                                                                     |
| Why is `Integer.valueOf()` preferred over `new Integer()`?                    | `valueOf()` can reuse cached objects; `new Integer()` is deprecated and always creates a new object.                                                                                          |
| Why is `String` immutable?                                                    | For string pool reuse, thread-safety, security, and stable `hashCode` when used as a map key.                                                                                                 |
| What is the difference between `==` and `.equals()`?                          | `==` compares references for objects; `.equals()` compares logical value if the class overrides it.                                                                                           |
| Why can `new String("abc") == "abc"` be `false`?                              | `new String("abc")` creates a separate heap object; the literal points to the string pool object.                                                                                             |
| What does `intern()` do?                                                      | Returns the string pool reference for an equal string value.                                                                                                                                  |
| Why should `StringBuilder` be preferred over `String` concatenation in loops? | `String` creates new objects repeatedly; `StringBuilder` mutates the same buffer.                                                                                                             |
| Difference between `StringBuilder` and `StringBuffer`?                        | `StringBuilder` is faster and not synchronized; `StringBuffer` is synchronized and legacy.                                                                                                    |
| Why is `char` 16-bit in Java?                                                 | Java uses UTF-16 code units internally, so `char` stores one 16-bit code unit.                                                                                                                |
| Difference between `length`, `length()`, and `size()`?                        | Arrays use `length`; `String` uses `length()`; collections use `size()`.                                                                                                                      |
| Why is `HashMap` usually O(1), and when can it degrade?                       | Hashing gives average O(1); bad hashes/collisions can degrade, though Java treeifies large collision buckets.                                                                                 |
| What happens if `hashCode()` and `equals()` are inconsistent?                 | Hash-based collections like `HashMap`/`HashSet` may fail to find, remove, or de-duplicate objects correctly.                                                                                  |
| Can `HashMap` store `null` keys and values?                                   | Yes. One `null` key and multiple `null` values are allowed.                                                                                                                                   |
| Can `ConcurrentHashMap` store `null` keys or values?                          | No. It rejects `null` keys and values to avoid ambiguity in concurrent reads.                                                                                                                 |
| Difference between `HashMap`, `LinkedHashMap`, and `TreeMap`?                 | `HashMap` has no order guarantee; `LinkedHashMap` keeps insertion order; `TreeMap` sorts by key.                                                                                              |
| Difference between `HashSet`, `LinkedHashSet`, and `TreeSet`?                 | `HashSet` has no order guarantee; `LinkedHashSet` keeps insertion order; `TreeSet` sorts values.                                                                                              |
| Why is `Vector` considered legacy?                                            | It synchronizes every method and is mostly replaced by `ArrayList` or concurrent collections.                                                                                                 |
| Why is `ArrayDeque` usually better than `Stack`?                              | `Stack` extends legacy `Vector`; `ArrayDeque` is faster, cleaner, and recommended for stack behavior.                                                                                         |
| Why is `Map` not part of the `Collection` hierarchy?                          | `Collection` represents individual elements; `Map` represents key-value pairs.                                                                                                                |
| Difference between `ArrayList` and `LinkedList`?                              | `ArrayList` is better for indexed access/cache locality; `LinkedList` is node-based and also works as `Deque`.                                                                                |
| Why is random access slow in `LinkedList`?                                    | It must walk nodes from the start or end; it cannot jump directly by index.                                                                                                                   |
| Does `PriorityQueue` preserve insertion order?                                | No. It removes elements by priority/natural order or comparator.                                                                                                                              |
| What is fail-fast iterator behavior?                                          | Many collection iterators throw `ConcurrentModificationException` if structurally modified outside the iterator.                                                                              |
| Is `ConcurrentModificationException` guaranteed?                              | No. It is best-effort bug detection, not a correctness guarantee.                                                                                                                             |
| What is the default value of object references?                               | `null` for fields/array elements; local variables have no default until initialized.                                                                                                          |
| What is the default value of primitive fields?                                | Numeric types get `0`, `boolean` gets `false`, and `char` gets `'\u0000'`.                                                                                                                    |
| Why are arrays objects in Java?                                               | Arrays are reference types, have `.length`, can be assigned to `Object`, and are created on the heap.                                                                                         |
| What does array covariance mean?                                              | A `String[]` can be assigned to `Object[]`, but storing a non-String later can throw `ArrayStoreException`.                                                                                   |
| Can arrays store primitives but collections cannot?                           | Yes. `int[]` can store primitives directly; collections need wrappers like `List<Integer>`.                                                                                                   |
| What is wrapper caching?                                                      | Java caches some boxed values: `Byte`, `Short`, `Integer`, `Long` from `-128` to `127`; `Character` from `0` to `127`; and `Boolean` values. So `==` may appear true for cached boxed values. |
| Should wrapper objects be compared with `==`?                                 | Usually no. Use `.equals()` because `==` checks object identity.                                                                                                                              |
| Can primitives be passed to methods by reference?                             | No. Java is always pass-by-value. Primitive values are copied; object references are copied.                                                                                                  |

## Collection vs Collections

| Collection                       | Collections                                   | Example                                                                  |
| -------------------------------- | --------------------------------------------- | ------------------------------------------------------------------------ |
| **Interface**                    | **Utility class**                             | `Collection<String> c = new ArrayList<>();` vs `Collections.sort(list);` |
| Represents a group of objects    | Provides static helper methods                | `add()`, `remove()`, `size()` vs `sort()`, `reverse()`, `shuffle()`      |
| Parent of `List`, `Set`, `Queue` | Works on collections                          | `ArrayList`, `HashSet`, `PriorityQueue` vs `Collections.max(list)`       |
| Cannot create an object          | Cannot instantiate (all methods are `static`) | `Collection c = new ArrayList<>();` vs `Collections.sort(list);`         |
| Package: `java.util`             | Package: `java.util`                          | —                                                                        |

## StringBuilder vs StringBuffer vs StringTokenizer

| Feature     | `StringBuilder`      | `StringBuffer`          | `StringTokenizer`                       |
| ----------- | -------------------- | ----------------------- | --------------------------------------- |
| Purpose     | Build/modify strings | Build/modify strings    | Split a string into tokens              |
| Mutable     | ✅ Yes               | ✅ Yes                  | ❌ N/A                                  |
| Thread-safe | ❌ No                | ✅ Yes (`synchronized`) | ❌ No                                   |
| Performance | ✅ Faster            | ❌ Slower               | Fast, but legacy                        |
| Modern use  | ✅ Preferred         | ⚠️ Rare                 | ⚠️ Prefer `String.split()` or `Scanner` |

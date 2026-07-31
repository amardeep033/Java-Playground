# Memory Management And JVM

Study order:

| No. | File | Topic |
| --- | --- | --- |
| 1 | `src/S01ReferencesAndPassByValue.java` | Reference assignment, object mutation, reassignment, and Java pass-by-value. |
| 2 | `src/S02CopyAndClone.java` | Same reference, shallow clone, and copy constructor. |
| 3 | `src/S03FinalKeyword.java` | `final` variable, final object reference, and mutability. |
| 4 | `src/S04ObjectLifecycle.java` | Object reachability and why `p1 = null` does not destroy an object. |
| 5 | `src/MemoryJvmDemo.java` | Prints the runnable study order. |

## Rough Study Map

```text
Memory Management + JVM
|
|-- 1. References
|      |-- p2 = p1 copies the reference
|      |-- both variables point to the same object
|      |-- p2 = new Person() changes only p2
|
|-- 2. Pass By Value
|      |-- primitive value is copied
|      |-- object reference value is copied
|      |-- method can mutate the same object
|      |-- method cannot replace caller's reference
|
|-- 3. Copy / Clone
|      |-- same reference
|      |-- shallow clone
|      |-- copy constructor
|
|-- 4. final
|      |-- final primitive cannot be reassigned
|      |-- final reference cannot point to another object
|      |-- mutable object can still be changed
|
|-- 5. Object Lifecycle
|      |-- object stays alive while reachable
|      |-- p1 = null removes only one reference
|      |-- unreachable objects become eligible for GC
|
|-- 6. JVM
       |-- .java -> javac -> .class bytecode
       |-- class loader loads classes
       |-- stack + heap store runtime data
       |-- GC cleans unreachable objects
       |-- metaspace stores class metadata
       |-- JIT optimizes hot code
       |-- escape analysis may remove allocation
```

## JVM Execution Flow

```text
Java source (.java)
   |
   v
javac
   |
   v
Class code (.class bytecode)
   |
   v
Class Loader
   |
   |-- Bootstrap Class Loader
   |      |-- loads core Java classes, such as java.lang and java.util
   |
   |-- Platform Class Loader
   |      |-- loads platform/JDK modules
   |
   |-- Application Class Loader
          |-- loads application classes

Class loading notes:
   |-- uses parent delegation
   |-- uses lazy loading
   |-- starting a program checks availability, but does not load every class immediately
   |-- loading a class does not create an object

Bytecode
   |
   v
JVM executes bytecode
   |
   |-- example style: push 10, store, push 20
   |
   v
Stack + Heap
   |
   v
Garbage Collection
   |
   v
Metaspace
   |
   |-- information about classes
   |
   v
JIT Optimization
   |
   |-- Just-In-Time compiler
   |-- JVM uses runtime information to optimize the program
   |
   v
Escape Analysis
      |-- if object does not escape, allocation may be optimized
      |-- it is not simply "object moves from heap to stack"
      |-- JIT may use scalar replacement
```

Machine code is CPU dependent. Java shifts portability to the JVM, so the same bytecode can follow WORA: Write Once, Run Anywhere.

## JDK And JVM

```text
JDK (on disk)
|
|-- java.base module
|      |-- physical .class files shipped inside the JDK
|      |-- java.lang.String
|      |-- java.lang.Object
|      |-- java.util.List
|      |-- ...
|
|-- other JDK modules
       |-- java.sql
       |-- java.xml
       |-- ...

JVM (running process)
|
|-- not a normal VM with its own OS
|-- runs bytecode
|-- provides portability across machines
|
|-- Class Loader Subsystem
       |-- Bootstrap Class Loader  -> loads java.base
       |-- Platform Class Loader   -> loads other JDK modules
       |-- Application Class Loader -> loads your compiled classes
```

Example jar structure:

```text
my-app.jar
|-- META-INF/
|-- com/
|   |-- example/
|       |-- Main.class
|-- ...
```

## JVM Memory Layout

```text
JVM Memory Layout
|
|-- Stack
|      |-- local variables
|      |-- method call frames
|      |-- primitive values, such as int
|      |-- object references
|      |-- grows and shrinks as methods are called and returned
|
|-- Heap
|      |-- objects created with new, conceptually
|      |-- instance variables
|      |-- arrays
|      |-- shared across all threads
|      |-- managed by Garbage Collector
|
|-- Method Area / Metaspace
       |-- class metadata
       |-- method metadata
       |-- runtime constant pool metadata
       |-- static fields are associated with class data; exact storage is JVM-specific
```

Stack vs Heap:

| Stack | Heap |
| --- | --- |
| Per thread | Shared among threads |
| Contains stack frames | Contains objects conceptually |
| Method invocation creates frame | `new` normally creates object |
| Frame disappears when method returns | Objects remain until unreachable + GC |
| Stack overflow -> `StackOverflowError` | Exhaustion -> `OutOfMemoryError` |

## Common Memory And Runtime Problems

| Term | What it means | Typical cause | What happens? | Java? |
| --- | --- | --- | --- | --- |
| Memory leak | Memory is still reachable but no longer logically needed | Forgotten references, unbounded cache, `static` collection, `ThreadLocal` | Memory usage keeps growing | Yes |
| Dangling pointer | Pointer refers to memory that has already been freed | Manual memory management | Access can cause undefined behavior | Normal Java does not allow this |
| Use-after-free | Program accesses an object after its memory was freed | Manual memory management / unsafe code | Undefined behavior | Normal Java does not allow this |
| Null pointer / NPE | Code tries to use a `null` reference | Calling/accessing through `null` | Runtime exception | `NullPointerException` |
| Double free | Same memory is freed twice | Manual memory management bug | Undefined behavior / corruption | Normal Java does not allow this |
| Stack overflow | Call stack exceeds available space | Infinite/deep recursion, huge stack frames | `StackOverflowError` | Yes |
| Out of memory / OOM | JVM cannot satisfy a memory allocation | Heap exhaustion, huge objects, leaks | `OutOfMemoryError` | Yes |
| Deadlock | Threads wait forever for each other's locks/resources | Lock ordering / nested locks | Threads stop making progress | Yes |
| Livelock | Threads keep responding to each other but make no progress | Poor retry/coordination logic | CPU may be active, but work does not progress | Yes |
| Race condition | Result depends on timing/interleaving of concurrent operations | Unsynchronized shared state | Incorrect/unpredictable result | Yes |
| Data race | Concurrent conflicting memory accesses without proper synchronization | Shared mutable state | Undefined/unsafe behavior in languages that permit it | Java memory model prevents C/C++-style undefined data races through synchronization rules, but race conditions still exist |
| Starvation | A thread keeps getting denied the resource/CPU it needs | Unfair locking/scheduling | One thread may wait indefinitely | Yes |
| Thread leak | Threads are created and never properly terminated | Unbounded thread creation, executor misuse | Too many live threads/resources | Yes |
| Resource leak | External resource is not released | Files, sockets, DB connections, etc. | Resource exhaustion | Yes |

## Key Reminders

| Topic | Reminder |
| --- | --- |
| Reference assignment | `p2 = p1` means both variables point to the same object. |
| Reference reassignment | `p2 = new Person()` changes only `p2`. |
| `final` reference | The reference cannot change, but the object may still be mutable. |
| Garbage collection | GC removes unreachable objects, not objects that are merely unwanted. |
| Escape analysis | JIT may optimize away an allocation if the object does not escape. |

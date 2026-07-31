# Java Exception Handling

## Cheat Sheet

| Concept | Meaning |
| --- | --- |
| Exception | An object that interrupts normal program flow when something goes wrong. |
| `Throwable` | Parent class of everything that can be thrown. |
| `Error` | Serious JVM/system problem; applications usually should not recover from it. |
| `Exception` | Application-level problem that code may handle. |
| Checked exception | Compiler forces caller to catch or declare it with `throws`. |
| Unchecked exception | `RuntimeException` or `Error`; compiler does not force handling. |
| `try` | Wraps code that may throw an exception. |
| `catch` | Handles a matching exception type. |
| `finally` | Runs cleanup code after `try/catch`. |
| `throw` | Throws one exception object. |
| `throws` | Declares that a method may pass exception(s) to its caller. |
| Custom exception | Domain-specific exception class created by extending `Exception` or `RuntimeException`. |
| Exception chaining | Wrapping the original cause inside a higher-level exception. |
| `getCause()` | Returns the original wrapped exception. |
| Try-with-resources | Automatically closes resources that implement `AutoCloseable`. |
| Suppressed exception | Cleanup exception preserved when try-with-resources already has a main exception. |
| `Objects.equals(a, b)` | Null-safe equality check. |
| `Objects.requireNonNull(value, message)` | Fails fast when null is not allowed. |
| `final` | Keyword for variables, methods, and classes. |
| `finalize()` | Deprecated cleanup method; avoid it. |

## Popular Errors And Exceptions

| Type | Checked? | Common Cause |
| --- | --- | --- |
| `NullPointerException` | No | Calling a method or field through `null`. |
| `ArithmeticException` | No | Invalid arithmetic, such as division by zero. |
| `ArrayIndexOutOfBoundsException` | No | Accessing an array index outside its range. |
| `IndexOutOfBoundsException` | No | Accessing an invalid list/string index. |
| `ClassCastException` | No | Casting an object to an incompatible type. |
| `IllegalArgumentException` | No | Passing an invalid argument to a method. |
| `IllegalStateException` | No | Calling a method when the object is in the wrong state. |
| `NumberFormatException` | No | Parsing invalid text as a number. |
| `IOException` | Yes | File, stream, or network IO failure. |
| `FileNotFoundException` | Yes | File path does not exist or cannot be opened. |
| `SQLException` | Yes | Database access/query failure. |
| `InterruptedException` | Yes | A sleeping/waiting thread is interrupted. |
| `ClassNotFoundException` | Yes | Required class cannot be found at runtime. |
| `OutOfMemoryError` | Error | JVM cannot allocate more memory. |
| `StackOverflowError` | Error | Stack is exhausted, often due to uncontrolled recursion. |
| `NoClassDefFoundError` | Error | Class was available at compile time but missing at runtime. |

## Best Practices

| No. | Practice | Why |
| --- | --- | --- |
| 1 | Catch specific exceptions. | Specific catches make intent and debugging clearer. |
| 2 | Never swallow exceptions silently. | Hidden failures become harder to diagnose. |
| 3 | Log, rethrow, or handle meaningfully. | Every catch block should do something useful. |
| 4 | Preserve the original cause. | Use `new RuntimeException("message", e)` so debugging keeps the real failure. |
| 5 | Do not use exceptions for normal flow. | Validate expected conditions instead of relying on catch blocks. |
| 6 | Prefer try-with-resources. | It is cleaner and safer than manual cleanup in `finally`. |
| 7 | Use checked exceptions when recovery is possible. | Callers should be forced to handle recoverable external failures. |
| 8 | Use runtime exceptions for programming mistakes. | Bugs like invalid arguments or invalid state should be fixed in code. |
| 9 | Do not catch `Throwable`. | It also catches serious `Error` types such as `OutOfMemoryError`. |
| 10 | Never return from `finally`. | It can hide exceptions and override earlier return values. |
| 11 | Catch specific types before broad types. | Broad catches first can make later catches unreachable. |
| 12 | Avoid `finalize()`. | It is deprecated, unpredictable, and unreliable for cleanup. |

## Interview Questions

| No. | Question | Answer |
| --- | --- | --- |
| Q1 | What is an exception? | An object/event that interrupts normal program flow when something goes wrong. |
| Q2 | What is the difference between `Exception` and `Error`? | `Exception` is usually application-handled; `Error` is usually a serious JVM/system problem. |
| Q3 | What is a checked exception? | An exception the compiler forces you to catch or declare. |
| Q4 | What is an unchecked exception? | A `RuntimeException` or `Error`; the compiler does not force handling. |
| Q5 | Give examples of checked exceptions. | `IOException`, `SQLException`, `InterruptedException`, `ClassNotFoundException`. |
| Q6 | Give examples of unchecked exceptions. | `NullPointerException`, `IllegalArgumentException`, `IllegalStateException`, `ArithmeticException`. |
| Q7 | What is the difference between `throw` and `throws`? | `throw` throws an object; `throws` declares possible exceptions in a method signature. |
| Q8 | Can `throw` be used without `throws`? | Yes, for unchecked exceptions. Checked exceptions must be caught or declared. |
| Q9 | What is the purpose of `finally`? | It runs cleanup code after `try/catch`. |
| Q10 | Can `finally` run after `return`? | Yes, normally it runs before the method actually returns. |
| Q11 | When might `finally` not run? | JVM crash, forced process kill, or `System.exit()` before cleanup completes. |
| Q12 | Why should you avoid returning from `finally`? | It can suppress exceptions and override values returned from `try` or `catch`. |
| Q13 | What causes `NullPointerException`? | Calling a method or accessing a field through a reference whose value is `null`. |
| Q14 | Why is `"Java".equals(s)` safer than `s.equals("Java")`? | The literal is never `null`, so the method call itself is safe. |
| Q15 | What does `Objects.equals(null, null)` return? | `true`. |
| Q16 | When should you use `Objects.requireNonNull()`? | When null is invalid and the code should fail fast with a clear message. |
| Q17 | What is exception chaining? | Wrapping a lower-level exception inside a higher-level exception. |
| Q18 | Why preserve the cause when wrapping exceptions? | It keeps the original stack trace and root failure available. |
| Q19 | What is try-with-resources? | Syntax that automatically closes `AutoCloseable` resources. |
| Q20 | What is a suppressed exception? | An exception from cleanup that is stored when another exception is already being thrown. |
| Q21 | Why does catch ordering matter? | A broad catch before a specific catch makes the specific catch unreachable. |
| Q22 | What is multi-catch? | One catch block handling multiple exception types using `|`. |
| Q23 | What is a custom exception? | A user-defined exception class for domain-specific failures. |
| Q24 | Should business validation be checked or unchecked? | Use checked when callers can recover; use unchecked for programming mistakes or invalid API usage. |
| Q25 | What is the difference between `finally` and `finalize()`? | `finally` is a cleanup block; `finalize()` is a deprecated GC-related method. |

# Java Threads And Concurrency

## 1. Chart + Cheatsheet

| No. | Classification | Topic / API | One-liner | Code | Internal link |
| --- | --- | --- | --- | --- | --- |
| 1 | Core Concepts | Concurrency vs Parallelism | Overlapping progress vs actual same-time execution. | `S01CoreConceptsAndTasks.java` | [2.1](#21-core-concept-internals) |
| 1.1 | Core Concepts | Blocking vs Non-blocking | Caller waits vs caller can continue. | `S01CoreConceptsAndTasks.java` | [2.1](#21-core-concept-internals) |
| 1.2 | Core Concepts | Sync vs Async | Result in same flow vs result handled later. | `S01CoreConceptsAndTasks.java` | [2.1](#21-core-concept-internals) |
| 1.3 | Core Concepts | async/await vs `Future.get()` | `await` suspends async task; `get()` blocks caller thread. | `S01CoreConceptsAndTasks.java` | [2.1](#21-core-concept-internals) |
| 2 | Task Abstractions | `Runnable` | Reusable task, no return value, no checked exception directly. | `S01CoreConceptsAndTasks.java` | [2.2](#22-task-abstraction-internals) |
| 2.1 | Task Abstractions | `Callable` | Reusable task, returns value, can throw checked exception. | `S01CoreConceptsAndTasks.java` | [2.2](#22-task-abstraction-internals) |
| 2.2 | Task Abstractions | `Thread` | Low-level API; directly represents one thread of execution. | `S02ThreadBasics.java` | [2.3](#23-thread-internals) |
| 2.3 | Task Abstractions | `ExecutorService` | Higher-level API; manages worker threads/pool. | `S03ExecutorServiceAndFuture.java` | [2.5](#25-threadpoolexecutor-internals) |
| 2.4 | Task Abstractions | `CompletableFuture` | Async workflow/pipeline; avoids manual `get()` / `isDone()` chains. | `S04CompletableFuture.java` | [2.6](#26-completablefuture-internals) |
| 3 | Thread | `t.start()` | Creates new call stack and schedules `run()` on new thread. | `S02ThreadBasics.java` | [2.3](#23-thread-internals) |
| 3.1 | Thread | `t.run()` | Normal method if called directly; exists because `Thread` wraps `Runnable`. | `S02ThreadBasics.java` | [2.3](#23-thread-internals) |
| 3.2 | Thread | `Thread.sleep()` | Pauses current thread; throws `InterruptedException`. | `S02ThreadBasics.java` | [2.3](#23-thread-internals) |
| 3.3 | Thread | `t.interrupt()` | Requests cooperative cancellation, not kill. | `S02ThreadBasics.java` | [2.3](#23-thread-internals) |
| 3.4 | Thread | `t.join()` | Current thread waits for `t` to finish. | `S02ThreadBasics.java` | [2.3](#23-thread-internals) |
| 3.5 | Thread Coordination | `wait()` / `notify()` / `notifyAll()` | Monitor-based coordination; theory only here. | comments/readme | [2.4](#24-wait-notify-notifyall-and-happens-before) |
| 3.6 | Thread Coordination | happens-before | Java visibility + ordering guarantee. | comments/readme | [2.4](#24-wait-notify-notifyall-and-happens-before) |
| 4 | ExecutorService | `e.execute(Runnable)` | Fire-and-forget; no `Future`. | `S03ExecutorServiceAndFuture.java` | [2.5](#25-threadpoolexecutor-internals) |
| 4.1 | ExecutorService | `e.submit(Runnable/Callable)` | Starts task and returns `Future`. | `S03ExecutorServiceAndFuture.java` | [2.5](#25-threadpoolexecutor-internals) |
| 4.2 | ExecutorService | `e.shutdown()` | Stop accepting new tasks; submitted tasks continue. | `S03ExecutorServiceAndFuture.java` | [2.5](#25-threadpoolexecutor-internals) |
| 4.3 | ExecutorService | `e.shutdownNow()` | Attempts interruption and returns queued tasks. | `S03ExecutorServiceAndFuture.java` | [2.5](#25-threadpoolexecutor-internals) |
| 4.4 | ExecutorService | ThreadPoolExecutor internals | Core size, max size, queue, rejection policy. | comments/readme | [2.5](#25-threadpoolexecutor-internals) |
| 5 | Future | `f.get()` | Blocks caller until result/cancel/failure. | `S03ExecutorServiceAndFuture.java` | [2.5](#25-threadpoolexecutor-internals) |
| 5.1 | Future | `f.isDone()` | Checks whether task completed/cancelled/failed. | `S03ExecutorServiceAndFuture.java` | [2.5](#25-threadpoolexecutor-internals) |
| 5.2 | Future | `f.cancel(true)` | Requests cancellation and may interrupt running task. | `S03ExecutorServiceAndFuture.java` | [2.5](#25-threadpoolexecutor-internals) |
| 6 | CompletableFuture | `runAsync()` | Async task with no result. | `S04CompletableFuture.java` | [2.6](#26-completablefuture-internals) |
| 6.1 | CompletableFuture | `supplyAsync()` | Async task with result. | `S04CompletableFuture.java` | [2.6](#26-completablefuture-internals) |
| 6.2 | CompletableFuture | `thenRun()` | Next step without previous result. | `S04CompletableFuture.java` | [2.6](#26-completablefuture-internals) |
| 6.3 | CompletableFuture | `thenApply()` | Transform previous result. | `S04CompletableFuture.java` | [2.6](#26-completablefuture-internals) |
| 6.4 | CompletableFuture | `thenApplyAsync()` | Transform asynchronously, often on pool thread. | `S04CompletableFuture.java` | [2.6](#26-completablefuture-internals) |
| 6.5 | CompletableFuture | `thenAccept()` | Consume previous result and return `CompletableFuture<Void>`. | `S04CompletableFuture.java` | [2.6](#26-completablefuture-internals) |
| 6.6 | CompletableFuture | `thenCompose()` | Dependent async call; flattens nested futures. | `S04CompletableFuture.java` | [2.6](#26-completablefuture-internals) |
| 6.7 | CompletableFuture | `thenCombine()` | Combine two independent async results. | `S04CompletableFuture.java` | [2.6](#26-completablefuture-internals) |
| 6.8 | CompletableFuture | `exceptionally()` | Failure-only fallback. | `S04CompletableFuture.java` | [2.6](#26-completablefuture-internals) |
| 6.9 | CompletableFuture | `handle()` | Success or failure path in one place. | `S04CompletableFuture.java` | [2.6](#26-completablefuture-internals) |
| 7 | Synchronization | `synchronized` method | Locks `this` for instance method, `ClassName.class` for static method. | `S05SynchronizedVolatileAtomicInteger.java` | [2.7](#27-synchronization-internals) |
| 7.1 | Synchronization | `synchronized(lock)` block | Locks the exact object used in the block. | `S05SynchronizedVolatileAtomicInteger.java` | [2.7](#27-synchronization-internals) |
| 7.2 | Synchronization | `volatile` | Visibility + ordering, not atomic `count++`. | `S05SynchronizedVolatileAtomicInteger.java` | [2.7](#27-synchronization-internals) |
| 7.3 | Synchronization | `AtomicInteger` | Atomic counter/update using CAS-style operations. | `S05SynchronizedVolatileAtomicInteger.java` | [2.7](#27-synchronization-internals) |
| 7.4 | Synchronization | `ReentrantLock` | Explicit lock/unlock, `tryLock`, timed lock, fairness option. | `S07ReentrantLocks.java` | [2.8](#28-lock-internals) |
| 7.5 | Synchronization | `ReentrantReadWriteLock` | Many readers together, writer alone. | `S07ReentrantLocks.java` | [2.8](#28-lock-internals) |
| 8 | Concurrent Data Structures | `BlockingQueue` | Producer-consumer; `put` waits when full, `take` waits when empty. | `S06BlockingQueueAndConcurrentHashMap.java` | [2.9](#29-blockingqueue-and-concurrenthashmap-internals) |
| 8.1 | Concurrent Data Structures | `ConcurrentHashMap` | Safe concurrent map operations without locking whole map. | `S06BlockingQueueAndConcurrentHashMap.java` | [2.9](#29-blockingqueue-and-concurrenthashmap-internals) |
| 8.2 | Concurrent Data Structures | `merge()` | Safe read-modify-write update for map values. | `S06BlockingQueueAndConcurrentHashMap.java` | [2.9](#29-blockingqueue-and-concurrenthashmap-internals) |
| 9 | Concurrency Problems | Data race | Unsynchronized conflicting shared reads/writes. | `S08DataRaceAndRaceCondition.java` | [2.10](#210-problem-internals) |
| 9.1 | Concurrency Problems | Race condition | Correctness depends on unlucky timing/interleaving. | `S08DataRaceAndRaceCondition.java` | [2.10](#210-problem-internals) |
| 9.2 | Concurrency Problems | Deadlock | Threads wait forever for each other's resources. | `S09Deadlock.java` | [2.10](#210-problem-internals) |
| 9.3 | Concurrency Problems | Starvation | One thread keeps missing CPU/resource access. | comments/readme | [2.10](#210-problem-internals) |
| 9.4 | Concurrency Problems | Livelock | Threads are active but no useful progress happens. | comments/readme | [2.10](#210-problem-internals) |
| 9.5 | Concurrency Problems | Thread leak | Threads keep living because they are not stopped/reused. | comments/readme | [2.10](#210-problem-internals) |

## 2. Internal Theory

### 2.1 Core Concept Internals

| No. | Topic | Internal idea |
| --- | --- | --- |
| 2.1.1 | Concurrency | One core can still do concurrent work by switching between tasks. |
| 2.1.2 | Parallelism | Needs multiple cores or hardware threads to execute at the same instant. |
| 2.1.3 | Blocking | Caller thread cannot move ahead; examples: `get()`, `join()`, `take()`. |
| 2.1.4 | Non-blocking | Caller can continue and check/callback later. |
| 2.1.5 | Async vs `Future.get()` | Async starts work in background; calling `get()` brings blocking back to caller. |

### 2.2 Task Abstraction Internals

| No. | Topic | Internal idea |
| --- | --- | --- |
| 2.2.1 | `Runnable` | Represents work as data; can be passed to `Thread`, `execute`, or async APIs. |
| 2.2.2 | `Callable` | Similar to `Runnable`, but result/error is captured by `Future`. |
| 2.2.3 | `Thread` | Low-level execution object; you manage creation, start, interruption, and join. |
| 2.2.4 | `ExecutorService` | Separates task submission from thread creation/reuse. |
| 2.2.5 | `CompletableFuture` | Represents a value that may arrive later and can trigger dependent stages. |

### 2.3 Thread Internals

| No. | Topic | Internal idea |
| --- | --- | --- |
| 2.3.1 | More threads than cores | CPU/JVM/OS scheduler context switches; all runnable threads do not literally run at once. |
| 2.3.2 | `start()` | Creates a new Java thread of execution. Calling `run()` directly is just a normal method call. |
| 2.3.3 | Why `run()` exists | `Thread` implements `Runnable`; `run()` is the task body that `start()` eventually invokes on a new call stack. |
| 2.3.4 | `sleep()` | Pauses only the current thread. It does not release locks held by that thread. |
| 2.3.5 | `interrupt()` | Cooperative cancellation signal. Blocking calls like `sleep`, `wait`, `join`, `take` can throw `InterruptedException`. |
| 2.3.6 | `join()` | Current thread waits for target thread completion; target thread actions happen-before successful `join()` return. |

### 2.4 wait / notify / notifyAll And Happens-Before

`wait`, `notify`, and `notifyAll` are monitor methods from `Object`. They must be called while holding the same object's monitor.

```java
synchronized (lock) {
    while (!condition) {
        lock.wait();
    }
}
```

| No. | Topic | Internal idea |
| --- | --- | --- |
| 2.4.1 | `wait()` | Releases monitor and waits until notified/interrupted/spuriously woken. |
| 2.4.2 | `notify()` | Wakes one waiting thread, but awakened thread still needs to reacquire the monitor. |
| 2.4.3 | `notifyAll()` | Wakes all waiting threads; safer when multiple conditions may be involved. |
| 2.4.4 | Loop around `wait()` | Needed because of spurious wakeups and because condition may be false again after reacquiring lock. |

Happens-before does not mean physical wall-clock order. It means Java guarantees visibility and ordering.

| No. | Rule | Meaning |
| --- | --- | --- |
| 2.4.5 | Unlock -> later lock on same monitor | Writes before unlock are visible after later lock. |
| 2.4.6 | Volatile write -> later volatile read of same variable | Writes before volatile write are visible after volatile read. |
| 2.4.7 | Before `Thread.start()` -> inside started thread | Started thread sees properly published state. |
| 2.4.8 | Thread actions -> `Thread.join()` returns | Joining thread sees completed thread's writes. |

Interview wording:

```text
volatile provides visibility and ordering through the Java Memory Model.
A volatile write happens-before a subsequent read of the same volatile variable.
```

```text
synchronized gives mutual exclusion and visibility.
Unlocking a monitor happens-before a later lock of that same monitor.
```

### 2.5 ThreadPoolExecutor Internals

```text
submit task
|
|-- if running workers < corePoolSize
|      |-- create core worker
|
|-- else try workQueue.offer(task)
|
|-- if queue full and workers < maximumPoolSize
|      |-- create extra worker
|
|-- else
       |-- reject task using rejection policy
```

| No. | Part | Meaning |
| --- | --- | --- |
| 2.5.1 | `corePoolSize` | Normal number of workers kept for tasks. |
| 2.5.2 | `maximumPoolSize` | Upper limit when queue is full and more workers are allowed. |
| 2.5.3 | `workQueue` | Holds tasks waiting for a worker. |
| 2.5.4 | Unbounded queue | Usually prevents growth beyond core size, so max size may not matter. |
| 2.5.5 | Bounded queue | Allows back pressure and makes max size/rejection behavior visible. |
| 2.5.6 | `AbortPolicy` | Throws `RejectedExecutionException`. |
| 2.5.7 | `CallerRunsPolicy` | Caller thread runs task; slows submitter and creates back pressure. |
| 2.5.8 | `DiscardPolicy` | Drops rejected task silently. |
| 2.5.9 | `DiscardOldestPolicy` | Drops oldest queued task and retries submission. |

### 2.6 CompletableFuture Internals

| No. | Topic | Internal idea |
| --- | --- | --- |
| 2.6.1 | Default executor | Async methods without executor usually use the common `ForkJoinPool`. |
| 2.6.2 | Non-async continuation | A method like `thenApply` may run on the thread that completes the previous stage. |
| 2.6.3 | Async continuation | A method like `thenApplyAsync` schedules the continuation on an executor. |
| 2.6.4 | `thenCompose` | Avoids nested future by flattening dependent async work. |
| 2.6.5 | `thenCombine` | Waits for two independent futures, then combines both results. |
| 2.6.6 | `join()` vs `get()` | Both wait; `join()` throws unchecked `CompletionException`, `get()` throws checked exceptions. |

### 2.7 Synchronization Internals

| No. | Topic | Internal idea |
| --- | --- | --- |
| 2.7.1 | Instance synchronized method | Locks `this`. |
| 2.7.2 | Static synchronized method | Locks `ClassName.class`. |
| 2.7.3 | Synchronized block | Locks exactly the object inside `synchronized(lock)`. |
| 2.7.4 | Wrong lock object | Two correct-looking locks do not protect the same state unless every path uses the same lock. |
| 2.7.5 | `volatile` | Prevents stale reads and gives ordering, but `count++` is still read-add-write. |
| 2.7.6 | `AtomicInteger` | Uses CAS-style retry: update succeeds only if value is still what was observed. |

### 2.8 Lock Internals

| No. | Topic | Internal idea |
| --- | --- | --- |
| 2.8.1 | `ReentrantLock` | Same thread can acquire same lock again; must unlock in `finally`. |
| 2.8.2 | `tryLock()` | Allows giving up instead of waiting forever. |
| 2.8.3 | Timed lock | Allows waiting only for a bounded time. |
| 2.8.4 | Fairness option | Can prefer longest-waiting thread, usually with throughput cost. |
| 2.8.5 | `ReentrantReadWriteLock` read lock | Shared by multiple readers when no writer holds the write lock. |
| 2.8.6 | `ReentrantReadWriteLock` write lock | Exclusive; blocks readers and writers while held. |

### 2.9 BlockingQueue And ConcurrentHashMap Internals

| No. | Topic | Internal idea |
| --- | --- | --- |
| 2.9.1 | `BlockingQueue.put()` | Waits when bounded queue is full. |
| 2.9.2 | `BlockingQueue.take()` | Waits when queue is empty. |
| 2.9.3 | BlockingQueue internals | Do not say it is simply `wait/notify`; common JDK queues use locks and `Condition`s. |
| 2.9.4 | `ConcurrentHashMap` vs `synchronizedMap` | `synchronizedMap` serializes access with one wrapper lock; `ConcurrentHashMap` allows better concurrent access. |
| 2.9.5 | `merge()` | Performs atomic update for one key, useful for counters/grouping. |

### 2.10 Problem Internals

| No. | Problem | Internal shape |
| --- | --- | --- |
| 2.10.1 | Data race | Two threads access same variable, at least one writes, and no happens-before/synchronization protects it. |
| 2.10.2 | Race condition | Program correctness depends on timing. It can exist even if individual methods are synchronized but the whole check-then-act sequence is not atomic. |
| 2.10.3 | Deadlock | Mutual exclusion + hold-and-wait + no preemption + circular wait. |
| 2.10.4 | Deadlock prevention | Use consistent lock ordering, smaller critical sections, `tryLock()` timeout, or avoid nested blocking. |
| 2.10.5 | Starvation | A thread keeps getting denied the resource or CPU time it needs. |
| 2.10.6 | Livelock | Threads keep reacting to each other but no useful work completes. |
| 2.10.7 | Thread leak | Threads are created and left alive instead of being stopped, reused, or shut down. |
| 2.10.8 | Thread pool starvation deadlock | Task running in a small pool submits another task to same pool and waits for it, but no worker is free. |

## 3. Q And A

| Question | Short answer |
| --- | --- |
| What is the difference between concurrency and parallelism? | Concurrency is dealing with multiple tasks at overlapping time; parallelism is running tasks at the same instant on multiple cores. |
| Does `thread.run()` start a new thread? | No. Direct `run()` is a normal method call on the current thread. `start()` creates the new thread. |
| Why does `Thread` take `Runnable`, not `Callable`? | `Thread` has no result handle. `Callable` returns a value, so it fits `ExecutorService.submit()` and `Future`. |
| Does `sleep()` release locks? | No. A sleeping thread keeps any locks it already holds. `wait()` releases the monitor. |
| Does `interrupt()` kill a thread? | No. It sets an interruption request. The target thread must cooperate by exiting or handling `InterruptedException`. |
| Why restore interrupt flag in catch block? | Blocking methods clear the flag when throwing `InterruptedException`; restoring it lets higher-level code still see cancellation. |
| What does `join()` guarantee? | The caller waits until the target thread finishes, and the target's actions happen-before successful `join()` return. |
| `execute()` vs `submit()`? | `execute()` is fire-and-forget for `Runnable`; `submit()` returns a `Future` and can accept `Callable`. |
| Why is `Future.get()` called blocking? | The calling thread waits until result, exception, or cancellation is available. |
| What does `isDone()` really mean? | The task completed normally, failed, or was cancelled. It does not mean success only. |
| Does `cancel(true)` always stop the task? | No. It requests cancellation and may interrupt if running. If task ignores interruption, it may continue. |
| Why use `CompletableFuture` over `Future`? | It supports chaining, combining, callbacks, and error recovery without manual blocking/get polling. |
| `thenApply` vs `thenAccept`? | `thenApply` transforms and returns a value; `thenAccept` consumes value and returns `Void`. |
| `thenCompose` vs `thenCombine`? | `thenCompose` chains dependent async work; `thenCombine` merges two independent futures. |
| `exceptionally` vs `handle`? | `exceptionally` runs only on failure; `handle` runs on both success and failure. |
| `thenApply` vs `thenApplyAsync`? | `thenApply` may run on the completing thread; `thenApplyAsync` schedules async execution, usually on a pool. |
| Why is `volatile count++` wrong? | `volatile` gives visibility, but `count++` is multiple operations and is not atomic. |
| When is `volatile` good? | Simple flags like stop/shutdown/ready, where one thread writes and others need latest visibility. |
| `synchronized` vs `AtomicInteger` for counter? | `AtomicInteger` is focused and lightweight for simple counters; `synchronized` works for larger critical sections. |
| Instance synchronized vs static synchronized? | Instance method locks `this`; static method locks `ClassName.class`. They are different locks. |
| Why can mixing `synchronized` and `ReentrantLock` be wrong? | If they lock different objects, they do not protect the same shared state. |
| Why unlock `ReentrantLock` in `finally`? | If code throws before unlock, the lock can stay held and block future threads. |
| When use `ReentrantReadWriteLock`? | When reads are frequent and writes are rare, so many readers can proceed together. |
| Why use `BlockingQueue` for producer-consumer? | It handles waiting/back pressure with `put()` and `take()` instead of manual coordination. |
| Why not say BlockingQueue internally uses only `wait/notify`? | JDK implementations commonly use `Lock` and `Condition`; the exact implementation is not just raw monitor calls. |
| `ConcurrentHashMap` vs `synchronizedMap`? | `ConcurrentHashMap` allows better concurrent access; `synchronizedMap` uses a single wrapper lock. |
| Why use `map.merge(key, 1, Integer::sum)`? | It makes update for that key atomic and avoids unsafe get-then-put logic. |
| Data race vs race condition? | Data race is unsafe shared memory access. Race condition is timing-dependent wrong behavior. Race condition is broader. |
| Can race condition happen without data race? | Yes. Separate synchronized methods can still form a non-atomic check-then-act sequence. |
| What are the four deadlock conditions? | Mutual exclusion, hold-and-wait, no preemption, circular wait. |
| How do you prevent deadlock with locks? | Use consistent lock ordering, avoid nested locks, or use `tryLock()` with timeout. |
| What is thread pool starvation deadlock? | A task waits for another task submitted to the same bounded pool, but all workers are already blocked. |
| Best choice for bounded worker producer-consumer problem? | `BlockingQueue` plus fixed/bounded executor plus proper shutdown/interruption handling. |

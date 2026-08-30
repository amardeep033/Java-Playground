# 06 Async Processing + Executors

## 1. Curl

### 1.1 Start The App

```bash
./run.sh
```

### 1.2 Submit A Background Job

```bash
curl -i -X POST http://localhost:8080/api/jobs \
  -H 'Content-Type: application/json' \
  -d '{
    "description": "generate invoice summary",
    "seconds": 5
  }'
```

### 1.3 Poll Job Status

```bash
curl http://localhost:8080/api/jobs/<id>
```

### 1.4 List Jobs

```bash
curl http://localhost:8080/api/jobs
```

### 1.5 Combine Two Async Results

```bash
curl http://localhost:8080/api/jobs/report
```

## 2. Cheatsheet

### 2.1 Code Flow In This Module

| No. | Step | Code | Main Point |
| --- | --- | --- | --- |
| 1 | Submit job | `POST /api/jobs` -> `JobController.submitJob()` | Returns `202 Accepted`; final work is not complete yet. |
| 2 | Store state | `JobService.submitJob()` | Creates `JobRecord` in memory before starting async work. |
| 3 | Async execution | `AsyncJobRunner.runJob()` | Runs on `jobExecutor` because of `@Async("jobExecutor")`. |
| 4 | Poll result | `GET /api/jobs/{id}` | Caller observes `ACCEPTED`, `RUNNING`, `SUCCEEDED`, or `FAILED`. |
| 5 | Parallel combine | `GET /api/jobs/report` | Runs two independent async calls and combines with `thenCombine`. |

### 2.2 Core Java Vs Spring Wrapper

| No. | Topic | Core Java | Spring Version / In This Module |
| --- | --- | --- | --- |
| 1 | Main type | `ThreadPoolExecutor` | `ThreadPoolTaskExecutor` wraps a `ThreadPoolExecutor`. |
| 2 | Submit no-return work | `execute(Runnable)` | `@Async` method can return `void` or `CompletableFuture<Void>`. |
| 3 | Submit return work | `submit(Callable)` returns `Future` | `@Async` method can return `CompletableFuture<T>`. |
| 4 | Blocking result | `future.get()` blocks | Use `thenApply`, `thenCombine`, `exceptionally`, `handle`. |
| 5 | Shutdown | `shutdown()` stops accepting new tasks and lets submitted tasks finish. | Spring calls lifecycle shutdown for executor beans. |
| 6 | Graceful shutdown flag | Manual executor lifecycle code. | `setWaitForTasksToCompleteOnShutdown(true)`. |
| 7 | Await time | `awaitTermination(...)`. | `setAwaitTerminationSeconds(...)`. |

### 2.3 Executor Configuration

| No. | Setting | Current Code | What To Notice |
| --- | --- | --- | --- |
| 1 | Job pool | `jobExecutor` | Main background job work. |
| 2 | Notification pool | `notificationExecutor` | Fire-and-forget notification work is isolated from jobs. |
| 3 | Core size | `2` for jobs | First tasks run on core worker threads. |
| 4 | Max size | `4` for jobs | Extra threads can be created only after the queue fills. |
| 5 | Queue capacity | `10` for jobs | Tasks wait here before max threads/rejection. |
| 6 | Thread prefix | `job-exec-`, `notify-exec-` | Makes logs show which pool ran the task. |
| 7 | Rejection policy | Not explicitly set | Default is `AbortPolicy`. |

### 2.4 Task Admission Order

| No. | Condition | Executor Behavior |
| --- | --- | --- |
| 1 | Core thread available | Run task on a core thread. |
| 2 | Core threads busy and queue has space | Put task in queue. |
| 3 | Queue full and pool can grow | Create thread up to `maxPoolSize`. |
| 4 | Queue full and pool already at max | Apply `RejectedExecutionHandler`. |

### 2.5 Rejection Policies

| No. | Policy | Behavior | Use / Risk |
| --- | --- | --- | --- |
| 1 | `AbortPolicy` | Throws `RejectedExecutionException`. | Good default when overload must be visible. |
| 2 | `CallerRunsPolicy` | Caller thread runs the task. | Slows the producer and gives natural backpressure. |
| 3 | `DiscardPolicy` | Silently drops the new task. | Dangerous for important work. |
| 4 | `DiscardOldestPolicy` | Drops oldest queued task and retries new task. | Only for workloads where newer work is more valuable. |

### 2.6 Backpressure Strategies

| No. | Strategy | Behavior | When Useful |
| --- | --- | --- | --- |
| 1 | Bounded queue | Limits tasks waiting in memory; once full, admission is blocked/rejected. | Default memory protection. |
| 2 | Blocking / throttling | Producer waits until capacity becomes available. | Every task must eventually be processed. |
| 3 | Caller-runs | Producer executes the task when workers are saturated. | Simple producer slowdown. |
| 4 | Rate limiting | Restricts producer to N requests/tasks per second. | APIs, DBs, or external services with known capacity. |
| 5 | Semaphore | Allows only N operations in flight simultaneously. | Expensive resources or downstream calls. |
| 6 | Token bucket | Tokens refill at a fixed rate; each task consumes a token. | Rate plus burst control. |
| 7 | Load shedding | Intentionally rejects/drops work when overloaded. | Protecting availability is more important than processing every request. |
| 8 | Batching | Accumulates multiple items and processes them together. | DB writes, network calls, bulk processing. |
| 9 | Pull-based consumption | Consumer asks for work only when it has capacity. | Kafka-style consumers and streaming pipelines. |

### 2.7 CompletableFuture Exception Handling

| No. | Pattern | Catches Async Failure? | Use |
| --- | --- | --- | --- |
| 1 | `try { supplyAsync(...) } catch (...)` | No | Only catches submission-time errors on caller thread. |
| 2 | `.exceptionally(ex -> fallback)` | Yes | Convert failure into fallback result. |
| 3 | `.handle((result, ex) -> ...)` | Yes | Inspect success and failure in one place. |
| 4 | `CompletableFuture.failedFuture(ex)` | Yes | Return an already failed future from async code. |
| 5 | `AsyncUncaughtExceptionHandler` | Only for `void @Async` | Last-resort logging for fire-and-forget methods. |

### 2.8 Spring `@Async` Rules

| No. | Rule | Practical Consequence |
| --- | --- | --- |
| 1 | `@EnableAsync` must be present. | Otherwise `@Async` is ignored. |
| 2 | `@Async` works through a Spring proxy. | Call must come from another Spring bean; self-invocation runs like a normal method. |
| 3 | Named executor chooses pool. | `@Async("jobExecutor")` uses the job pool. |
| 4 | Async method runs on another thread. | Exceptions do not return to the original call stack. |
| 5 | Transaction context is thread-bound. | Async method does not automatically join caller transaction. |
| 6 | `void @Async` is fire-and-forget. | Use logs/metrics/`AsyncUncaughtExceptionHandler`; no caller future exists. |
| 7 | `CompletableFuture<T>` carries result/failure. | Caller can compose or handle failure without immediate blocking. |

### 2.9 Transaction + Async Pitfalls

| No. | Scenario | Risk | Safer Pattern |
| --- | --- | --- | --- |
| 1 | `@Transactional` method calls `@Async sendEmail()` | Email can run even if DB transaction later rolls back. | Publish after commit or use outbox. |
| 2 | Order commit succeeds, app crashes before async event finishes | Event/email/analytics can be lost. | Store outbox row in same transaction. |
| 3 | Async method opens DB transaction | It is separate from caller transaction. | Treat async work as a new unit of work. |
| 4 | Important external side effect uses plain fire-and-forget | No durable retry by default. | Queue/broker/outbox worker. |
| 5 | Method calls its own `@Async` method | Proxy is bypassed; method runs synchronously. | Move async method to another Spring bean. |

### 2.10 Production Async Architecture

| No. | Layer | Production Habit |
| --- | --- | --- |
| 1 | API request | Do critical synchronous validation and DB state change. |
| 2 | DB transaction | Save business data and outbox/message intent together. |
| 3 | Commit | Return response after durable state exists. |
| 4 | Worker pool | Process email, analytics, notifications, external calls. |
| 5 | Backpressure | Use bounded queues, rejection handling, and monitoring. |
| 6 | Failure handling | Retry safe operations with limits; track failures. |
| 7 | Scale | Increase workers/pools only after checking downstream capacity. |

## 3. Interview QA

| No. | Scenario / Tricky Question | Strong SDE2 Answer |
| --- | --- | --- |
| 1 | `@Transactional createOrder()` calls `@Async updateAnalytics()`. Is analytics in the same transaction? | No. Spring transaction context is generally thread-bound. `@Async` moves work to another thread, so it does not automatically participate in the caller transaction. |
| 2 | Transaction rolls back after `asyncService.sendEmail()` was called. Is email rolled back too? | No. The async side effect may already have happened. This is why external side effects should not depend on uncommitted state. |
| 3 | What is the safer production pattern for DB update + external event? | Transactional outbox: write DB change and outbox row in one transaction, then a worker publishes the event after commit. |
| 4 | Executor has `core=4`, `max=8`, `queue=100`; 500 tasks arrive. What happens? | First 4 run on core threads, up to 100 queue, then pool can grow up to 8, then remaining submissions hit the rejection policy. |
| 5 | Executor is saturated. What determines the next task behavior? | `RejectedExecutionHandler`: `AbortPolicy`, `CallerRunsPolicy`, `DiscardPolicy`, or `DiscardOldestPolicy`. |
| 6 | Queue keeps growing. What does that mean? | Producer is generating work faster than workers/downstream systems can process it. Need bounded capacity, backpressure, rejection strategy, or more capacity after measuring bottlenecks. |
| 7 | Why can 4 worker threads make progress on 100 requests that each wait on DB for 500 ms? | If work is IO-bound, threads spend much time waiting. Other requests can progress when threads become free, but only up to pool and queue limits. |
| 8 | Does `@Async` make CPU-heavy work faster? | Not automatically. CPU-bound work needs CPU cores. Async can move work off caller thread, but too many CPU tasks just queue or context-switch. |
| 9 | Does `try/catch` around `CompletableFuture.supplyAsync(...)` catch exceptions inside the task? | No. It catches submission-time errors only. Use `exceptionally`, `handle`, or observe the returned future. |
| 10 | When should `AsyncUncaughtExceptionHandler` matter? | For `void @Async` methods, because there is no returned future to carry the exception. |
| 11 | Why can self-invocation break `@Async`? | Spring applies `@Async` through a proxy. `this.someAsyncMethod()` bypasses the proxy and runs like a normal synchronous call. |
| 12 | Why use separate executors for jobs and notifications? | Isolation. Slow or overloaded notification tasks should not starve core job-processing tasks. |
| 13 | What should you check when async processing makes an API slow? | Pool size, active threads, queue depth/growth, task duration, CPU, downstream DB/API latency, rejection count, and whether caller threads are being blocked. |
| 14 | `CallerRunsPolicy` sounds bad because caller does extra work. Why use it? | It is deliberate backpressure. When the pool is full, the producer slows down by running the task itself instead of flooding the queue. |
| 15 | When is `DiscardPolicy` dangerous? | When every task matters. It silently drops work, so lost email/event/job bugs can be hard to detect. |
| 16 | `shutdown()` vs `shutdownNow()`? | `shutdown()` stops accepting new tasks and lets submitted tasks finish. `shutdownNow()` attempts to interrupt running tasks and returns tasks that did not start. |
| 17 | Why is `Future.get()` tricky in async code? | It blocks the caller thread. If used immediately after submit, the flow becomes effectively synchronous. |
| 18 | Why prefer `CompletableFuture.thenCombine(...)` for independent calls? | It lets independent calls run in parallel and builds the response after both are ready, without blocking between the calls. |
| 19 | Is `202 Accepted` the same as event-driven architecture? | No. `202 + polling/callback` is async-over-HTTP. Event-driven usually means durable broker/pub-sub with decoupled consumers. |
| 20 | What is the big executor mental chain? | Request -> task -> executor -> core threads -> queue -> max threads -> rejection handler, with backpressure and shutdown around it. |

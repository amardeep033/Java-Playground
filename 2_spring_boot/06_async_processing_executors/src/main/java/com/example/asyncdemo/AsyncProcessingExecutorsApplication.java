package com.example.asyncdemo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

// There are three separate questions:
// 1. Where does the work execute? Same thread, another thread, another process, or another service.
// 2. What does the caller do? Waits for the result (sync) or continues and receives/observes it later (async).
// 3. If a thread is waiting, is it parked by the OS (blocking) or free to do other work (non-blocking)?
// Note: async is about caller flow, not automatically about threads.
// >> In this module, Spring async uses executors, so the async work is offloaded to other threads.

// Java building blocks:
// 1. Runnable has no return value.
// 2. Callable returns a value through Future, but Future.get() blocks if the result is not ready.
// 3. CompletableFuture supports chaining/composition so callers do not have to block immediately.
// >> In this module, the example has two executor pools: one for jobs and one for notifications.

// There are three common cross-service patterns:
// 1. Request/response | caller waits | caller knows callee | REST/gRPC, orchestration (central coordinator)
// 2. Event-driven | caller does not wait | producer does not know every consumer | broker pub/sub, choreography
// 3. Middle path | first call returns 202 | status is polled or callback/webhook arrives later | async-over-HTTP

// For important external side effects like email, analytics, or publishing to another service, plain fire-and-forget async is not durable.
// If the DB commit succeeds and the app crashes before the async side effect, the event can be lost.
// Use the transactional outbox pattern when the side effect must be reliably published.
// CompletableFuture is useful when multiple independent calls, like loading order count and payment count, can run in parallel and then be combined.
// >> In this module, the job submit API uses a 202 + polling pattern, not a broker-based event-driven flow.
// >> In this module, internal parallel work uses CompletableFuture.

@EnableAsync
@SpringBootApplication
public class AsyncProcessingExecutorsApplication {

    public static void main(String[] args) {
        SpringApplication.run(AsyncProcessingExecutorsApplication.class, args);
    }
}

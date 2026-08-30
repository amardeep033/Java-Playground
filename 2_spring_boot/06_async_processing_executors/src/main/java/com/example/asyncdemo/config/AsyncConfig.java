package com.example.asyncdemo.config;

import java.util.concurrent.Executor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

// | ThreadPoolExecutor | Core Java executor with core/max threads, queue, rejection policy, and shutdown behavior. |
// | ThreadPoolTaskExecutor | Spring wrapper around ThreadPoolExecutor; convenient as a Spring bean and works with @Async. |

@Configuration
public class AsyncConfig implements AsyncConfigurer {

    private static final Logger log = LoggerFactory.getLogger(AsyncConfig.class);

    // This example keeps two separate executor pools:
    // 1. jobExecutor: main background job work.
    // 2. notificationExecutor: fire-and-forget notification work.
    // A method chooses a pool with @Async("jobExecutor") or @Async("notificationExecutor").
    // @Async is proxy-based like @Transactional, so another Spring bean must call the async method.
    // The async method runs on an executor thread; any transaction started there is separate from the caller's transaction.

    @Bean(name = "jobExecutor")
    public Executor jobExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("job-exec-");
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(10);
        // Rejection policy is not set here, so ThreadPoolExecutor.AbortPolicy is the default.
        // When the pool and queue are full, the submitted task is rejected with RejectedExecutionException.
        executor.setWaitForTasksToCompleteOnShutdown(true); // graceful shutdown
        executor.setAwaitTerminationSeconds(10);
        executor.initialize();
        return executor;
    }

    @Bean(name = "notificationExecutor")
    public Executor notificationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("notify-exec-");
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(20);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(5);
        executor.initialize();
        return executor;
    }

    // Default executor for @Async methods that do not name a pool.
    // In this project most methods use @Async("jobExecutor"), so this is mainly a safety/default setting.
    @Override
    public Executor getAsyncExecutor() {
        return jobExecutor();
    }

    // Handles exceptions from @Async methods that return void.
    // Methods returning CompletableFuture carry failures in the future instead.
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (exception, method, params) ->
                log.error("Uncaught async exception in {}", method.getName(), exception);
    }
}


// Task admission order inside ThreadPoolExecutor:
// core thread -> queue -> max thread -> rejection policy

// | Backpressure | Overall strategy for controlling overload so fast producers cannot overwhelm slower workers. |
// | Rejection policy | Executor-specific decision used only after the pool and queue cannot accept more work. |

// | AbortPolicy | Reject and throw RejectedExecutionException. | Default; caller should explicitly know the system is overloaded. |
// | CallerRunsPolicy | Calling thread executes the task. | Slows the producer naturally. |
// | DiscardPolicy | Silently discards the new task. | Dangerous for important work. |
// | DiscardOldestPolicy | Removes oldest queued task, then retries the new task. | Only when newer work is more valuable. |

// | Bounded queue | Limit tasks waiting in memory; once full, admission is blocked/rejected. | Default memory protection. |
// | Blocking / throttling | Producer waits until capacity becomes available. | Every task must eventually be processed. |
// | Caller-runs | Producer executes the task when workers are saturated. | Simple producer slowdown. |
// | Rate limiting | Restrict producer to N requests/tasks per second. | APIs, DBs, or external services with known capacity. |
// | Semaphore | Allow only N operations in flight simultaneously. | Expensive resources or downstream calls. |
// | Token bucket | Tokens refill at a fixed rate; each task consumes a token. | Rate plus burst control. |
// | Load shedding | Intentionally reject/drop work when overloaded. | Protecting availability is more important than processing every request. |
// | Batching | Accumulate multiple items and process together. | DB writes, network calls, bulk processing. |
// | Pull-based consumption | Consumer asks for work only when it has capacity. | Kafka-style consumers and streaming pipelines. |

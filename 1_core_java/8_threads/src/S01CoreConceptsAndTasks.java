import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

// Concurrency: multiple tasks make progress in overlapping time.
// Parallelism: multiple tasks literally execute at the same time on multiple cores.
// Blocking: caller waits.
// Non-blocking: caller can continue.
// Synchronous: result is handled in the same flow.
// Asynchronous: work starts now and the result is handled later.

public class S01CoreConceptsAndTasks {
    public static void main(String[] args) throws Exception {
        // 1. Runnable: reusable task logic -- does not return a value
        Runnable emailTask = () -> System.out.println("Runnable: send welcome email");
        // 1.1 run(): normal method call on the same thread
        emailTask.run();
        // 1.2 start(): runs the Runnable on a new thread
        new Thread(emailTask, "email-thread").start();

        // 2. Callable: returns a value and can throw checked exceptions
        Callable<Integer> invoiceTask = () -> {
            return 42;
        };
        // 2.1 call(): normal method call on the same thread
        Integer invoiceCount = invoiceTask.call();
        System.out.println("Callable result: " + invoiceCount);

        // ExecutorService executor2 = Executors.newSingleThreadExecutor();
        // executor2.execute(emailTask);

        // 2.2 Callable cannot be passed directly to Thread, but it can be submitted to an ExecutorService
        ExecutorService executor = Executors.newSingleThreadExecutor();
        // ExecutorService executor = Executors.newFixedThreadPool(1); // if a worker dies before shutdown, the pool can create a replacement
        // Another option is execute(), which is fire-and-forget, has no return value, and accepts Runnable
        Future<Integer> future = executor.submit(invoiceTask);
        // Here get() is blocking; the task was already submitted in the background, but caller waits for result if it is not ready
        Integer asyncResult = future.get();
        System.out.println("Async result: " + asyncResult);
        // shutdown() stops new submissions; already submitted tasks continue, but shutdown() itself does not wait
        executor.shutdown();    
    }
}

// There are three levels:
// 1. Thread - explicit low-level API, works with Runnable, common methods are start, sleep, interrupt, join
// 2. ExecutorService - higher-level pool API, supports Runnable through execute() and Runnable/Callable through submit()
// 2.1 ExecutorService submit returns Future: get, isDone, cancel
// 3. CompletableFuture - useful when manual get/isDone chaining becomes blocking and hard to compose

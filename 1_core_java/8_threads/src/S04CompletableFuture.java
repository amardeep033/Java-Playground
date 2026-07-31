import java.util.concurrent.CompletableFuture;

// Future may be returned by someone else; if we do not want manual get()/isDone() checks, CompletableFuture gives a pipeline.
public class S04CompletableFuture {
    public static void main(String[] args) {

        // CompletableFuture can use the common pool by default, or a supplied ExecutorService when we want more control.
        // ExecutorService executor = Executors.newFixedThreadPool(3);

        // -------------------------------------------------------------------
        System.out.println("Main " + Thread.currentThread().getName()); // main

        // A1. runAsync -- async work with no returned value
        // B1. thenRun -- runs next on the thread that completes the previous stage, with no result to consume
        CompletableFuture<Void> audit = CompletableFuture
                // .runAsync(() -> System.out.println("audit side effect on " + Thread.currentThread().getName()), executor)
                .runAsync(() -> System.out.println("Worker1 " + Thread.currentThread().getName()))
                .thenRun(() -> System.out.println("Worker1 " + Thread.currentThread().getName()));

        // If there is no join(), main may exit before async println runs.
        audit.join();

        // -------------------------------------------------------------------

        // A2. supplyAsync -- async work with a returned value
        CompletableFuture<Integer> orderCountFuture = CompletableFuture
                .supplyAsync(() -> fetchOrderCount(101));

        // B2. thenApply -- transforms the returned value, usually on the thread that completed the previous stage
        CompletableFuture<String> userFuture = CompletableFuture
                .supplyAsync(() -> fetchUserName(101))
                .thenApply(String::toUpperCase);

        // B3. thenCombine -- waits for two independent futures and combines their results into one value
        // B4. thenCompose -- chains to another CompletableFuture-returning function and flattens nested futures
        // B5. thenApplyAsync -- like thenApply, but schedules the continuation asynchronously instead of reusing the completing thread
        // C1. exceptionally -- fallback if an upstream stage fails, runs only on the exception path, returns replacement value
        CompletableFuture<String> summary = userFuture
                .thenCombine(orderCountFuture, (name, orderCount) -> name + " has " + orderCount + " orders")
                .thenCompose(S04CompletableFuture::saveSummaryAsync)
                .thenApplyAsync(savedMessage -> savedMessage + " on " + Thread.currentThread().getName())
                .exceptionally(ex -> "fallback summary because: " + ex.getMessage());

        // B6. thenAccept -- consumes final value as a side effect and returns CompletableFuture<Void>
        summary.thenAccept(System.out::println).join();

        // C2. handle -- runs on success or failure, receives (value, error), and lets you decide the final outcome
        String handled = CompletableFuture.<String>supplyAsync(() -> {
            throw new IllegalStateException("payment service down");
        })
                .handle((value, error) -> error == null ? value : "handled fallback")
                .join();

        System.out.println(handled);
    }

    private static String fetchUserName(int userId) {
        return "amardeep-" + userId;
    }

    private static int fetchOrderCount(int userId) {
        return 3;
    }

    private static CompletableFuture<String> saveSummaryAsync(String summary) {
        return CompletableFuture.supplyAsync(() -> "saved: " + summary);
    }
}

// thenApply vs thenApplyAsync: may reuse completing thread vs scheduled async
// thenApply vs thenAccept: transform vs consume
// thenCompose vs thenCombine: dependent async flow vs independent async results
// exceptionally vs handle: failure-only fallback vs success/failure handling

// |                          | `Thread`             | `ExecutorService`              | `CompletableFuture`                          |
// | ------------------------ | -------------------- | ------------------------------ | -------------------------------------------- |
// | Main purpose             | Run work on a thread | Manage threads + execute tasks | Compose async operations                     |
// | You manage threads?      | **Yes**              | **Executor manages them**      | Usually no                                   |
// | Thread pool              | ❌                    | ✅                              | Usually uses common pool / supplied executor |
// | Return result easily     | ❌                    | `Future`                       | `CompletableFuture`                          |
// | Chain operations         | ❌                    | Awkward                        | **✅ Excellent**                              |
// | Combine async operations | ❌                    | Awkward                        | **✅**                                        |
// | Error pipeline           | Manual               | `Future` handling              | **✅**                                        |
// | Typical abstraction      | Low-level            | Task execution                 | Async workflow                               |

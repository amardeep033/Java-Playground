import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
public class S03ExecutorServiceAndFuture {
    public static void main(String[] args) throws Exception {

        // 0. Create a pool of two worker threads.
        ExecutorService executor = Executors.newFixedThreadPool(2);

        // 1. execute() accepts Runnable and is fire-and-forget -- no Future is returned.
        executor.execute(() -> System.out.println("execute(): log analytics event"));

        // 2. submit() can accept Callable and returns Future, so the task can produce a value and is scheduled in the background.
        Future<Integer> invoiceTotal = executor.submit(() -> {
            Thread.sleep(200); // remove/comment this sleep if you want isDone to be more likely true immediately
            return 1250;
        });

        // 3. isDone() checks completion without blocking, but manual checking becomes complex, which is where CompletableFuture helps.
        System.out.println("isDone before get: " + invoiceTotal.isDone()); // maybe true depending on timing
        // 4. get() blocks the calling thread until result is available; if task throws, get() throws ExecutionException.
        System.out.println("Future.get() result: " + invoiceTotal.get());
        System.out.println("isDone after get: " + invoiceTotal.isDone()); // always true after get() returns normally

        Future<String> slowTask = executor.submit(() -> {
            try {
                Thread.sleep(5000);
                return "finished";
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                return "interrupted";
            }
        });

        // 5. cancel(true) requests cancellation and may interrupt if task is already running; it does not kill the thread.
        boolean cancelRequested = slowTask.cancel(true);
        System.out.println("cancel requested: " + cancelRequested);
        try {
            slowTask.get();
        } catch (CancellationException ex) {
            System.out.println("cancelled task: get() throws CancellationException");
        }

        // 6. shutdown() stops accepting new tasks, and already submitted tasks continue.
        executor.shutdown();
        if (!executor.awaitTermination(2, TimeUnit.SECONDS)) {
            // shutdownNow() attempts a stronger shutdown by interrupting running tasks and returning queued tasks.
            executor.shutdownNow();
        }
    }
}

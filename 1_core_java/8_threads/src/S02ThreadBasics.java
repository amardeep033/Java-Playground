// If runnable threads are more than CPU cores, the scheduler uses context switching.

public class S02ThreadBasics {
    public static void main(String[] args) throws InterruptedException {
        
        // Thread constructor takes a Runnable and a thread name.
        Thread worker1 = new Thread(() -> {
            for (int i = 1; i <= 3; i++) {
                System.out.println("worker1 step " + i + " on " + Thread.currentThread().getName());
                // sleepQuietly() handles InterruptedException internally, so no try/catch is needed here.
                sleepQuietly(150);
            }
        }, "worker1");
        // start() creates a new call stack and schedules this Runnable on a new thread.
        worker1.start();

        Thread worker2 = new Thread(() -> {
            try {
                for (int i = 1; i <= 3; i++) {
                System.out.println("worker2 step " + i + " on " + Thread.currentThread().getName());
                // Thread.sleep() throws checked InterruptedException, so try/catch is required here.
                Thread.sleep(300);
            }
            } catch (InterruptedException ex) {
                // sleep() clears the interrupt flag when it throws.
                // Restore it so higher-level code can still observe cancellation.
                Thread.currentThread().interrupt();
                System.out.println("interruptible thread was asked to stop");
            }
        }, "worker2");
        // start() creates a new call stack and schedules this Runnable on a new thread.
        worker2.start();

        // interrupt() requests cooperative cancellation so the thread can clean up; it does not kill the thread, and Thread.stop() is deprecated.
        // worker2.interrupt();

        // join() makes the current thread wait until worker2 finishes.
        worker2.join();
        // worker2.interrupt(); -- does not make sense here because worker2 is already finished
        worker1.join(); // maybe already finished, but join() is still safe
        System.out.println("main continues after join()");
    }

    private static void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}

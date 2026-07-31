public class S09Deadlock {
    private static final Object accountA = new Object();
    private static final Object accountB = new Object();

    public static void main(String[] args) throws InterruptedException {

        // Deadlock story:
        // Thread 1 locks accountA then waits for accountB.
        // Thread 2 locks accountB then waits for accountA.
        // That circular wait can block both forever.
        //
        // This runnable example uses consistent lock ordering, so it shows the fix.

        Thread transfer1 = new Thread(() -> transferWithFixedOrder("transfer1"), "transfer1");
        Thread transfer2 = new Thread(() -> transferWithFixedOrder("transfer2"), "transfer2");

        transfer1.start();
        transfer2.start();
        transfer1.join();
        transfer2.join();

        System.out.println("deadlock prevented by consistent lock ordering");
    }

    private static void transferWithFixedOrder(String transferName) {
        synchronized (accountA) {
            synchronized (accountB) {
                System.out.println(transferName + " locked accountA then accountB");
            }
        }
    }

    private static void deadlockShapeOnlyDoNotRun() {
        Thread transfer1 = new Thread(() -> {
            synchronized (accountA) {
                sleepQuietly(100);
                synchronized (accountB) {
                    System.out.println("transfer1 done");
                }
            }
        });

        Thread transfer2 = new Thread(() -> {
            synchronized (accountB) {
                sleepQuietly(100);
                synchronized (accountA) {
                    System.out.println("transfer2 done");
                }
            }
        });

        // Starting these two can deadlock because the lock order is different.
        // transfer1.start();
        // transfer2.start();
    }

    private static void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}


// Other common causes of deadlock:

// 1. Circular wait via join() -- no locks needed, but same circular-wait shape.
// Thread t1 = new Thread(() -> { t2.join(); }); // waits for t2
// Thread t2 = new Thread(() -> { t1.join(); }); // waits for t1
// Neither can finish because each waits for the other to finish first.

// 2. ReentrantLock without unlock() in a finally block.
// lock.lock();
// doSomethingThatThrows(); // exception skips the rest of the method
// lock.unlock(); // never reached -- lock stays held, and every future acquirer blocks

// 3. Thread pool exhaustion -- inner task cannot run because no worker thread is free.
// ExecutorService executor = Executors.newFixedThreadPool(1);
// executor.submit(() -> {
//     Future<?> inner = executor.submit(() -> System.out.println("inner"));
//     inner.get(); // blocks forever because the only worker is busy running THIS task
// });

// Fixes:
// - Consistent lock ordering -- works with synchronized (as shown above) or ReentrantLock.
// - tryLock() with timeout -- only ReentrantLock supports this; synchronized blocks indefinitely with no timeout escape.
// - Avoid nested blocking on the same bounded thread pool.

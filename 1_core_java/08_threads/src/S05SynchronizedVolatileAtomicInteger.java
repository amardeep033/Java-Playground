import java.util.concurrent.atomic.AtomicInteger;

public class S05SynchronizedVolatileAtomicInteger {
    // Not inside main because local variables referenced from a lambda must be final or effectively final.
    private static int normalCount = 0;
    private static int synchronizedCount = 0;
    private static final AtomicInteger atomicCount = new AtomicInteger(0);

    private static volatile boolean stopRequested = false;

    public static void main(String[] args) throws InterruptedException {

        Thread worker1 = new Thread(() -> {
            for (int i = 1; i <= 10_000; i++) {
                // 0. Normal increment without synchronization -- result may be wrong.
                normalCount++; 
                // 1. synchronized: protects a critical section -- gives both visibility and mutual exclusion.
                incrementSynchronized();
                // 2. AtomicInteger: atomic read-modify-write for counter-style state.
                atomicCount.incrementAndGet(); 
            }
        }, "worker1");

        Thread worker2 = new Thread(() -> {
            for (int i = 1; i <= 10_000; i++) {
                normalCount++;
                incrementSynchronized();
                atomicCount.incrementAndGet();
            }
        }, "worker2");

        worker1.start();
        worker2.start();
        worker1.join();
        worker2.join();

        System.out.println("normal count maybe wrong: " + normalCount);
        System.out.println("synchronized count: " + synchronizedCount);
        System.out.println("atomic count: " + atomicCount.get());

        //---------------------------------------------------------------------------------

        // 3. volatile: visibility + ordering, not atomicity; good for stop flags, bad for count++.
        Thread worker3 = new Thread(() -> {
            while (!stopRequested) {
                System.out.println("continue");
                Thread.onSpinWait();
            }
            System.out.println("stop");
        }, "backgroundWorker");

        worker3.start();
        Thread.sleep(1);
        // Changing volatile flag so worker3 can see the update.
        stopRequested = true;
        worker3.join();
    }

    // count++ is read + add + write, so two threads can clash without locking.
    // This protection can be a synchronized method or a synchronized block.
    private static synchronized void incrementSynchronized() {
        synchronizedCount++;
    }
}


//Important:
// synchronized instance method
//         ↓
//       this

// synchronized(this)
//         ↓
//       this

// static synchronized method
//         ↓
//    ClassName.class

// |                       | `volatile` | `synchronized` | `AtomicInteger` |
// | --------------------- | ---------- | -------------- | --------------- |
// | Visibility            | ✅         | ✅             | ✅              |
// | Mutual exclusion/lock | ❌         | ✅             | ❌              |
// | Atomic increment      | ❌         | ✅             | ✅              |
// | Good for simple flag  | ✅         | ✅             | ✅              |
// | Good for counter      | ❌         | ✅             | ✅              |
// | Typical overhead      | Low        | Locking        | Low             |

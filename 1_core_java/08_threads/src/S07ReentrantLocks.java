import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class S07ReentrantLocks {
    // Define explicit lock variable for wallet updates.
    private static final ReentrantLock paymentLock = new ReentrantLock();
    private static int walletBalance = 1000;

    private static final ReentrantReadWriteLock catalogLock = new ReentrantReadWriteLock();
    private static String productPrice = "phone=50000";

    public static void main(String[] args) throws InterruptedException {

        // 1. ReentrantLock: explicit lock/unlock -- mutual exclusion.
        // Compared to synchronized, it gives extra APIs like tryLock(), timed lock, fairness option.
        Thread debit1 = new Thread(() -> debitWallet(100), "debit1");
        Thread debit2 = new Thread(() -> debitWallet(200), "debit2");
        // synchronized also gives mutual exclusion, but see note below: this uses a different lock object.
        Thread debit3 = new Thread(() -> debitWallet0(100), "debit3");

        debit1.start();
        debit2.start();
        debit3.start();

        debit1.join();
        debit2.join();
        debit3.join();

        System.out.println("wallet balance: " + walletBalance);

        //------------------------------------------------------------------------

        // 2. ReentrantReadWriteLock -- read/write locking.
        // Many readers can read together, but writer needs exclusive access.
        Thread reader1 = new Thread(S07ReentrantLocks::readCatalog, "reader1");
        Thread reader2 = new Thread(S07ReentrantLocks::readCatalog, "reader2");
        Thread writer = new Thread(() -> updateCatalog("phone=48000"), "writer");

        reader1.start();
        reader2.start();
        writer.start();

        reader1.join();
        reader2.join();
        writer.join();

        readCatalog();
    }

    // synchronized uses S07ReentrantLocks.class here because this is a static synchronized method.
    private static synchronized void debitWallet0(int amount) {
        walletBalance = walletBalance - amount; // atomic only against the class lock, not against paymentLock
        System.out.println(Thread.currentThread().getName() + " debited " + amount);
    }

    // MUTEX
    private static void debitWallet(int amount) {
        // Acquire the explicit paymentLock; only code using this same lock is protected together.
        paymentLock.lock();
        try {
            walletBalance = walletBalance - amount;
            System.out.println(Thread.currentThread().getName() + " debited " + amount);
        } finally {
            // Release manually in finally, unlike synchronized which unlocks automatically.
            paymentLock.unlock();
        }
    }

    // READ LOCK
    private static void readCatalog() {
        catalogLock.readLock().lock();
        try {
            System.out.println(Thread.currentThread().getName() + " read " + productPrice);
        } finally {
            catalogLock.readLock().unlock();
        }
    }

    // WRITE LOCK
    private static void updateCatalog(String newPrice) {
        catalogLock.writeLock().lock();
        try {
            productPrice = newPrice;
            System.out.println(Thread.currentThread().getName() + " updated catalog");
        } finally {
            catalogLock.writeLock().unlock();
        }
    }
}

// Realistic fix: pick one locking mechanism for the same shared state, not both.
// Either all debit paths go through paymentLock.lock()/unlock().
// Or all debit paths go through synchronized on the same shared monitor object.
// Example monitor: private static final Object walletMonitor = new Object().
// Another option: make all debit methods static synchronized so they use the same class lock.

// |                                | `synchronized` | `ReentrantLock` |
// | ------------------------------ | -------------- | --------------- |
// | Mutual exclusion               | ✅              | ✅               |
// | Lock/unlock manually           | ❌              | ✅               |
// | Automatically unlocks          | ✅              | ❌               |
// | `tryLock()`                    | ❌              | ✅               |
// | Timed lock                     | ❌              | ✅               |
// | Interruptible lock acquisition | Limited        | ✅               |
// | Fairness option                | ❌              | ✅               |
// | Condition variables            | `wait/notify`  | `Condition`     |
// | Reentrant                      | ✅              | ✅               |
// | Simpler                        | **✅**          | More powerful   |

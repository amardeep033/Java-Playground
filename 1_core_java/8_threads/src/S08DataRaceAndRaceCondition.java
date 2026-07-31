public class S08DataRaceAndRaceCondition {
    private static int availableSeats = 1;

    public static void main(String[] args) throws InterruptedException {

        // Story: two users try to book the last seat.
        // Data race: both threads read/write availableSeats without synchronization.
        // Race condition: correctness depends on timing; both may believe booking succeeded.

        Thread user1 = new Thread(() -> bookLastSeat("user1"), "user1");
        Thread user2 = new Thread(() -> bookLastSeat("user2"), "user2");

        user1.start();
        user2.start();
        user1.join();
        user2.join();

        System.out.println("available seats after booking: " + availableSeats);
    }

    private static void bookLastSeat(String userName) {
    // private static synchronized void bookLastSeat(String userName) {
        if (availableSeats > 0) {
            sleepQuietly(100); // makes the wrong interleaving easier to observe
            availableSeats--;
            System.out.println(userName + " booked the seat");
        } else {
            System.out.println(userName + " saw no seats");
        }
    }

    private static void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}

// Race condition does NOT require a data race.
// You can have a race condition with properly synchronized individual operations if the full sequence is not atomic.
// synchronized void checkAndBook() {
//     if (availableSeats > 0) { ... } // atomic read under lock - safe individually
// }
// synchronized void decrement() {
//     availableSeats--; // atomic write under lock - safe individually
// }
// Even though each method is individually thread-safe, calling checkAndBook() then decrement() as two separate calls still has a race condition.
// Another thread can sneak in between the two calls, so the check-then-act sequence is not atomic as a whole.

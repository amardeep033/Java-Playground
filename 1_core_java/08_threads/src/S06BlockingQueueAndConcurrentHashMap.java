import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;

public class S06BlockingQueueAndConcurrentHashMap {
    public static void main(String[] args) throws InterruptedException {

        BlockingQueue<String> orders = new LinkedBlockingQueue<>(2);
        ConcurrentMap<String, Integer> processedCount = new ConcurrentHashMap<>();

        // Story: two producers create orders, and one consumer processes them.
        // BlockingQueue handles producer-consumer waiting and back pressure.
        // ConcurrentHashMap handles safe counting by producer name.

        Thread producer1 = new Thread(() -> produceOrders("producer1", orders), "producer1");
        Thread producer2 = new Thread(() -> produceOrders("producer2", orders), "producer2");

        Thread consumer = new Thread(() -> {
            try {
                int totalOrders = 6;
                for (int i = 1; i <= totalOrders; i++) {
                    String order = orders.take(); // waits if the queue is empty
                    String producerName = order.split("-order-")[0];

                    // merge() makes this map update safe even if more consumers are added later.
                    processedCount.merge(producerName, 1, Integer::sum);
                    System.out.println("consumer processed " + order);
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }, "consumer");

        producer1.start();
        producer2.start();
        consumer.start();

        producer1.join();
        producer2.join();
        consumer.join();

        System.out.println("processed count: " + processedCount);
    }

    private static void produceOrders(String producerName, BlockingQueue<String> orders) {
        try {
            for (int i = 1; i <= 3; i++) {
                String order = producerName + "-order-" + i;
                orders.put(order); // waits if the queue is full
                System.out.println(producerName + " produced " + order);
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}

// 4. Streams
//
// A stream is a readable pipeline for processing data from a source.
// Basic structure:
// source -> intermediate operation(s) -> terminal operation

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// The same logic can be written with loops and nested if conditions.
// Streams are useful when the pipeline makes the intent easier to read and change.
//
// Streams usually process elements vertically:
// one element moves through the pipeline operations before the next element is processed.
public class S04Streams {
    public static void main(String[] args) {
        List<Order> orders = Arrays.asList(
                new Order("Asha", "Laptop", "Electronics", 75000),
                new Order("Ravi", "Mouse", "Electronics", 800),
                new Order("Meera", "Book", "Books", 500),
                new Order("Asha", "Keyboard", "Electronics", 2500),
                new Order("Kiran", "Pen", "Stationery", 50)
        );

        // Pipeline 1:
        // Source: orders.stream()
        // Intermediate operations: filter(), map(), distinct(), sorted()
        // Terminal operation: collect()
        //
        // Intermediate operations are lazy; they do not run until a terminal operation is called.
        // After the terminal operation runs, the stream is consumed and cannot be reused.
        // Filtering early is common because fewer elements continue to the remaining operations.
        List<String> expensiveItems = orders.stream()
                .filter(order -> order.price > 1000)       // filter uses Predicate<Order>: Order -> boolean
                .map(order -> order.item)                  // map uses Function<Order, String>: Order -> String
                .distinct()
                .sorted()
                .collect(Collectors.toList());             // collect uses a Collector to build a List
        System.out.println("Expensive items: " + expensiveItems);

        // Pipeline 2:
        // peek() is mainly for debugging or inspecting elements while learning.
        // count() is the terminal operation, so this pipeline returns a long.
        long electronicsCount = orders.stream()
                .peek(order -> System.out.println("Checking " + order.item)) // peek uses Consumer<Order>: Order -> void
                .filter(order -> "Electronics".equals(order.category))       // filter uses Predicate<Order>
                .count();
        System.out.println("Electronics count: " + electronicsCount);

        // Pipeline 3:
        // map() extracts prices, then reduce() combines all prices into one total.
        int totalPrice = orders.stream()
                .map(order -> order.price)                 // map uses Function<Order, Integer>
                .reduce(0, Integer::sum);                  // reduce uses an accumulator: (a, b) -> result
        System.out.println("Total price: " + totalPrice);

        // Pipeline 4:
        // min() is a terminal operation and returns Optional<Order>.
        // Optional.map() then converts the selected Order into its item name.
        String cheapestItem = orders.stream()
                .min(Comparator.comparingInt(order -> order.price)) // min uses Comparator<Order>
                .map(order -> order.item)                           // Optional.map uses Function<Order, String>
                .orElse("None");
        System.out.println("Cheapest item: " + cheapestItem);

        // Pipeline 5:
        // anyMatch() is a terminal operation.
        // It answers whether at least one element satisfies the predicate.
        boolean hasPremiumOrder = orders.stream()
                .anyMatch(order -> order.price > 50000);   // anyMatch uses Predicate<Order>
        System.out.println("Has premium order? " + hasPremiumOrder);

        // Pipeline 6:
        // distinct(), skip(), and limit() are intermediate operations.
        // They are not filters because they do not use Predicate.
        List<String> selectedCustomers = orders.stream()
                .map(order -> order.customer)              // map uses Function<Order, String>
                .distinct()                                // removes duplicate customer names
                .skip(1)                                   // skips the first remaining customer
                .limit(2)                                  // keeps at most two customers
                .collect(Collectors.toList());
        System.out.println("Selected customers: " + selectedCustomers);

        // Pipeline 7:
        // flatMap() is used when each element can produce multiple values.
        // Here each order item is split into characters and all character streams are flattened into one stream.
        // flatMap() flattens one nesting level. If data is nested multiple levels deep, flatten each level intentionally.
        List<String> itemLetters = orders.stream()
                .map(order -> order.item)                  // Function<Order, String>
                .flatMap(item -> Arrays.stream(item.split(""))) // Function<String, Stream<String>>
                .distinct()
                .limit(8)
                .collect(Collectors.toList());
        System.out.println("Item letters: " + itemLetters);

        // Pipeline 8: collector groupingBy()
        // groupingBy() creates a Map where each category points to all matching orders.
        Map<String, List<Order>> byCategory = orders.stream()
                .collect(Collectors.groupingBy(order -> order.category)); // groupingBy uses Function<Order, String>
        System.out.println("Grouped keys: " + byCategory.keySet());

        // Pipeline 9: collector partitioningBy()
        // partitioningBy() splits elements into two groups: true and false.
        Map<Boolean, List<Order>> expensivePartition = orders.stream()
                .collect(Collectors.partitioningBy(order -> order.price > 1000)); // uses Predicate<Order>
        System.out.println("Partition keys: " + expensivePartition.keySet());

        // Pipeline 10: collector joining()
        // joining() combines stream elements into one String.
        String itemLine = orders.stream()
                .map(order -> order.item)                  // map uses Function<Order, String>
                .collect(Collectors.joining(", "));         // joining returns one String
        System.out.println("Items: " + itemLine);
    }

    private static class Order {
        private final String customer;
        private final String item;
        private final String category;
        private final int price;

        Order(String customer, String item, String category, int price) {
            this.customer = customer;
            this.item = item;
            this.category = category;
            this.price = price;
        }
    }
}

// This file keeps only comments that explain the demo code.
// Broader stream reference tables and common-confusion notes are kept in readme.md.

import java.util.Arrays;
import java.util.List;

public class S10UpperBoundedWildcard {
    public static void main(String[] args) {
        List<Integer> integers = Arrays.asList(10, 20, 30);
        List<Double> doubles = Arrays.asList(1.5, 2.5, 3.5);

        System.out.println("Sum of integers: " + sum(integers));
        System.out.println("Sum of doubles: " + sum(doubles));

        // List<? extends Number> numbers = new ArrayList<Integer>();
        // numbers.add(10); // Compile-time error: actual list might be List<Double>, List<Long>, etc.
    }

    // A parameter of List<Number> would not accept List<Integer> or List<Double> because generics are invariant.
    // List<? extends Number> means "a list whose element type is Number or some subclass of Number."
    // That restriction allows this method to accept List<Integer>, List<Double>, List<Long>, and similar lists.
    private static double sum(List<? extends Number> numbers) {
        // The method can read values as Number because every allowed element type extends Number.
        // It still cannot add values, because Java does not know the exact list type.
        // For example, adding Integer would be unsafe if the actual list were List<Double>.
        double total = 0;
        for (Number number : numbers) {
            total += number.doubleValue();
        }
        return total;
    }
}

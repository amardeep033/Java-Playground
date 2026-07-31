import java.util.Arrays;
import java.util.List;

public class S08Invariance {
    public static void main(String[] args) {
        List<Integer> integers = Arrays.asList(1, 2, 3);
        // List<Number> numbers = integers; // Compile-time error: List<Integer> is not List<Number>.
        // Integer is a subtype of Number, but List<Integer> is not a subtype of List<Number>.
        // If Java allowed this assignment, code using numbers could add a Double into the original List<Integer>.
        // That would break type safety when someone later reads from integers expecting only Integer values.

        System.out.println("Integer extends Number, but List<Integer> does not extend List<Number>.");
        System.out.println("Use wildcards when you want flexibility: " + sum(integers));
    }

    private static double sum(List<? extends Number> numbers) {
        double total = 0;
        for (Number number : numbers) {
            total += number.doubleValue();
        }
        return total;
    }
}

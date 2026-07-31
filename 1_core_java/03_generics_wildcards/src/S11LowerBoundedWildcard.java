import java.util.ArrayList;
import java.util.List;

public class S11LowerBoundedWildcard {
    public static void main(String[] args) {
        List<Integer> integers = new ArrayList<>();
        addIntegers(integers);
        System.out.println("List<Integer> after addIntegers: " + integers);

        List<Number> numbers = new ArrayList<>();
        addIntegers(numbers);
        System.out.println("List<Number> after addIntegers: " + numbers);

        List<Object> objects = new ArrayList<>();
        addIntegers(objects);
        System.out.println("List<Object> after addIntegers: " + objects);

        Object first = objects.get(0);
        System.out.println("Reading from ? super Integer is only guaranteed as Object: " + first);
    }

    private static void addIntegers(List<? super Integer> numbers) {
        // The parameter can be List<Integer>, List<Number>, or List<Object>.
        // In every one of those cases, adding an Integer is safe because Integer fits into all three.
        // Reading back as Integer is not safe, because a List<Object> could contain non-Integer values too.
        numbers.add(10);
        numbers.add(20);
        // Integer first = numbers.get(0); // Compile-time error: only Object is guaranteed.
    }
}

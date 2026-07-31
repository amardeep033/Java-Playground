import java.util.Arrays;
import java.util.List;

public class S09UnboundedWildcard {
    public static void main(String[] args) {
        printUnknownList(Arrays.asList(1, 2, 3));
        printUnknownList(Arrays.asList("a", "b", "c"));
    }

    // Even though String and Integer are subtypes of Object, List<String> and List<Integer> are not subtypes of List<Object>.
    // If this parameter were List<Object>, callers could pass only lists declared exactly as List<Object>.
    // List<?> means "a list of some unknown type", so it accepts List<Integer>, List<String>, and many others.
    private static void printUnknownList(List<?> items) {
        System.out.println("Printing List<?>: " + items);
        // items.add("new item"); // Compile-time error: actual element type is unknown.
        // Writing is not allowed because the compiler does not know whether this is a List<String>, List<Integer>, etc.
        // Reading is allowed, but only as Object, because every possible element type is at least Object.
    }
}

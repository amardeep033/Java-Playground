import java.util.Arrays;
import java.util.List;

public class S14GenericRestrictions {
    public static void main(String[] args) {
        // These restrictions all come from type erasure.
        // At runtime, Java usually does not know the exact generic type argument.
        System.out.println("These lines are intentionally comments because they do not compile:");
        System.out.println("1. new T() - T is erased, so Java does not know which constructor to call.");
        System.out.println("2. new T[10] - Java arrays need a real runtime component type.");
        System.out.println("3. obj instanceof List<String> - List<String> is just List at runtime.");

        // Java cannot create T directly because it does not know what T is at runtime.
        // T value = new T(); // Compile-time error.

        // Java arrays store their component type at runtime, but T has been erased.
        // T[] values = new T[10]; // Compile-time error.

        Object obj = Arrays.asList("Java", "Rust");

        // Java cannot check List<String> specifically because List<String> and List<Integer> both erase to List.
        // if (obj instanceof List<String>) { } // Compile-time error.

        // This is allowed because List<?> only checks that the object is some kind of List.
        if (obj instanceof List<?>) {
            System.out.println("Allowed runtime check: obj instanceof List<?>");
        }
    }
}

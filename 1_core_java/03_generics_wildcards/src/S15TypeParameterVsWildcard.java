import java.util.Arrays;
import java.util.List;

public class S15TypeParameterVsWildcard {
    public static void main(String[] args) {
        List<String> words = Arrays.asList("java", "generics");

        // first() uses <T> because its return type is connected to the list element type.
        // Since words is List<String>, T becomes String and the return value is String.
        String first = first(words);
        System.out.println("<T> names the element type, so first returns String: " + first);

        // printUnknownList() uses ? because it does not need to name the element type.
        // It only prints the list, so accepting an unknown element type is enough.
        printUnknownList(words);
        System.out.println("? accepts the list without naming its element type.");
    }

    // Use <T> when the method needs to remember the exact type.
    // Here the method receives List<T> and returns T, so the same type appears in two places.
    private static <T> T first(List<T> items) {
        return items.get(0);
    }

    // Use ? when the method only needs "some list", without relating the element type to anything else.
    private static void printUnknownList(List<?> items) {
        System.out.println("Printing List<?>: " + items);
        // items.add("new item"); // Compile-time error: actual element type is unknown.
    }
}

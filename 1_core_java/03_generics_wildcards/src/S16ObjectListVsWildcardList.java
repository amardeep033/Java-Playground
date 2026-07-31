import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class S16ObjectListVsWildcardList {
    public static void main(String[] args) {
        // List<Object> means the list is explicitly a list of Object.
        // Because every Java object extends Object, this list can store mixed object types.
        List<Object> objects = new ArrayList<>();
        objects.add("Java");
        objects.add(42);
        objects.add(3.14);
        printObjectList(objects);

        List<String> strings = Arrays.asList("one", "two");
        List<Integer> integers = Arrays.asList(1, 2);

        // List<?> is more flexible as a parameter.
        // It accepts List<String> and List<Integer> because it means "some unknown element type."
        printUnknownList(strings);
        printUnknownList(integers);

        // printObjectList(strings); // Compile-time error: List<String> is not List<Object>.
        // A List<String> is not a List<Object> because generics are invariant.
    }

    // This method accepts only List<Object>.
    // It can add any object because the element type is exactly Object.
    private static void printObjectList(List<Object> items) {
        System.out.println("Printing List<Object>: " + items);
        items.add(new User(202, "Nina"));
        System.out.println("List<Object> can accept any object: " + items);
    }

    // This method accepts List<String>, List<Integer>, List<Object>, and more.
    // It cannot add a new element because the actual element type is unknown.
    private static void printUnknownList(List<?> items) {
        System.out.println("Printing List<?>: " + items);
        // items.add("new item"); // Compile-time error: actual element type is unknown.
    }

    // A custom object helps show that List<Object> can store normal domain objects too.
    static class User {
        private final int id;
        private final String name;

        User(int id, String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public String toString() {
            return "User{id=" + id + ", name='" + name + "'}";
        }
    }
}

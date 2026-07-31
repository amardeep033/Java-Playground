import java.util.ArrayList;
import java.util.List;

public class S01WhyGenerics {
    public static void main(String[] args) {
        withoutGenerics();
        withGenerics();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void withoutGenerics() {
        // This is allowed because raw types come from old Java before generics existed.
        // The problem is that the compiler cannot check what type of values the list contains.
        List rawList = new ArrayList();
        rawList.add("Hello");
        rawList.add(10);

        String first = (String) rawList.get(0);
        System.out.println("Raw list first value after cast: " + first);

        try {
            String second = (String) rawList.get(1);
            System.out.println(second);
        } catch (ClassCastException ex) {
            System.out.println("Raw list failed at runtime: " + ex.getClass().getSimpleName());
        }
    }

    private static void withGenerics() {
        // "Generic" does not mean "accept anything."
        // It means the code is written once with a type placeholder, and each usage fixes that placeholder to a specific type.
        // Here, List<String> means this particular list can contain only String values.
        List<String> names = new ArrayList<>();
        names.add("Java");
        names.add("Rust");
        // names.add(10); // Compile-time error: Integer is not a String.

        String first = names.get(0);
        System.out.println("Generic list first value without cast: " + first);
    }
}

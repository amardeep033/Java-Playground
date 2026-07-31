import java.util.ArrayList;
import java.util.List;

public class S13TypeErasure {
    public static void main(String[] args) {
        // Java checks List<String> and List<Integer> at compile time.
        // After compilation, generic type information is erased from the runtime class.
        List<String> names = new ArrayList<>();
        List<Integer> scores = new ArrayList<>();

        // Both lists are ArrayList at runtime.
        // The JVM does not keep separate runtime classes for ArrayList<String> and ArrayList<Integer>.
        System.out.println("List<String> runtime class: " + names.getClass().getName());
        System.out.println("List<Integer> runtime class: " + scores.getClass().getName());
        System.out.println("Same runtime class? " + (names.getClass() == scores.getClass()));
        System.out.println("Generic type checks happen at compile time, then type details are erased.");
    }
}

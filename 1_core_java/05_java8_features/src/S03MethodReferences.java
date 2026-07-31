// 3. Method References
//
// A method reference is a short form of a lambda.
// Use it when the lambda only calls an existing method.

// General idea:
// If a lambda only calls an existing method, it can often be replaced by a method reference.
//
// Lambda:
// names.stream().map(s -> s.length())
//
// Method reference:
// names.stream().map(String::length)

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class S03MethodReferences {
    public static void main(String[] args) {
        // Type 1: static method reference
        // lambda: (a, b) -> Integer.sum(a, b)
        BiFunction<Integer, Integer, Integer> staticLambda = (a, b) -> Integer.sum(a, b);
        BiFunction<Integer, Integer, Integer> staticMethodReference = Integer::sum;
        System.out.println("Static lambda: " + staticLambda.apply(10, 20));
        System.out.println("Static method reference: " + staticMethodReference.apply(10, 20));

        // Type 2: instance method reference of a particular object
        // lambda: message -> reportPrinter.print(message)
        ReportPrinter reportPrinter = new ReportPrinter("ORDER");
        Consumer<String> specificObjectLambda = message -> reportPrinter.print(message);
        Consumer<String> specificObjectReference = reportPrinter::print;
        specificObjectLambda.accept("Lambda: Laptop approved for dispatch");
        specificObjectReference.accept("Method reference: Laptop approved for dispatch");

        // Type 3: instance method reference of an arbitrary object
        // lambda: item -> item.length()
        Function<String, Integer> arbitraryObjectLambda = item -> item.length();
        Function<String, Integer> arbitraryObjectReference = String::length;
        System.out.println("Arbitrary object lambda: " + arbitraryObjectLambda.apply("Keyboard"));
        System.out.println("Arbitrary object method reference: " + arbitraryObjectReference.apply("Keyboard"));

        // Type 4: constructor reference
        // lambda: () -> new ArrayList<>()
        Supplier<List<String>> constructorLambda = () -> new ArrayList<>();
        Supplier<List<String>> constructorReference = ArrayList::new;
        List<String> lambdaItems = constructorLambda.get();
        List<String> referenceItems = constructorReference.get();
        lambdaItems.add("Mouse");
        referenceItems.add("Monitor");
        System.out.println("Constructor lambda list: " + lambdaItems);
        System.out.println("Constructor reference list: " + referenceItems);
    }

    private static class ReportPrinter {
        private final String reportType;

        ReportPrinter(String reportType) {
            this.reportType = reportType;
        }

        void print(String message) {
            System.out.println(reportType + ": " + message);
        }
    }
}

// | Type                                   | Example               | Equivalent Lambda             |
// | -------------------------------------- | --------------------- | ----------------------------- |
// | Static method                          | `Integer::sum`        | `(a, b) -> Integer.sum(a, b)` |
// | Instance method of a particular object | `System.out::println` | `x -> System.out.println(x)`  |
// | Instance method of an arbitrary object | `String::length`      | `s -> s.length()`             |
// | Constructor reference                  | `ArrayList::new`      | `() -> new ArrayList<>()`     |

// 2. Custom Functional Interfaces and Types of Lambda Expressions
//
// This file shows the same idea in two ways:
// 1. Custom functional interfaces, where the method name has business meaning.
// 2. Built-in functional interfaces, where Java provides generic method names.

// | Functional Interface | Abstract Method     | Shape         | Purpose                          | Example Lambda               | Common APIs                                          |
// | -------------------- | ------------------- | ------------- | -------------------------------- | ---------------------------- | ---------------------------------------------------- |
// | **Predicate<T>**     | `boolean test(T t)` | `T → boolean` | Check a condition                | `n -> n % 2 == 0`            | `filter()`, `removeIf()`, `anyMatch()`, `allMatch()` |
// | **Function<T,R>**    | `R apply(T t)`      | `T → R`       | Transform one value into another | `s -> s.length()`            | `map()`, `Collectors.toMap()`                        |
// | **Consumer<T>**      | `void accept(T t)`  | `T → void`    | Consume/use a value, no return   | `s -> System.out.println(s)` | `forEach()`, `ifPresent()`                           |
// | **Supplier<T>**      | `T get()`           | `() → T`      | Produce a value when asked       | `() -> new ArrayList<>()`    | `orElseGet()`, `Stream.generate()`                   |

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class S02CustomAndLambdaTypes {
    public static void main(String[] args) {
        // Custom intf lambda 1: one parameter, expression body
        TaxCalculator gstCalculator = amount -> amount * 0.18;
        System.out.println("GST on 1000: " + gstCalculator.tax(1000));

        // Custom intf lambda 2: two parameters, expression body
        InvoiceCalculator invoiceCalculator = (price, quantity) -> price * quantity;
        System.out.println("Invoice total: " + invoiceCalculator.total(1200, 3));
        
        // Custom intf lambda 3: one parameter, block body
        RiskScorer riskScorer = application -> {
            int score = 0;
            if (application.salary >= 50000) {
                score += 50;
            }
            if (application.creditScore >= 750) {
                score += 50;
            }
            return score;
        };
        LoanApplication application = new LoanApplication("Asha", 65000, 780);
        System.out.println("Risk score: " + riskScorer.score(application));

        // Built-in functional interfaces:
        // Java provides these so we do not need to create a custom functional interface
        // for every common lambda shape.

        // Builtin intf lambda 1: Supplier<T>, no parameter
        Supplier<String> noParameter = () -> "INV-1001";
        System.out.println("Generated invoice: " + noParameter.get());

        // Builtin intf lambda 2: Function<T, R>, one parameter
        Function<Integer, Integer> oneParameter = amount -> amount + 100;
        System.out.println("Amount after fee: " + oneParameter.apply(900));

        // Builtin intf lambda 3: Predicate<T>, one parameter returns boolean
        // A Predicate can contain extra statements inside a block body if needed.
        // The only contract is that it must finally return a boolean.
        // Predicates can also be combined: Predicate<Integer> positiveEven = positive.and(even);
        Predicate<LoanApplication> expressionBody = app -> app.creditScore >= 700;
        System.out.println("Credit score accepted? " + expressionBody.test(application));

        // Builtin intf lambda 4: Consumer<T>, one parameter returns nothing
        Consumer<LoanApplication> blockBody = app -> {
            String status = app.salary >= 50000 ? "eligible" : "manual review";
            System.out.println(app.name + " is " + status);
        };
        blockBody.accept(application);
    }

    @FunctionalInterface
    private interface TaxCalculator {
        double tax(double amount);
    }

    @FunctionalInterface
    private interface InvoiceCalculator {
        int total(int price, int quantity);
    }

    @FunctionalInterface
    private interface RiskScorer {
        int score(LoanApplication application);
    }

    private static class LoanApplication {
        private final String name;
        private final int salary;
        private final int creditScore;

        LoanApplication(String name, int salary, int creditScore) {
            this.name = name;
            this.salary = salary;
            this.creditScore = creditScore;
        }
    }
}

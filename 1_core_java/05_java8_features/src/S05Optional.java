// 5. Optional
//
// Optional represents a value that may be present or absent.
// It makes absence visible in the method signature instead of silently returning null.

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class S05Optional {
    public static void main(String[] args) {
        // Type 1: Optional.of()
        // Optional.of():
        // Use when you are sure the value is not null.
        // If the value is null, Optional.of() throws NullPointerException immediately.
        Optional<String> sureName = Optional.of("Asha");
        System.out.println("of(): " + sureName.orElse("Unknown"));

        // Type 2: Optional.ofNullable()
        // Optional.ofNullable():
        // Use when the value may be null.
        // If findNickname(false) returns null, this becomes Optional.empty().
        Optional<String> maybeName = Optional.ofNullable(findNickname(false));
        System.out.println("ofNullable(): " + maybeName.orElse("Unknown"));

        // Type 3: Optional.empty()
        // Optional.empty():
        // Explicitly creates an Optional with no value.
        // This is useful when a method wants to return "no result" without returning null.
        Optional<String> emptyName = Optional.empty();
        System.out.println("empty(): " + emptyName.orElse("Unknown"));

        // Type 4: Optional.orElse()
        // Reading values:
        // get() exists, but it throws when the Optional is empty.
        // orElse() is safer because it provides a default value.
        UserRepository repository = new UserRepository();
        String email = repository.findById(2)
                .map(user -> user.email)
                .orElse("not-found@example.com");
        System.out.println("Email: " + email);

        // Type 5: Optional.orElseGet()
        // orElseGet():
        // Uses a Supplier, so the fallback value is created only when the Optional is empty.
        String fallbackEmail = repository.findById(9)
                .map(user -> user.email)
                .orElseGet(() -> "created-by-supplier@example.com");
        System.out.println("Fallback email: " + fallbackEmail);

        // Type 6: Optional.orElseThrow()
        // orElseThrow():
        // Return the value when present, otherwise throw the exception supplied by the lambda.
        String requiredName = repository.findById(1)
                .map(user -> user.name)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        System.out.println("Required name: " + requiredName);

        // Type 7: Optional.filter() and Optional.ifPresent()
        // Optional.filter():
        // Keeps the user only if the predicate is true.
        // Optional.ifPresent():
        // Runs the Consumer only when the value exists.
        repository.findById(3)
                .filter(user -> user.age >= 18)
                .ifPresent(user -> System.out.println("Adult: " + user.name));

        // Type 8: checked Optional.get()
        // Checked get():
        // Calling get() after isPresent() is safe, but fluent methods like map(), orElse(),
        // orElseGet(), and orElseThrow() are usually preferred.
        if (sureName.isPresent()) {
            System.out.println("Checked get(): " + sureName.get());
        }
    }

    private static String findNickname(boolean found) {
        if (found) {
            return "Ace";
        }
        return null;
    }

    private static class UserRepository {
        private final List<User> users = Arrays.asList(
                new User(1, "Asha", 24, "asha@example.com"),
                new User(2, "Ravi", 17, "ravi@example.com"),
                new User(3, "Meera", 29, "meera@example.com")
        );

        Optional<User> findById(int id) {
            return users.stream()
                    .filter(user -> user.id == id)
                    .findFirst();
        }
    }

    private static class User {
        private final int id;
        private final String name;
        private final int age;
        private final String email;

        User(int id, String name, int age, String email) {
            this.id = id;
            this.name = name;
            this.age = age;
            this.email = email;
        }
    }
}

// Creation:
// of(), ofNullable(), empty()
//
// Read Value:
// get(), orElse(), orElseGet() -> Supplier, orElseThrow()
//
// Functional Operations:
// map() -> Function, filter() -> Predicate, ifPresent() -> Consumer

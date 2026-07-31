import java.util.Objects;

public class S12BestPractices {
    public static void main(String[] args) {
        User user = new User("Asha");
        System.out.println(user.greeting());

        try {
            new User(null);
        } catch (NullPointerException exception) {
            System.out.println("Fail fast message: " + exception.getMessage());
        }
    }

    private static class User {
        private final String name;

        User(String name) {
            this.name = Objects.requireNonNull(name, "name cannot be null");
        }

        String greeting() {
            return "Hello, " + name;
        }
    }
}

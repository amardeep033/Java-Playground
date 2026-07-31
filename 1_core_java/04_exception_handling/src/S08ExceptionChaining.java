// Exception chaining means wrapping one exception inside another.
// Example: a SQLException can be wrapped inside a ServiceException.
// Use getCause() to preserve and inspect the original failure.

public class S08ExceptionChaining {
    public static void main(String[] args) {
        try {
            register("A", 15);
        } catch (RegistrationException exception) {
            // The high-level exception gives business context.
            System.out.println("High-level message: " + exception.getMessage());
            // The cause keeps the original technical failure.
            System.out.println("Original cause: " + exception.getCause().getClass());
            System.out.println("Cause message: " + exception.getCause().getMessage());
        }
    }

    private static void register(String name, int age) {
        try {
            validateName(name);
            validateAge(age);
        } catch (IllegalArgumentException exception) {
            // Pass both a new message and the original cause.
            throw new RegistrationException("Registration failed", exception);
        }
    }

    private static void validateName(String name) {
        if (name == null || name.length() < 2) {
            // Original low-level validation failure.
            throw new IllegalArgumentException("Name must have at least 2 characters");
        }
    }

    private static void validateAge(int age) {
        if (age < 18) {
            // Original low-level validation failure.
            throw new IllegalArgumentException("Age must be 18 or above");
        }
    }

    private static class RegistrationException extends RuntimeException {
        // A constructor with only message loses the original cause.
        RegistrationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}

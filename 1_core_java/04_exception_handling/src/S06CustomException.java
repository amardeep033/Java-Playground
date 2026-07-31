public class S06CustomException {
    public static void main(String[] args) {
        try {
            createAccount("A", 21);
        } catch (RegistrationException exception) {
            System.out.println(exception.getMessage());
        }
    }

    private static void createAccount(String name, int age) {
        if (name == null || name.length() < 2) {
            throw new RegistrationException("Name must have at least 2 characters");
        }
        if (age < 18) {
            throw new RegistrationException("Age must be 18 or above");
        }
        System.out.println("Account created");
    }

    // Extend Exception for a checked custom exception.
    // Extend RuntimeException for an unchecked custom exception.
    private static class RegistrationException extends RuntimeException {
        // Use RegistrationException(String message, Throwable cause) when exception chaining is needed.
        RegistrationException(String message) {
            // Call the parent constructor to store the exception message.
            super(message);
        }
    }
}

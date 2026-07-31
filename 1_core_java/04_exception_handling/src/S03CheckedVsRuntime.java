public class S03CheckedVsRuntime {
    public static void main(String[] args) {
        try {
            registerAge(16);
        } catch (InvalidAgeException exception) {
            System.out.println("Checked exception handled: " + exception.getMessage());
        }

        try {
            withdraw(500, 900);
        } catch (IllegalArgumentException exception) {
            System.out.println("Runtime exception handled: " + exception.getMessage());
        }
    }

    private static void registerAge(int age) throws InvalidAgeException {
        if (age < 18) {
            throw new InvalidAgeException("Age must be 18 or above");
        }
        System.out.println("Age accepted");
    }

    private static void withdraw(int balance, int amount) {
        if (amount > balance) {
            throw new IllegalArgumentException("Cannot withdraw more than balance");
        }
        System.out.println("Withdraw successful");
    }

    private static class InvalidAgeException extends Exception {
        InvalidAgeException(String message) {
            super(message);
        }
    }
}

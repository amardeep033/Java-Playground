public class S04TryCatchFinally {
    public static void main(String[] args) {
        divide(10, 2);
        divide(10, 0);
    }

    private static void divide(int left, int right) {
        try {
            System.out.println("A");
            double x = left / right;
            // If an exception occurs above, this line will not run.
            System.out.println("B");
        } 
        // Java looks for a matching catch block. Multiple exception types can be handled using |.
        // If no catch block matches, the exception is passed to the caller.
        catch (ArithmeticException exception) {
            System.out.println("C: " + exception.getMessage());
        } finally {
            // finally normally runs for cleanup after try/catch.
            // For files, sockets, and streams, try-with-resources is usually preferred.
            System.out.println("D");
        }
    }
}

// If the try block returns, finally still runs before the method returns.
// If both try/catch and finally return, the finally return overrides the earlier return.

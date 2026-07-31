// Try-with-resources is preferred over manually closing resources in a finally block.

import java.io.BufferedReader;
import java.io.FileReader;

public class S10TryWithResources {
    public static void main(String[] args) {
        // Resources are acquired inside the try parentheses. Multiple resources are allowed.
        // try (FileReader fr = new FileReader("a.txt"); BufferedReader br = new BufferedReader(fr))
        try (FakeResource resource = new FakeResource("orders.csv")) {
            resource.read();
        }
        // No finally block is required; Java automatically closes the resource, even when an exception occurs.
    }

    // Any type that implements AutoCloseable can be used in try-with-resources.
    // Closeable is mainly for IO resources and is a subtype of AutoCloseable.
    private static class FakeResource implements AutoCloseable {
        private final String name;

        FakeResource(String name) {
            this.name = name;
            System.out.println("Opening " + name);
        }

        void read() {
            System.out.println("Reading " + name);
        }

        @Override
        public void close() {
            System.out.println("Closing " + name + " automatically");
        }
    }
}


// If both the try block and close() throw exceptions, Java throws the main exception
// and stores the close() exception as a suppressed exception.
// catch (Exception e) {
//    System.out.println(e.getMessage());
//    for (Throwable t : e.getSuppressed()) {
//       System.out.println(t.getMessage());
//    }
// }

// | Exception Chaining          | Suppressed Exception              |
// | --------------------------- | --------------------------------- |
// | Created manually            | Created automatically             |
// | new Exception(msg, cause)    | try-with-resources                |
// | Access using `getCause()`   | Access using `getSuppressed()`    |
// | One primary cause           | One or more suppressed exceptions |
// | Used to add context         | Used to preserve cleanup failures |

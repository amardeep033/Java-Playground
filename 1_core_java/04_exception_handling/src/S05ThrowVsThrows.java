// throw is used to throw an exception object.
// throws is used in a method signature to declare that the method may pass exception(s) to its caller.

public class S05ThrowVsThrows {
    public static void main(String[] args) {
        try {
            openFile("notes.txt");
            openFile("");
        } catch (FileProblemException exception) {
            System.out.println("Caller handled: " + exception.getMessage());
        }
    }

    // This method does not handle the exception itself; it lets the caller handle it.
    // A method can declare multiple exception types, for example: throws IOException, SQLException.
    private static void openFile(String fileName) throws FileProblemException {
        if (fileName == null || fileName.isBlank()) {
            throw new FileProblemException("File name is required");
        }
        System.out.println("Opening " + fileName);
    }

    private static class FileProblemException extends Exception {
        FileProblemException(String message) {
            super(message);
        }
    }
}

// Can we use throw without throws?
// Yes, if you're throwing an unchecked (runtime) exception.

// Compiles:
// public void test() {
//     throw new RuntimeException("Error");
// }

// Does not compile:
// public void test() {
//     throw new Exception("Error"); // Exception is checked, so it must be caught or declared.
// }

// Unnecessary: RuntimeException is unchecked, so callers are not forced to catch it.
// public void test() throws RuntimeException {
//     throw new RuntimeException("Error");
// }

// | `throw`                             | `throws`                                      |
// | ----------------------------------- | --------------------------------------------- |
// | Used inside a method                | Used in the method signature                  |
// | Actually throws an exception object | Declares that a method may throw an exception |
// | Followed by an exception object     | Followed by exception type(s)                 |
// | Example: `throw new IOException();` | Example: `void read() throws IOException`     |

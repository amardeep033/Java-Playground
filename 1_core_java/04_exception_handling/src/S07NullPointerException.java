import java.util.Objects;

// null means a reference is not pointing to any object.

public class S07NullPointerException {
    public static void main(String[] args) {
        String language = null;

        try {
            // NullPointerException occurs when calling a method through null.
            // Printing null or comparing it with == is safe.
            System.out.println(language.equals("Java"));
        } catch (NullPointerException exception) {
            System.out.println("Calling a method on null causes NullPointerException");
        }

        System.out.println("\"Java\".equals(language): " + "Java".equals(language));
        System.out.println("Objects.equals(language, \"Java\"): " + Objects.equals(language, "Java"));
        System.out.println("Objects.equals(language, null): " + Objects.equals(language, null));
    }
}


// if (s.equals("hello")) {        // Can throw NullPointerException.
// if ("hello".equals(s)) {        // Safe because the string literal is not null.
// if (Objects.equals(s, "Java"))  // Safe even when either value is null.

// String s = null;
// System.out.println(Objects.equals(s, null)); // true

// Use Objects.requireNonNull when null is not allowed and you want to fail fast with a clear message.
// System.out.println(Objects.requireNonNull(name).length());

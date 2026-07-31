public class S03GenericMethod {
    public static void main(String[] args) {
        printValue(10);
        printValue("Hello");
        printValue(2.5);
    }

    private static <T> void printValue(T value) {
        // T belongs only to this method, not to the whole class.
        // The compiler infers T from the argument: Integer for 10, String for "Hello", Double for 2.5.
        // In this example we only read/use the value; if this method accepted a List<T>, it could also add T values.
        System.out.println("Generic method inferred " + value.getClass().getSimpleName() + ": " + value);
    }
}

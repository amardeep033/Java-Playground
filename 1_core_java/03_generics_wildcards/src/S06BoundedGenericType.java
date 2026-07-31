public class S06BoundedGenericType {
    public static void main(String[] args) {
        NumberBox<Integer> integerBox = new NumberBox<>(10);
        NumberBox<Double> doubleBox = new NumberBox<>(3.5);
        // NumberBox<String> textBox = new NumberBox<>("invalid"); // Compile-time error.

        System.out.println("Integer as double: " + integerBox.asDouble());
        System.out.println("Double as double: " + doubleBox.asDouble());
    }

    static class NumberBox<T extends Number> {
        // T is restricted to Number or a subclass of Number.
        // Because of this bound, Java knows every T has Number methods such as doubleValue().
        // Integer and Double are valid; String is rejected at compile time.
        private final T value;

        NumberBox(T value) {
            this.value = value;
        }

        double asDouble() {
            return value.doubleValue();
        }
    }
}

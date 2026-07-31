public class S02GenericClass {
    public static void main(String[] args) {
        Box<String> languageBox = new Box<>();
        languageBox.set("Java");
        System.out.println("Box<String>: " + languageBox.get());

        Box<Integer> scoreBox = new Box<>();
        scoreBox.set(100);
        System.out.println("Box<Integer>: " + scoreBox.get());
    }

    // This is a generic class because it declares T at the class level.
    // T becomes a real type for each object, such as Box<String> or Box<Integer>.
    static class Box<T> {
        // Since Box owns the named type T, it can both write T through set() and read T through get().
        // This is different from List<?> where the actual type is unknown, so writing is restricted.
        private T value;

        void set(T value) {
            this.value = value;
        }

        T get() {
            return value;
        }
    }
}

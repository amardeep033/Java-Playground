public class S04MultipleGenericTypes {
    public static void main(String[] args) {
        // A generic type can have more than one placeholder.
        // Here K is fixed as Integer and V is fixed as String for this Pair object.
        // This is useful when two related values have different types, such as id -> name.
        Pair<Integer, String> rank = new Pair<>(1, "Generics");
        System.out.println("Pair<Integer, String>: " + rank.key() + " -> " + rank.value());
    }

    // K and V are two separate named type parameters.
    // The compiler keeps them related to the same Pair instance: key() returns K and value() returns V.
    static class Pair<K, V> {
        private final K key;
        private final V value;

        Pair(K key, V value) {
            this.key = key;
            this.value = value;
        }

        K key() {
            return key;
        }

        V value() {
            return value;
        }
    }
}

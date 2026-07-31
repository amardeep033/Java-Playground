public class S07MultipleBounds {
    public static void main(String[] args) {
        // Integer and Double both extend Number and implement Comparable with their own type.
        // That means both satisfy the multiple bounds used by maxNumber().
        Integer max = maxNumber(10, 20);
        Double higher = maxNumber(3.5, 2.5);

        System.out.println("Max Integer using <T extends Number & Comparable<T>>: " + max);
        System.out.println("Max Double using <T extends Number & Comparable<T>>: " + higher);
    }

    // T must satisfy both requirements:
    // 1. extends Number, so it is numeric.
    // 2. implements Comparable<T>, so two T values can be compared.
    // The class bound must come first, then interface bounds follow with &.
    private static <T extends Number & Comparable<T>> T maxNumber(T first, T second) {
        return first.compareTo(second) >= 0 ? first : second;
    }
}

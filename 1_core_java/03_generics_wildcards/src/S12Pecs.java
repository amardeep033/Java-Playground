import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class S12Pecs {
    public static void main(String[] args) {
        // Source is a producer: this method only reads numbers from it.
        List<Integer> source = Arrays.asList(7, 8, 9);

        // Destination is a consumer: this method only writes numbers into it.
        List<Number> destination = new ArrayList<>();

        copyNumbers(source, destination);
        System.out.println("Copied from producer to consumer: " + destination);
    }

    // PECS means Producer Extends, Consumer Super.
    // producer uses ? extends Number because it gives Number values to this method.
    // consumer uses ? super Number because it receives Number values from this method.
    private static void copyNumbers(List<? extends Number> producer, List<? super Number> consumer) {
        for (Number number : producer) {
            consumer.add(number);
        }
    }
}

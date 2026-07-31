public class S02ExceptionHierarchy {
    public static void main(String[] args) {
        Throwable throwable = new Throwable("Anything throwable starts here");
        Exception exception = new Exception("Application-level problem");
        RuntimeException runtimeException = new RuntimeException("Unchecked problem");
        Error error = new AssertionError("Serious JVM/system-style problem");

        printType(throwable);
        printType(exception);
        printType(runtimeException);
        printType(error);
    }

    private static void printType(Throwable throwable) {
        System.out.println(throwable.getClass().getSimpleName() + ": " + throwable.getMessage());
    }
}

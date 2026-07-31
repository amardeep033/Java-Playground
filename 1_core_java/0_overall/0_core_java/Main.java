public class Main {
    /*
     * public: JVM can call this method from outside the class.
     * static: JVM can call it without creating a Main object first.
     * void: the method does not return a value.
     * main: conventional entry point name searched by the JVM.
     * String[] args: command-line arguments passed after `java Main`.
     */
    public static void main(String[] args) {
        System.out.println("Hello from plain Java");
    }
}

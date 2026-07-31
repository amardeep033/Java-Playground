// An exception is an event that interrupts the normal flow of a program.
// Object
// └── Throwable -- parent of everything that can be thrown.
//     ├── Error -- serious JVM problems, such as OutOfMemoryError, where recovery is usually not possible.
//     └── Exception -- problems an application may handle.
//         ├── Checked exceptions -- compiler-enforced failures, often caused by external conditions such as missing files.
//         └── RuntimeException -- unchecked failures, often caused by bugs such as null access, invalid indexes, or division by zero.

public class S01WhatIsException {
    public static void main(String[] args) {
        try {
            System.out.println("A");
            int result = 10 / 0;
            System.out.println("B");
        } catch (ArithmeticException exception) {
            System.out.println("C: " + exception.getMessage());
        }
        System.out.println("D.");
    }
}

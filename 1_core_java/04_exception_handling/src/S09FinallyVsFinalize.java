public class S09FinallyVsFinalize {
    public static void main(String[] args) {
        try {
            System.out.println("try: work starts");
            return;
        } finally {
            System.out.println("finally: cleanup still runs before return");
        }

        // finalize() is intentionally not demonstrated.
        // It is deprecated, unreliable for cleanup, and should not be used in new code.
        // Because:
        // There is no guarantee when or if it will run.
        // It hurts GC performance.
        // It can lead to subtle bugs and resource leaks.
        // Instead, Java recommends:
        // 1. try-with-resources
        // 2. AutoCloseable
        // 3. explicit close() methods
    }
}

// protected void finalize() {
//     System.out.println("Finalize called");
// }

// finalize() is a method, while finally is a block.
// The JVM might call finalize() before reclaiming an object's memory.
// The JVM decides when, or even if, garbage collection happens.
// Calling finalize() manually is just a normal method call; it does not clean memory.

// | `finally`                                                            | `finalize()`                                   |
// | -------------------------------------------------------------------- | ---------------------------------------------- |
// | **Block**                                                            | **Method**                                     |
// | Used for **exception handling**                                      | Used by **Garbage Collector** (historically)   |
// | Executes immediately before leaving `try`/`catch`                    | May execute before object is garbage collected |
// | Used for resource cleanup                                            | **Not recommended** for resource cleanup       |
// | Execution is predictable (except special cases like `System.exit()`) | Execution is unpredictable                     |
// | Still used today                                                     | Deprecated (Java 9+) and effectively obsolete  |

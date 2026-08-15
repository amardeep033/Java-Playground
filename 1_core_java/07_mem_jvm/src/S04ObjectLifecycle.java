import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;

// Memory leak:
// Objects are no longer logically needed, but they are still reachable through references.
// static List<Person> people = new ArrayList<>(); 
// for (int i = 0; i < 1_000_000; i++) {
//     people.add(new Person());
// }
// Common examples: static collection, listener, callback, ThreadLocal

public class S04ObjectLifecycle {
    static class Person {
        String name;

        Person(String name) {
            this.name = name;
        }
    }

    public static void main(String[] args) {
        Person p1 = new Person("Alice"); // p1 is a strong reference
        Person p2 = p1;

        p1 = null;
        System.out.println(p2.name); // Alice: object is still reachable through p2

        p2 = null; // object is now eligible for GC because no strong reference points to it
        // An object is eligible for GC when it is no longer reachable from GC roots.
        // GC roots include active stack references, static fields, JNI references, and similar JVM-managed roots.


        // -------------------------------------------------------------------------------------------

        // WeakReference:
        // Use this when you want to refer to an object without keeping it alive.
        // If no strong reference exists, GC may clear the object and weak.get() may return null.
        Person weakPerson = new Person("Weak Person");
        WeakReference<Person> weak = new WeakReference<>(weakPerson);
        // While weakPerson still exists, the object is strongly reachable.
        // If weakPerson later becomes null, only the WeakReference remains and GC may clear the object.

        // -------------------------------------------------------------------------------------------

        // SoftReference:
        // Use this for memory-sensitive caching.
        // The JVM may keep the object longer than a weak reference, but can clear it under memory pressure.
        Person softPerson = new Person("Soft Person");
        SoftReference<Person> soft = new SoftReference<>(softPerson);
        // While softPerson still exists, the object is strongly reachable.
        // If softPerson later becomes null, only the SoftReference remains and GC may clear it under memory pressure.

    }
}

// new Person()
//         ↓
// Memory allocated
//         ↓
// Constructor runs
//         ↓
// Object is used
//         ↓
// Last reference disappears
//         ↓
// Object becomes eligible for GC
//         ↓
// JVM reclaims memory at some later time

// |                                               | WeakReference            | SoftReference                               |
// | --------------------------------------------- | ------------------------ | ------------------------------------------- |
// | Keeps object alive?                           | No                       | Not indefinitely                            |
// | Can GC reclaim it when otherwise unreachable? | Yes                      | Yes, depending on memory pressure/GC policy |
// | Typical idea                                  | Do not prevent collection | Memory-sensitive caching                   |
// | get() after collection                        | null                     | null                                        |

// Why reference counting isn't enough
// A and B may reference each other, so their reference counts may never become 0.
// But if neither object is reachable from a GC root, a tracing GC can still collect them.
// Mark and Sweep:
// mark reachable objects from GC roots, then sweep unreachable objects.

// Why not just collect everything constantly?
// Because GC itself costs CPU time.

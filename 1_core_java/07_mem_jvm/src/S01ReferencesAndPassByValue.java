// Primitive values are stored directly in local variables/fields. For method calls, the primitive value is copied.
// For primitives, == compares the actual value. equals() is not used directly on primitives.
// Object variables store references. For method calls, the reference value is copied, not the object itself.
// For objects, == compares references. equals() compares logical equality only if the class implements it that way.
// hashCode() returns an int hash value. Collisions are allowed.
// If a.equals(b) is true, then a.hashCode() must be equal to b.hashCode().
// But same hashCode() does not prove that two objects are equal.

public class S01ReferencesAndPassByValue {
    static class Person {
        String name;
    }
    static int y=1;

    public static void main(String[] args) {
        Person p1 = new Person();
        p1.name = "Alice";

        Person p2 = p1;
        p2.name = "Bob";
        System.out.println(p1.name); // Bob: p1 and p2 point to the same object

        // If we do p1 = null, printing p2.name still works because:
        // p1 -> [object on heap] <- p2, and only the p1 reference was removed.
        // If both p1 and p2 stop referring to the object, it becomes eligible for GC.
        // Eligible for GC does not mean immediately cleared.

        p2 = new Person();
        p2.name = "Charlie";
        System.out.println(p1.name); // Bob: p2 now points to a different object

        int x=1;
        change(p1,x);
        System.out.println(p1.name); // David: object state changed through copied reference
        System.out.println(x); // 1: primitive argument was copied
        System.out.println(y); // 2: static variable belongs to the class
    }

    // In Java, everything is pass-by-value.
    // For primitives, the actual primitive value is copied, so changing x here does not change x in main().
    // For objects, the reference value is copied, so this method receives another reference to the same object.
    // Because both references point to the same object, person.name = "David" changes the object seen by main().
    // But person = new Person() changes only this local parameter, not p1 in main().
    // If code needs shared mutable state, use an object/array/wrapper field intentionally.
    // A static variable also works here because it belongs to the class, but it is shared global state.
    static void change(Person person, int x) {
        person.name = "David";
        person = new Person();
        person.name = "Eve";
        x=2;
        y=2;
    }
}

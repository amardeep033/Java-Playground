// Mutable object: its state can change through the same reference, such as person.name = "Bob".
// A final reference still allows mutation if the object itself is mutable.
// Immutable object: its state cannot be changed after creation.
// String is immutable, so s.concat(" World") returns a new String instead of changing the existing String.

public class S03FinalKeyword {
    static class Person {
        String name;

        Person(String name) {
            this.name = name;
        }
    }

    public static void main(String[] args) {
        int a=1;
        a=2;
        System.out.println(a); // normal local variable: reassignment is allowed

        final int number = 10;
        // number = 20; // compile error: final local variable cannot be reassigned

        //--------------------------------------------------------------------------

        String s = "Hello";
        s.concat(" World");
        System.out.println(s); // still Hello: String is immutable, concat() returned a new String
        s=s.concat(" World"); // s now refers to a new String object; the original String was not changed

        //--------------------------------------------------------------------------

        final Person person = new Person("Alice");
        person.name = "Bob"; // final prevents reassigning person, but Person is mutable, so its field can change
        System.out.println(person.name);
        // person = new Person("Tom"); // compile error: final reference cannot be reassigned
    }
}

// How do you make your own class immutable?
// final class Person {               ------- final prevents subclassing
//     private final String name;     ------- field must be assigned once and cannot be reassigned

//     Person(String name) {
//         this.name = name;
//     }

//     public String getName() {      ------- expose only getters, not setters
//         return name;
//     }
// }

// 1. final variable
// final int x = 10;
// x = 20; // compile error -- You cannot reassign it

// 2. final object
// final Person p = new Person();
// p = new Person(); // compile error -- cannot reassign p to a new object
// p.name = "Alice"; // allowed -- updates state inside the same object if Person is mutable

// 3. final method
// class Animal {
//     final void eat() {
//         System.out.println("Eating");
//     }
// }
// class Dog extends Animal {
//     // compile error -- subclass cannot override final method
// }

// 4. final class
// final class Person {
// }
// class Employee extends Person { // compile error -- final class cannot be extended
// }

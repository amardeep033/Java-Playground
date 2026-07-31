public class S02CopyAndClone {
    // Static nested class:
    // Address belongs to S02CopyAndClone as a nested type.
    // Because it is static, an Address object can be created without creating an S02CopyAndClone object.
    // In real projects, these would usually be separate classes unless they are used only inside this example.
    static class Address {
        String city;

        Address(String city) {
            this.city = city;
        }
    }

    static class Student implements Cloneable {
        String name;
        Address address;

        Student(String name, Address address) {
            this.name = name;
            this.address = address;
        }

        // Copy constructor:
        // This is our own constructor that creates a new Student from another Student.
        // Here we also create a new Address, so the nested Address object is not shared.
        Student(Student other) {
            this.name = other.name;
            this.address = new Address(other.address.city);
        }

        // clone():
        // Object.clone() performs a field-by-field copy.
        // For reference fields, it copies the reference value, so this default clone is shallow.
        // Cloneable is only a marker interface. Without it, Object.clone() throws CloneNotSupportedException.
        @Override
        protected Student clone() throws CloneNotSupportedException {
            return (Student) super.clone();
        }
    }

    public static void main(String[] args) throws CloneNotSupportedException {
        Student original = new Student("S1", new Address("C1"));
        Student cloned = original.clone();

        cloned.name = "S2";
        cloned.address.city = "C2";
        System.out.println(original.name); // S1: String reference was replaced only in cloned
        System.out.println(original.address.city); // C2: Address object is shared because clone is shallow

        // Default clone is shallow.
        // The outer Student object is new, but the nested Address reference points to the same Address object.
        // To make an independent copy, we need custom copy logic, such as a copy constructor.

        Student super_cloned = new Student(original);
        super_cloned.name = "S3";
        super_cloned.address.city = "C3";;
        System.out.println(original.name); // S1: original Student name was not changed
        System.out.println(original.address.city); // C2: original Address was not changed by the copy constructor object
    }
}

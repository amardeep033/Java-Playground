
// import allows this default-package class to use classes from account package.
import java.util.Arrays; //built-in package import from JDK
import account.Account;
import account.AccountOperation;
import account.CurrentAccount;
import account.SavingsAccount;

// Executable class -- JVM entry point method -- The JVM starts execution from this exact signature: public static void main(String[] args)
// You can overload main(), but the JVM only invokes -- public static void main(String[] args)

// A Java source file can contain multiple top-level classes -- but at most ONE can be public:
// public class A {}
// public class B {}   // ❌ Compile error (two public classes in one file)

// The public class name must match the file name: OopsBasics.java -> public class OopsBasics
// public class OopsBasicsWrong {}  // ❌ If file name is OopsBasics.java

// Top-level classes cannot be static. Only nested classes can be static.
// public static class OopsBasics {} // ❌ Not allowed

public class OopsBasics { //class: blueprint for JVM to start this program

    // Static variable: belongs to class, shared by all objects.
    // Final variable: cannot be reassigned after initialization.
    // Java has no true global variable; this still belongs to OopsBasics class.
    static final String BANK_NAME = "Core Java Bank";

    private OopsBasics() {
        //private constructor: blocks new OopsBasics()
    }

    // static can be used with:
    // - variables (shared by all objects)
    // - methods (called without creating an object)
    // - initialization blocks (run once when the class is loaded)
    // - nested classes (not top-level classes)

    // final can be used with:
    // - variables (cannot be reassigned)
    // - methods (cannot be overridden)
    // - classes (cannot be extended)
    // - parameters (cannot be reassigned inside the method)

    // public -> JVM can access it from outside the class.
    // static -> JVM can call it without creating an object.
    // void -> returns nothing.
    // String[] args -> command-line arguments.

    // Function vs Method: Java uses the term "method" because every behavior belongs to a class -- Java does not support standalone functions.
    public static void main(String[] args) {
        int totalAccounts = 4; //local variable: declared inside method, must be initialized before use

        final int maxLoginAttempts = 3; //local final variable: assign once
        // maxLoginAttempts = 5; //final cant be reassigned

        System.out.println("Bank name: " + BANK_NAME);
        System.out.println("Total accounts: " + totalAccounts);
        System.out.println("Max login attempts: " + maxLoginAttempts);
        System.out.println("Command-line args: " + Arrays.toString(args));

        //CurrentAccount acc1 = new CurrentAccount(); //valid, but reference can only point to CurrentAccount
        //CurrentAccount acc1 = new Account(); //invalid, parent object cannot fit into child reference
        //Account acc = new Account(); //invalid now because Account is abstract
        //Reference type is Account: compiler allows only Account methods/fields through acc1
        //Actual object type is CurrentAccount: overridden methods run from CurrentAccount at runtime
        //Account acc1 = new CurrentAccount(); //valid, parent reference can hold child object
        Account acc1 = new CurrentAccount(); //no-argument constructor in CurrentAccount calls Account constructor
        Account acc2 = new SavingsAccount(); //no-argument constructor in SavingsAccount calls Account constructor
        SavingsAccount acc3 = new SavingsAccount("SAV-2", 2000); //parameterized constructor
        Account acc4 = new SavingsAccount(acc3); //copy constructor: creates object from another object
        AccountOperation operation = new SavingsAccount(); //abstraction + polymorphism through interface

        acc1.openAccount(); //instance method: called using object, runtime runs CurrentAccount version
        acc2.openAccount(); //instance method: called using object, runtime runs SavingsAccount version
        operation.openAccount(); //interface reference, SavingsAccount method runs at runtime
        acc3.showBalance();
        acc4.showBalance();

        System.out.println("Public accountNumber from outside package: " + acc1.accountNumber);
        acc1.showBalance(); //instance method: uses private balance of acc1 object
        acc1.deposit(500); //method overloading: deposit(double)
        acc1.deposit(100, 200, 300); //method overloading + varargs: deposit(double, double...)
        acc1.showBalance();
        System.out.println("Objects created: " + Account.getObjectCount()); //static method: called using class name
        // acc1.accountType; //protected: not accessible here because OopsBasics is not child and not in account package
        // acc1.branchCode; //default/package-private: not accessible outside account package
        // acc1.balance; //private: not accessible outside Account class

        acc2 = null; //object lifecycle: object used above, now reference removed
        //If no reference points to that SavingsAccount object, it becomes eligible for garbage collection later.
    }
}

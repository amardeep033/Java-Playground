# 2. Java Variables, Methods, Classes, Constructors, and OOP Basics

This folder uses one small account example to cover variables, methods, classes,
packages, constructors, and basic OOP without going too deep into each topic.

## Topics Covered

| No | Area | Topics |
|---|---|---|
|01|`01_variables/`|Local, instance, static, final, why Java has no true global variables|
|01|`02_methods/`|`public static void main(String[] args)`, static vs instance, overloading, varargs|
|01|`03_classes_objects_packages/`|Class, object, `new` keyword, object lifecycle, folder structure, access modifiers, package, import, built-in vs user-defined packages|
|01|`04_constructors/`|Default, parameterized, copy, private, overloading|
|01|`05_oops/`|Inheritance, encapsulation, abstraction, polymorphism, runtime polymorphism|

## Current Folder Structure

```text
src/
├── OopsBasics.java                  (default package, main class)
└── account/
    ├── Account.java                 (package account)
    ├── AccountOperation.java        (package account)
    ├── CurrentAccount.java          (package account)
    └── SavingsAccount.java          (package account)
```

## How To Run

From this folder:

```bash
javac -d out src/OopsBasics.java src/account/*.java
java -cp out OopsBasics
```

Or compile all Java files:

```bash
javac -d out $(find src -name "*.java")
java -cp out OopsBasics
```

## Code Map

| File | What It Shows |
|---|---|
| `OopsBasics.java` | `main`, local variables, `final`, static constant, private constructor, imports, object creation, polymorphic references |
| `Account.java` | instance variables, static variable, access modifiers, constructors, instance/static methods, overloading, varargs, encapsulation |
| `AccountOperation.java` | interface-based abstraction |
| `CurrentAccount.java` | inheritance, overriding, `protected`, package-private access |
| `SavingsAccount.java` | inheritance, overriding, runtime polymorphism |

## Important Examples In This Code

| Concept | Where |
|---|---|
| Local variable | `int totalAccounts = 4;` inside `main` |
| Instance variable | `accountNumber`, `accountType`, `branchCode`, `balance` in `Account` |
| Static variable | `objectCount` in `Account` |
| Static final constant | `BANK_NAME` in `OopsBasics` |
| No true global variable | `BANK_NAME` is shared, but still belongs to `OopsBasics` |
| Static method | `Account.getObjectCount()` |
| Instance method | `acc1.openAccount()`, `acc1.showBalance()` |
| Overloading | `deposit(double)` and `deposit(double, double...)` |
| Varargs | `double... moreAmounts` |
| Package | `package account;` |
| Built-in import | `import java.util.Arrays;` |
| User-defined import | `import account.Account;` |
| Access modifiers | `public`, `protected`, package-private, `private` in `Account` |
| Default constructor | Covered as comment in `CurrentAccount`: compiler adds it only when no constructor is written |
| No-arg constructor | `Account()` |
| Parameterized constructor | `SavingsAccount(String accountNumber, double balance)` |
| Copy constructor | `SavingsAccount(SavingsAccount other)` |
| Private constructor | `private OopsBasics()` |
| Constructor overloading | Multiple `Account` constructors |
| Encapsulation | `private balance` accessed through public methods |
| Inheritance | `CurrentAccount extends Account`, `SavingsAccount extends Account` |
| Abstract class | `abstract class Account` |
| Interface | `AccountOperation` |
| Abstraction | `Account` abstract class + `AccountOperation` interface |
| Polymorphism | `Account acc1 = new CurrentAccount()` |
| Runtime polymorphism | `acc1.openAccount()` runs `CurrentAccount.openAccount()` |

## Interface And Abstract Class Design Notes

Interfaces and abstract classes are both used for abstraction, but they solve
different design problems.

Simple rule:

```text
Interface      = what an object can do
Abstract class = common code/state for related objects
Concrete class = actual object you create
```

Use an interface when you want to define a behavior contract. It describes a
capability that many different classes can provide, even if those classes are
not closely related.

Use an abstract class when related child classes need shared state, constructors,
or common helper behavior. The abstract class can provide partial implementation,
while child classes complete the specific behavior.

| Need | Prefer |
|---|---|
| Only a capability or contract | Interface |
| Multiple unrelated classes need the same behavior | Interface |
| A class needs multiple capabilities | Interface |
| Code should depend on behavior, not one concrete class | Interface |
| Shared fields or state | Abstract class |
| Shared constructors | Abstract class |
| Shared helper methods | Abstract class |
| Partial implementation plus forced child behavior | Abstract class |

A common Java design shape is:

```text
Capability interface
    implemented by abstract base class
        extended by concrete child classes
```

This means:

```text
Interface:
    defines the required behavior

Abstract base class:
    may implement the interface
    may keep shared data and common logic
    may still leave some behavior for child classes

Concrete class:
    provides the actual object-specific behavior
```

| Item | Convention |
|---|---|
| Package/folder name | lowercase |
| Interface name | PascalCase |
| Abstract class name | PascalCase |
| Concrete class name | PascalCase |
| Method/variable name | camelCase |

When a child object is created, Java initializes the parent part first. A child
constructor can call the parent constructor using `super(...)`, which is common
when shared fields are defined in the abstract base class.

You can also store a concrete object in an interface or parent-class reference.
In that case, the reference type controls what the compiler allows, while the
actual object type decides which overridden method runs at runtime.

## Tricky Interview-Style Questions

| Situation | What Should You Say? |
|---|---|
| You need one shared constant for all accounts. Use instance variable or `static final`? | Use `static final`, like `BANK_NAME`, because the value belongs to the class and should not change. |
| You need every account object to have its own account number. Use static or instance variable? | Use an instance variable. If it is static, all account objects share the same value. |
| You want outside classes to read balance directly. Make `balance` public? | No. Keep it `private` and expose behavior through methods like `showBalance()` or controlled getters. |
| You are outside the `account` package. Can you access `branchCode`? | No. It has package-private access, so only classes inside `account` can access it. |
| A child class needs parent data. Should the field be `private` or `protected`? | Prefer `private` with methods when possible. Use `protected` only when child classes genuinely need direct access. |
| Why use `Account acc1 = new CurrentAccount()` instead of `CurrentAccount acc1 = new CurrentAccount()`? | Use the parent reference when you want polymorphism and flexibility to handle different account types uniformly. |
| Why is `CurrentAccount acc1 = new Account()` invalid? | `Account` is abstract, so it cannot be instantiated. Also, a parent object cannot fit into a child reference. |
| Reference type is `Account`, object type is `CurrentAccount`. Which decides accessible methods? | Reference type controls what the compiler allows. Actual object type decides overridden method execution at runtime. |
| You call `acc1.openAccount()`. Which method runs? | `CurrentAccount.openAccount()` runs because the actual object is `CurrentAccount`. |
| When should you use an interface like `AccountOperation`? | When caller only needs a contract, not the concrete class details. |
| When should you use abstract class instead of interface? | Use abstract class when child classes need shared fields/constructors/common code, like `Account`. Use interface for only a capability contract, like `AccountOperation`. |
| When should you use overloading? | When operations mean the same thing but accept different inputs, like `deposit(500)` and `deposit(100, 200, 300)`. |
| When should you use varargs? | When the caller may pass zero, one, or many values of the same type. Keep it as the last parameter. |
| Why is `main` static? | JVM must start the program without creating an object first. |
| Can `main` be overloaded? | Yes, but JVM starts only `public static void main(String[] args)`. |
| You wrote a constructor in a class. Will compiler still add default constructor? | No. Once you write any constructor, compiler does not add the default no-arg constructor. |
| Why does default constructor exist at all? | So Java can create objects for classes that need no custom initialization. If no constructor is written, compiler adds an empty no-arg constructor. |
| Do we need a separate class only to show default constructor? | No. A comment is enough here because later folders can cover constructors in more depth. |
| Why create a private constructor in `OopsBasics`? | It blocks object creation for a utility-style class that only needs static members and `main`. |
| When would you write a copy constructor? | When you want a new object initialized from an existing object, instead of sharing the same reference. |
| After `acc2 = null`, is garbage collection immediate? | No. The object only becomes eligible for GC if no references point to it; JVM decides when GC runs. |
| Should package name match folder name? | Yes. `package account;` should live under `src/account/` for normal Java project structure. |
| Built-in package vs user-defined package? | `java.util.Arrays` is built-in from JDK. `account.Account` is user-defined in this project. |

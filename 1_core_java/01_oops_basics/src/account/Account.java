package account; //package groups related classes; folder path is src/account/

// Abstract class: can have fields, constructors, concrete methods, and abstract methods.
// Interface AccountOperation only gives the contract.
public abstract class Account implements AccountOperation { //encapsulation: private data + public methods
    public final String accountNumber; //instance final variable: each object gets its own copy, assign once
    protected String accountType = "Generic"; //instance variable + protected: child classes can access
    String branchCode = "BLR"; //instance variable + default/package-private: only account package
    private double balance; //instance variable + private: only this class
    private static int objectCount; //static variable: one shared copy for Account class

    //no-argument constructor: explicit constructor with no parameters
    public Account() {
        this("ACC-1", 1000);
    }

    //parameterized constructor: receives values while creating object
    public Account(String accountNumber, double balance) {
        this.accountNumber = accountNumber;
        this.balance = balance;
        objectCount++;
    }

    //copy constructor: creates a new Account from an existing Account
    public Account(Account other) {
        this(other.accountNumber, other.balance);
    }

    @Override
    public abstract void openAccount();

    public void showBalance() {
        System.out.println("Balance: " + balance); //private field used through public method
    }

    public void deposit(double amount) {
        balance += amount;
        System.out.println("Deposited one amount: " + amount);
    }

    public void deposit(double firstAmount, double... moreAmounts) {
        deposit(firstAmount);

        for (double amount : moreAmounts) {
            balance += amount;
        }

        System.out.println("Extra deposit count: " + moreAmounts.length);
    }

    void showBranchCode() {
        System.out.println("Branch code: " + branchCode);
    }

    public static int getObjectCount() {
        return objectCount;
    }
}

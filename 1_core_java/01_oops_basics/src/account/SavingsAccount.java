package account; //same package as Account, so package-private members are accessible here

// Child class: SavingsAccount is an Account.
public class SavingsAccount extends Account {
    public SavingsAccount() {
        this("SAV-1", 1000);
    }

    public SavingsAccount(String accountNumber, double balance) {
        super(accountNumber, balance);
    }

    public SavingsAccount(SavingsAccount other) {
        super(other);
    }

    @Override
    public void openAccount() {
        System.out.println("Opening savings account");
        System.out.println("Protected accountType from child: " + accountType);
        System.out.println("Package-private branchCode from same package: " + branchCode);
    }
}

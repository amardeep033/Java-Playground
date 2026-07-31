package account; //same package as Account, so package-private members are accessible here

// Child class: CurrentAccount is an Account.
// If this class had no constructor, compiler would add default no-argument constructor.
// But because we wrote CurrentAccount(), compiler will not add one automatically.
public class CurrentAccount extends Account {
    public CurrentAccount() {
        super("CUR-1", 1000);
    }

    @Override
    public void openAccount() {
        System.out.println("Opening current account");
        System.out.println("Protected accountType from child: " + accountType);
        System.out.println("Package-private branchCode from same package: " + branchCode);
        showBranchCode();
        // System.out.println(balance); //private field from Account is not accessible here
    }
}

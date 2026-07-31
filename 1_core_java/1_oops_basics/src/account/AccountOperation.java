package account;

//Abstraction: caller knows what can be done, not how each account does it.
public interface AccountOperation {
    void openAccount();
}

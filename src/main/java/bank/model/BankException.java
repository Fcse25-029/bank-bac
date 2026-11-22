package bank.model;

/** Thrown when a banking rule is broken. The message is shown to the user. */
public class BankException extends Exception {
    public BankException(String message) { super(message); }
}

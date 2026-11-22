package bank.model;

import java.time.LocalDateTime;

/** One line in an account's history: a deposit, withdrawal or interest payment. */
public class Transaction {
    private int transactionID;
    private final String accountNumber;
    private final LocalDateTime date;
    private final TransactionType type;
    private final double amount;
    private final double balanceAfter;
    private String description = "";

    public Transaction(int transactionID, String accountNumber, TransactionType type, double amount, double balanceAfter) {
        this(transactionID, accountNumber, LocalDateTime.now(), type, amount, balanceAfter, "");
    }

    /** Used when loading from the database. */
    public Transaction(int transactionID, String accountNumber, LocalDateTime date, TransactionType type,
                       double amount, double balanceAfter, String description) {
        this.transactionID = transactionID;
        this.accountNumber = accountNumber;
        this.date = date;
        this.type = type;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.description = description;
    }

    public String getDetails() {
        return type + " BWP" + Account.format(amount) + ", balance BWP" + Account.format(balanceAfter);
    }

    public int getTransactionID() { return transactionID; }
    public void setTransactionID(int transactionID) { this.transactionID = transactionID; }
    public String getAccountNumber() { return accountNumber; }
    public LocalDateTime getDate() { return date; }
    public TransactionType getType() { return type; }
    public double getAmount() { return amount; }
    public double getBalanceAfter() { return balanceAfter; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}

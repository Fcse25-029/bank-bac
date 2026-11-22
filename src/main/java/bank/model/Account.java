package bank.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Base class of all accounts. An account cannot exist without a customer (FR-4). */
public abstract class Account {
    private final String accountNumber;
    private String branch;
    private final Customer owner;
    private LocalDate dateOpened = LocalDate.now();
    private String status = "ACTIVE";
    protected double balance;
    private final List<Transaction> transactions = new ArrayList<>();

    public Account(String accountNumber, String branch, Customer owner) {
        if (owner == null) throw new IllegalArgumentException("An account must have a customer.");
        this.accountNumber = accountNumber;
        this.branch = branch;
        this.owner = owner;
    }

    public Account(String accountNumber, String branch, Customer owner, double initialDeposit) throws BankException {
        this(accountNumber, branch, owner);
        if (initialDeposit > 0) deposit(initialDeposit, "Initial deposit");
    }

    public abstract String getType();

    /** Each account type decides if and how money can be withdrawn. */
    public abstract Transaction withdraw(double amount) throws BankException;

    public Transaction deposit(double amount) throws BankException {
        return deposit(amount, "");
    }

    public Transaction deposit(double amount, String description) throws BankException {
        checkAmount(amount);
        balance = round(balance + amount);
        return record(TransactionType.DEPOSIT, amount, description);
    }

    /** Shared by investment and cheque accounts: rejects a withdrawal above the balance. */
    protected Transaction takeOut(double amount) throws BankException {
        checkAmount(amount);
        if (amount > balance) {
            throw new BankException("Insufficient funds. Your balance is BWP" + format(balance) + ".");
        }
        balance = round(balance - amount);
        return record(TransactionType.WITHDRAWAL, amount, "");
    }

    protected Transaction record(TransactionType type, double amount, String description) {
        Transaction t = new Transaction(0, accountNumber, type, amount, balance);
        t.setDescription(description);
        transactions.add(t);
        return t;
    }

    /** Amounts must be above zero with at most 2 decimal places (NFR-5). */
    protected void checkAmount(double amount) throws BankException {
        if (amount <= 0) throw new BankException("Amount must be greater than zero.");
        if (Math.abs(amount * 100 - Math.round(amount * 100)) > 0.000001) {
            throw new BankException("Amount can have at most 2 decimal places.");
        }
    }

    public static double round(double value) { return Math.round(value * 100.0) / 100.0; }
    public static String format(double value) { return String.format("%.2f", value); }

    /** Used when loading from the database. */
    public void addTransaction(Transaction t) { transactions.add(t); }

    public List<Transaction> getTransactionHistory() { return Collections.unmodifiableList(transactions); }
    public Transaction getLastTransaction() { return transactions.isEmpty() ? null : transactions.get(transactions.size() - 1); }

    @Override
    public String toString() { return getType() + " " + accountNumber + " (BWP" + format(balance) + ")"; }

    public String getAccountNumber() { return accountNumber; }
    public double getBalance() { return balance; }
    public void setBalance(double balance) { this.balance = round(balance); }
    public String getBranch() { return branch; }
    public void setBranch(String branch) { this.branch = branch; }
    public LocalDate getDateOpened() { return dateOpened; }
    public void setDateOpened(LocalDate dateOpened) { this.dateOpened = dateOpened; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Customer getOwner() { return owner; }
}

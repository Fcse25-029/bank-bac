package bank.controller;

import bank.dao.AccountDAO;
import bank.model.*;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Deposit, withdraw and history for a logged-in customer. */
public class AccountController {
    private static final double SESSION_LIMIT = 5000.00;       // NFR-6
    private final AccountDAO accountDAO;
    private double withdrawnThisSession;

    public AccountController(AccountDAO accountDAO) { this.accountDAO = accountDAO; }

    /** Called when a customer logs in: the BWP5000 withdrawal allowance starts again. */
    public void startSession() { withdrawnThisSession = 0; }

    public List<Account> getAccounts(Customer customer) { return new ArrayList<>(customer.getAccounts()); }

    public void deposit(Customer customer, String accountNumber, String amountText) throws BankException {
        Account account = find(customer, accountNumber);
        Transaction t = account.deposit(parseAmount(amountText));
        save(account, t);
    }

    public void withdraw(Customer customer, String accountNumber, String amountText, String pin) throws BankException {
        Account account = find(customer, accountNumber);
        double amount = parseAmount(amountText);
        if (!customer.verifyPin(pin)) throw new BankException("Incorrect PIN. The withdrawal is not authorised.");
        if (withdrawnThisSession + amount > SESSION_LIMIT) {
            throw new BankException("You can withdraw at most BWP" + Account.format(SESSION_LIMIT)
                    + " in one login session. Withdrawn so far: BWP" + Account.format(withdrawnThisSession) + ".");
        }
        Transaction t = account.withdraw(amount);    // the account decides if this is allowed
        withdrawnThisSession += amount;
        save(account, t);
    }

    public List<Transaction> getHistory(Customer customer, String accountNumber) throws BankException {
        return new ArrayList<>(find(customer, accountNumber).getTransactionHistory());
    }

    // ---- helpers ----
    private Account find(Customer customer, String accountNumber) throws BankException {
        Account a = customer.findAccount(accountNumber);
        if (a == null) throw new BankException("Account not found.");
        return a;
    }

    private void save(Account account, Transaction t) throws BankException {
        try {
            accountDAO.update(account);
            accountDAO.saveTransaction(t);
        } catch (SQLException e) {
            throw new BankException("Database error: " + e.getMessage());
        }
    }

    private double parseAmount(String text) throws BankException {
        if (text == null || text.trim().isEmpty()) throw new BankException("Enter an amount.");
        try {
            return Double.parseDouble(text.trim());
        } catch (NumberFormatException e) {
            throw new BankException("Amount must be a number, for example 250.50");
        }
    }
}

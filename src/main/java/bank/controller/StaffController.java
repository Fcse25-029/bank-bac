package bank.controller;

import bank.dao.AccountDAO;
import bank.model.*;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** What bank staff can do: open accounts for customers and pay monthly interest. */
public class StaffController {
    private final Bank bank;
    private final AccountDAO accountDAO;

    public StaffController(Bank bank, AccountDAO accountDAO) {
        this.bank = bank;
        this.accountDAO = accountDAO;
    }

    public List<Customer> getCustomers() { return new ArrayList<>(bank.getCustomers()); }

    /** Opens an account on behalf of a customer (found by customer ID or full name). */
    public Account openAccount(String customerIdOrName, String type, String depositText) throws BankException {
        String key = String.valueOf(customerIdOrName).trim();
        Customer customer = bank.findCustomerById(key);
        if (customer == null) customer = bank.findCustomer(key);
        double deposit = 0;
        if (depositText != null && !depositText.trim().isEmpty()) {
            try {
                deposit = Double.parseDouble(depositText.trim());
            } catch (NumberFormatException e) {
                throw new BankException("Initial deposit must be a number, for example 500.00");
            }
        }
        Account account = bank.openAccount(customer, type, deposit);
        try {
            accountDAO.create(account);
            if (account.getLastTransaction() != null) accountDAO.saveTransaction(account.getLastTransaction());
        } catch (SQLException e) {
            throw new BankException("Database error: " + e.getMessage());
        }
        return account;
    }

    /**
     * Pays this month's interest. Saving each payment is tried up to 3 times; if all fail the
     * process stops and staff are told (see the PayInterest state diagram). Returns accounts paid.
     */
    public int payMonthlyInterest() throws BankException {
        List<Transaction> paid = bank.payMonthlyInterest();
        for (Transaction t : paid) {
            Account account = bank.findAccount(t.getAccountNumber());
            boolean saved = false;
            for (int attempt = 1; attempt <= 3 && !saved; attempt++) {
                try {
                    accountDAO.update(account);
                    accountDAO.saveTransaction(t);
                    saved = true;
                } catch (SQLException e) {
                    saved = false;
                }
            }
            if (!saved) {
                throw new BankException("Interest payment aborted after 3 attempts on account "
                        + t.getAccountNumber() + ". Bank staff must check it.");
            }
        }
        return paid.size();
    }
}

package bank.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** The bank. Its staff register customers, open accounts for them and run the monthly interest. */
public class Bank {
    private final String bankName;
    private final String branchCode;
    private final List<Customer> customers = new ArrayList<>();

    public Bank(String bankName, String branchCode) {
        this.bankName = bankName;
        this.branchCode = branchCode;
    }

    /** Next unique customer ID: CUS0001, CUS0002 ... */
    public String nextCustomerId() { return String.format("CUS%04d", customers.size() + 1); }

    /** Adds a new customer after checking that nothing that must be unique is already used. */
    public void registerCustomer(Customer customer) throws BankException {
        if (findCustomer(customer.getDisplayName()) != null) {
            throw new BankException("A customer with that name is already registered.");
        }
        for (Customer c : customers) {
            if (customer instanceof CompanyCustomer && c instanceof CompanyCustomer
                    && ((CompanyCustomer) customer).getRegistrationNumber().equals(((CompanyCustomer) c).getRegistrationNumber())) {
                throw new BankException("This registration number is already registered.");
            }
        }
        customer.register();
        customers.add(customer);
    }

    /** Used when loading from the database. */
    public void addExistingCustomer(Customer customer) { customers.add(customer); }

    /** Finds a customer by full name (or company name). */
    public Customer findCustomer(String name) {
        for (Customer c : customers) if (c.getDisplayName().equalsIgnoreCase(String.valueOf(name).trim())) return c;
        return null;
    }

    public Customer findCustomerById(String customerID) {
        for (Customer c : customers) if (c.getCustomerID().equalsIgnoreCase(customerID)) return c;
        return null;
    }

    /** Opens an account that starts with a balance of 0.00. Called by bank staff. */
    public Account openAccount(Customer customer, String type) throws BankException {
        return openAccount(customer, type, 0);
    }

    /** Called by bank staff only (FR-18). type is "Savings", "Investment" or "Cheque". */
    public Account openAccount(Customer customer, String type, double initialDeposit) throws BankException {
        if (customer == null) throw new BankException("Customer not found. Register the customer first.");
        String number = branchCode + String.format("%06d", getAllAccounts().size() + 1);
        Account account;
        if ("Savings".equals(type)) {
            account = new SavingsAccount(number, branchCode, customer);
            if (initialDeposit > 0) account.deposit(initialDeposit, "Initial deposit");
        } else if ("Investment".equals(type)) {
            account = new InvestmentAccount(number, branchCode, customer, initialDeposit);
        } else if ("Cheque".equals(type)) {
            account = new ChequeAccount(number, branchCode, customer, "", "");
            if (initialDeposit > 0) account.deposit(initialDeposit, "Initial deposit");
        } else {
            throw new BankException("Choose an account type.");
        }
        customer.addAccount(account);
        return account;
    }

    /** Pays this month's interest on every savings and investment account. Cheque accounts are skipped. */
    public List<Transaction> payMonthlyInterest() {
        List<Transaction> paid = new ArrayList<>();
        for (Account a : getAllAccounts()) {
            if (a instanceof InterestBearing) {
                Transaction t = ((InterestBearing) a).payInterest();
                if (t != null) paid.add(t);
            }
        }
        return paid;
    }

    public List<Account> getAllAccounts() {
        List<Account> all = new ArrayList<>();
        for (Customer c : customers) all.addAll(c.getAccounts());
        return all;
    }

    public Account findAccount(String accountNumber) {
        for (Account a : getAllAccounts()) if (a.getAccountNumber().equals(accountNumber)) return a;
        return null;
    }

    public String getBankName() { return bankName; }
    public String getBranchCode() { return branchCode; }
    public List<Customer> getCustomers() { return Collections.unmodifiableList(customers); }
}

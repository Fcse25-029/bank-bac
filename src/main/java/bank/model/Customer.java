package bank.model;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A customer of the bank: either an IndividualCustomer or a CompanyCustomer.
 * A customer logs in with their full name (or company name) and a 4 digit PIN.
 */
public abstract class Customer {
    private final String customerID;
    private String address;
    private String phone;
    private String email;
    private String pinHash;
    private LocalDate registrationDate = LocalDate.now();
    private int failedLoginAttempts;
    private boolean loggedIn;
    private final List<Account> accounts = new ArrayList<>();

    public Customer(String customerID, String address, String phone, String email) {
        this.customerID = customerID;
        this.address = address;
        this.phone = phone;
        this.email = email;
    }

    /** The full name of a person, or the name of a company. Used to log in. */
    public abstract String getDisplayName();

    public void register() { registrationDate = LocalDate.now(); }

    /** Checks the login with name and PIN. Three wrong attempts lock the login (NFR-9). */
    public void login(String name, String pin) throws BankException {
        if (isLocked()) throw new BankException("This login is locked after 3 failed attempts. Please see bank staff.");
        if (name != null && getDisplayName().equalsIgnoreCase(name.trim()) && verifyPin(pin)) {
            failedLoginAttempts = 0;
            loggedIn = true;
            return;
        }
        failedLoginAttempts++;
        throw new BankException(isLocked() ? "Wrong PIN. The login is now locked. Please see bank staff."
                : "Wrong name or PIN.");
    }

    public void logout() { loggedIn = false; }
    public boolean isLocked() { return failedLoginAttempts >= 3; }
    public void unlock() { failedLoginAttempts = 0; }

    /** The 4 digit PIN used to log in and to withdraw (NFR-7). */
    public void setPin(String pin) throws BankException {
        if (pin == null || !pin.matches("\\d{4}")) throw new BankException("PIN must be exactly 4 digits.");
        pinHash = hash(pin);
    }

    public boolean verifyPin(String pin) { return pinHash != null && pin != null && hash(pin).equals(pinHash); }

    public void addAccount(Account account) { accounts.add(account); }

    public Account findAccount(String accountNumber) {
        for (Account a : accounts) if (a.getAccountNumber().equals(accountNumber)) return a;
        return null;
    }

    /** The PIN is saved as a SHA-256 hash, never as plain text (NFR-8). */
    public static String hash(String text) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256").digest(String.valueOf(text).getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /** Used when loading from the database. */
    public void restore(String pinHash, LocalDate registrationDate, int failedLoginAttempts) {
        this.pinHash = pinHash;
        this.registrationDate = registrationDate;
        this.failedLoginAttempts = failedLoginAttempts;
    }

    public String getCustomerID() { return customerID; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPinHash() { return pinHash; }
    public LocalDate getRegistrationDate() { return registrationDate; }
    public int getFailedLoginAttempts() { return failedLoginAttempts; }
    public boolean isLoggedIn() { return loggedIn; }
    public List<Account> getAccounts() { return Collections.unmodifiableList(accounts); }
}

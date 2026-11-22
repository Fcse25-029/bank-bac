package bank.controller;

import bank.dao.CustomerDAO;
import bank.model.*;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/** Registration and login (name and PIN) for customers and bank staff. */
public class LoginController {
    private final Bank bank;
    private final CustomerDAO customerDAO;

    public LoginController(Bank bank, CustomerDAO customerDAO) {
        this.bank = bank;
        this.customerDAO = customerDAO;
    }

    /** A customer logs in with their full name (or company name) and PIN. */
    public Customer loginCustomer(String name, String pin) throws BankException {
        if (isEmpty(name) || isEmpty(pin)) throw new BankException("Enter your name and PIN.");
        Customer customer = bank.findCustomer(name);
        if (customer == null) throw new BankException("Wrong name or PIN.");
        try {
            customer.login(name, pin);
        } catch (BankException e) {
            save(customer);          // remember the failed attempt (it can lock the login)
            throw e;
        }
        save(customer);
        return customer;
    }

    public void logout(Customer customer) { customer.logout(); }

    /** Bank staff log in with one fixed account to keep things simple: Staff / 1234. */
    public void loginStaff(String name, String pin) throws BankException {
        if (!"Staff".equalsIgnoreCase(name == null ? "" : name.trim()) || !"1234".equals(pin)) {
            throw new BankException("Wrong staff name or PIN.");
        }
    }

    public Customer registerIndividual(String fullName, String dateOfBirth, String phone, String pin)
            throws BankException {
        require(fullName, "Full name");
        LocalDate dob = parseDate(dateOfBirth);
        require(phone, "Phone");
        return finish(new IndividualCustomer(bank.nextCustomerId(), fullName.trim(), dob, phone.trim()), pin);
    }

    public Customer registerCompany(String companyName, String registrationNumber, String phone, String pin)
            throws BankException {
        require(companyName, "Company name");
        require(registrationNumber, "Registration number");
        require(phone, "Phone");
        return finish(new CompanyCustomer(bank.nextCustomerId(), companyName.trim(), registrationNumber.trim(),
                phone.trim()), pin);
    }

    private Customer finish(Customer c, String pin) throws BankException {
        c.setPin(pin);                 // must be 4 digits
        bank.registerCustomer(c);      // the name (and a company registration number) must be unique
        try {
            customerDAO.create(c);
        } catch (SQLException e) {
            throw new BankException("Database error: " + e.getMessage());
        }
        return c;
    }

    private void save(Customer c) throws BankException {
        try {
            customerDAO.update(c);
        } catch (SQLException e) {
            throw new BankException("Database error: " + e.getMessage());
        }
    }

    private static void require(String value, String field) throws BankException {
        if (isEmpty(value)) throw new BankException(field + " is required.");
    }

    private static LocalDate parseDate(String text) throws BankException {
        require(text, "Date of birth");
        try {
            return LocalDate.parse(text.trim());
        } catch (DateTimeParseException e) {
            throw new BankException("Date of birth must look like 1990-05-21.");
        }
    }

    private static boolean isEmpty(String s) { return s == null || s.trim().isEmpty(); }
}

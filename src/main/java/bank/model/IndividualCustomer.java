package bank.model;

import java.time.LocalDate;

/** A single person. */
public class IndividualCustomer extends Customer {
    private String fullName;
    private final LocalDate dateOfBirth;

    public IndividualCustomer(String customerID, String fullName, LocalDate dateOfBirth, String phone) {
        super(customerID, "", phone, "");
        this.fullName = fullName;
        this.dateOfBirth = dateOfBirth;
    }

    @Override public String getDisplayName() { return fullName; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public LocalDate getDateOfBirth() { return dateOfBirth; }
}

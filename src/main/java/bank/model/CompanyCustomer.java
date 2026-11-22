package bank.model;

/** A company. */
public class CompanyCustomer extends Customer {
    private String companyName;
    private final String registrationNumber;

    public CompanyCustomer(String customerID, String companyName, String registrationNumber, String phone) {
        super(customerID, "", phone, "");
        this.companyName = companyName;
        this.registrationNumber = registrationNumber;
    }

    @Override public String getDisplayName() { return companyName; }

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }
    public String getRegistrationNumber() { return registrationNumber; }
}

package bank.model;

import java.time.LocalDate;
import java.time.YearMonth;

/** Opened with at least BWP500.00, earns 5% interest a month, withdrawals allowed (FR-5, FR-9, FR-11). */
public class InvestmentAccount extends Account implements InterestBearing {
    private double interestRate = 0.05;
    private final double minimumOpeningDeposit = 500.00;
    private LocalDate lastInterestDate;

    /** Used when loading an existing account from the database. */
    public InvestmentAccount(String accountNumber, String branch, Customer owner) {
        super(accountNumber, branch, owner);
    }

    /** Opening a new account: an initial deposit below BWP500.00 is rejected. */
    public InvestmentAccount(String accountNumber, String branch, Customer owner, double initialDeposit)
            throws BankException {
        super(accountNumber, branch, owner);
        if (initialDeposit < minimumOpeningDeposit) {
            throw new BankException("An investment account needs an initial deposit of at least BWP"
                    + format(minimumOpeningDeposit) + ".");
        }
        deposit(initialDeposit, "Initial deposit");
    }

    @Override public String getType() { return "Investment"; }

    @Override
    public Transaction withdraw(double amount) throws BankException {
        return takeOut(amount);
    }

    @Override public double getInterestRate() { return interestRate; }
    @Override public double calculateInterest() { return round(balance * interestRate); }

    @Override
    public Transaction payInterest() {
        LocalDate today = LocalDate.now();
        boolean alreadyPaid = lastInterestDate != null && YearMonth.from(lastInterestDate).equals(YearMonth.from(today));
        double interest = calculateInterest();
        if (alreadyPaid || interest <= 0) return null;
        balance = round(balance + interest);
        lastInterestDate = today;
        return record(TransactionType.INTEREST, interest, "Monthly interest");
    }

    public void setInterestRate(double interestRate) { this.interestRate = interestRate; }
    public double getMinimumOpeningDeposit() { return minimumOpeningDeposit; }
    public LocalDate getLastInterestDate() { return lastInterestDate; }
    public void setLastInterestDate(LocalDate lastInterestDate) { this.lastInterestDate = lastInterestDate; }
}

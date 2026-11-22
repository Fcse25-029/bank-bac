package bank.model;

import java.time.LocalDate;
import java.time.YearMonth;

/** Earns 0.05% interest a month. Withdrawals are never allowed (FR-8, FR-11). */
public class SavingsAccount extends Account implements InterestBearing {
    private double interestRate = 0.0005;
    private LocalDate lastInterestDate;

    public SavingsAccount(String accountNumber, String branch, Customer owner) {
        super(accountNumber, branch, owner);
    }

    @Override public String getType() { return "Savings"; }

    @Override
    public Transaction withdraw(double amount) throws BankException {
        throw new BankException("You cannot withdraw from a savings account.");
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
    public LocalDate getLastInterestDate() { return lastInterestDate; }
    public void setLastInterestDate(LocalDate lastInterestDate) { this.lastInterestDate = lastInterestDate; }
}

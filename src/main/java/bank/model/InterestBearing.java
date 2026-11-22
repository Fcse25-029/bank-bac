package bank.model;

/** Implemented by the accounts that earn interest (savings and investment). */
public interface InterestBearing {
    double getInterestRate();

    /** One month of interest on the current balance. */
    double calculateInterest();

    /** Adds this month's interest. Returns the transaction, or null if nothing was paid. */
    Transaction payInterest();
}

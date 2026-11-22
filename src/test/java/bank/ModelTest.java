package bank;

import bank.model.*;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/** Tests for the core classes. No database and no screens are involved. */
class ModelTest {
    private Customer person() {
        Customer c = new IndividualCustomer("CUS0001", "Thato Moyo", LocalDate.of(1995, 5, 21), "71234567");
        try {
            c.setPin("1234");
        } catch (BankException e) {
            throw new IllegalStateException(e);
        }
        return c;
    }

    @Test void depositAddsToBalanceAndRecordsTransaction() throws Exception {
        Account a = new SavingsAccount("A1", "1001", person());
        a.deposit(100.50);
        assertEquals(100.50, a.getBalance());
        assertEquals(1, a.getTransactionHistory().size());
    }

    @Test void invalidAmountsAreRejected() throws Exception {
        Account a = new ChequeAccount("A1", "1001", person(), "Acme", "Gaborone");
        assertThrows(BankException.class, () -> a.deposit(0));
        assertThrows(BankException.class, () -> a.deposit(-5));
        assertThrows(BankException.class, () -> a.deposit(10.123));      // 2 decimal places only
    }

    @Test void savingsNeverAllowsWithdrawals() throws Exception {
        Account a = new SavingsAccount("A1", "1001", person());
        a.deposit(100);
        assertThrows(BankException.class, () -> a.withdraw(10));
        assertEquals(100, a.getBalance());
    }

    @Test void investmentAndChequeRejectWithdrawalsAboveBalance() throws Exception {
        Customer p = person();
        Account inv = new InvestmentAccount("A1", "1001", p, 500);
        Account chq = new ChequeAccount("A2", "1001", p, "Acme", "Gaborone");
        chq.deposit(100);
        inv.withdraw(200);
        chq.withdraw(100);
        assertEquals(300, inv.getBalance());
        assertEquals(0, chq.getBalance());
        assertThrows(BankException.class, () -> inv.withdraw(300.01));
    }

    @Test void investmentNeedsAtLeast500() {
        assertThrows(BankException.class, () -> new InvestmentAccount("A1", "1001", person(), 499.99));
        assertDoesNotThrow(() -> new InvestmentAccount("A1", "1001", person(), 500));
    }

    @Test void accountNeedsACustomer() {
        assertThrows(IllegalArgumentException.class, () -> new SavingsAccount("A1", "1001", null));
    }

    @Test void interestRatesAndOncePerMonth() throws Exception {
        Customer p = person();
        SavingsAccount s = new SavingsAccount("A1", "1001", p);
        s.deposit(1000);
        InvestmentAccount i = new InvestmentAccount("A2", "1001", p, 1000);
        assertEquals(0.50, s.payInterest().getAmount());      // 0.05%
        assertEquals(50.00, i.payInterest().getAmount());     // 5%
        assertNull(s.payInterest());                          // already paid this month
    }

    @Test void loginWithNameAndPinLocksAfterThreeFailedAttempts() throws Exception {
        Customer c = person();
        c.login("thato moyo", "1234");                                   // the name is not case sensitive
        assertTrue(c.isLoggedIn());
        for (int i = 0; i < 3; i++) assertThrows(BankException.class, () -> c.login("Thato Moyo", "0000"));
        assertTrue(c.isLocked());
        assertThrows(BankException.class, () -> c.login("Thato Moyo", "1234"));   // locked even with the right PIN
        c.unlock();
        c.login("Thato Moyo", "1234");
        assertTrue(c.isLoggedIn());
        assertThrows(BankException.class, () -> c.login("Someone Else", "1234")); // wrong name
    }

    @Test void pinHasFourDigitsAndIsStoredHashed() throws Exception {
        Customer c = person();
        assertThrows(BankException.class, () -> c.setPin("123"));
        assertThrows(BankException.class, () -> c.setPin("12a4"));
        assertTrue(c.verifyPin("1234"));
        assertFalse(c.verifyPin("0000"));
        assertNotEquals("1234", c.getPinHash());
    }

    @Test void bankOpensAccountsWithAnInitialDeposit() throws Exception {
        Bank bank = new Bank("BANK BAC", "1001");
        Customer c = person();
        bank.registerCustomer(c);
        assertEquals(250.0, bank.openAccount(c, "Savings", 250).getBalance());
        assertEquals(100.0, bank.openAccount(c, "Cheque", 100).getBalance());
        assertEquals(500.0, bank.openAccount(c, "Investment", 500).getBalance());
        assertEquals(0.0, bank.openAccount(c, "Savings", 0).getBalance());
        assertEquals(1, c.getAccounts().get(0).getTransactionHistory().size());     // the deposit is recorded
        assertThrows(BankException.class, () -> bank.openAccount(c, "Savings", 10.123));
        assertEquals(4, c.getAccounts().size());                                    // the rejected one was not added
    }
}

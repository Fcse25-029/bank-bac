package bank;

import bank.controller.AccountController;
import bank.controller.LoginController;
import bank.controller.StaffController;
import bank.dao.AccountDAO;
import bank.dao.CustomerDAO;
import bank.dao.Database;
import bank.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests the whole chain: controller -> core model -> DAO -> H2 database.
 * The screens only call these controller methods, so each test is a full user flow.
 * After a flow the Bank is rebuilt from the database to prove the data was saved.
 */
class IntegrationTest {
    private Database db;
    private Bank bank;
    private LoginController login;
    private StaffController staff;
    private AccountController accounts;

    @BeforeEach
    void setUp() throws Exception {
        db = Database.inMemory("test" + UUID.randomUUID().toString().replace("-", ""));
        db.createTables();
        bank = db.loadBank("BANK BAC", "1001");
        CustomerDAO customerDAO = new CustomerDAO(db);
        AccountDAO accountDAO = new AccountDAO(db);
        login = new LoginController(bank, customerDAO);
        staff = new StaffController(bank, accountDAO);
        accounts = new AccountController(accountDAO);
    }

    private Customer registerThato() throws Exception {
        return login.registerIndividual("Thato Moyo", "1995-05-21", "71234567", "1234");
    }

    private Bank reload() throws Exception { return db.loadBank("BANK BAC", "1001"); }

    @Test
    void IT1_registerIndividualAndCompanyThenLoginWithNameAndPin() throws Exception {
        Customer person = registerThato();
        Customer company = login.registerCompany("Kalahari Traders", "BW001", "2412345", "4321");
        assertEquals("CUS0001", person.getCustomerID());
        assertEquals("CUS0002", company.getCustomerID());

        Bank again = reload();
        assertTrue(again.findCustomer("Thato Moyo") instanceof IndividualCustomer);
        assertTrue(again.findCustomer("Kalahari Traders") instanceof CompanyCustomer);
        assertNotEquals("1234", again.findCustomer("Thato Moyo").getPinHash());        // the PIN is stored as a hash

        assertEquals("CUS0001", login.loginCustomer("Thato Moyo", "1234").getCustomerID());
        assertEquals("CUS0002", login.loginCustomer("Kalahari Traders", "4321").getCustomerID());
        assertThrows(BankException.class, () -> login.loginCustomer("Thato Moyo", "4321"));   // wrong PIN
        assertThrows(BankException.class, () -> login.loginCustomer("Nobody", "1234"));       // unknown name
        assertThrows(BankException.class, () -> login.registerIndividual("Thato Moyo", "1990-01-01", "70000000", "1234")); // name taken
        assertThrows(BankException.class, () -> login.registerIndividual("Other Person", "1990-01-01", "70000000", "12"));  // bad PIN
    }

    @Test
    void IT2_threeFailedLoginsLockTheCustomer() throws Exception {
        registerThato();
        for (int i = 0; i < 3; i++) assertThrows(BankException.class, () -> login.loginCustomer("Thato Moyo", "0000"));
        assertThrows(BankException.class, () -> login.loginCustomer("Thato Moyo", "1234"));   // locked
        assertTrue(reload().findCustomer("Thato Moyo").isLocked());                            // the lock was saved
    }

    @Test
    void IT3_staffOpenAccountsWithAnInitialDeposit() throws Exception {
        registerThato();
        login.registerCompany("Kalahari Traders", "BW001", "2412345", "4321");

        Account savings = staff.openAccount("CUS0001", "Savings", "250");
        Account cheque = staff.openAccount("Thato Moyo", "Cheque", "100.50");
        Account investment = staff.openAccount("CUS0001", "Investment", "500");
        Account empty = staff.openAccount("CUS0001", "Savings", "");
        staff.openAccount("CUS0002", "Cheque", "");                                    // a company can have accounts too

        assertThrows(BankException.class, () -> staff.openAccount("CUS0001", "Investment", "499.99"));
        assertThrows(BankException.class, () -> staff.openAccount("CUS9999", "Savings", ""));
        assertThrows(BankException.class, () -> staff.openAccount("CUS0001", "Savings", "abc"));

        Bank again = reload();
        assertEquals(4, again.findCustomer("Thato Moyo").getAccounts().size());        // rejected attempts saved nothing
        assertEquals(250.0, again.findAccount(savings.getAccountNumber()).getBalance());
        assertEquals(100.5, again.findAccount(cheque.getAccountNumber()).getBalance());
        assertEquals(500.0, again.findAccount(investment.getAccountNumber()).getBalance());
        assertEquals(0.0, again.findAccount(empty.getAccountNumber()).getBalance());
        assertEquals(1, again.findAccount(savings.getAccountNumber()).getTransactionHistory().size()); // the deposit is in the history
    }

    @Test
    void IT4_fullFlow_deposit_withdraw_historySavedInDatabase() throws Exception {
        registerThato();
        Account cheque = staff.openAccount("CUS0001", "Cheque", "");
        Customer c = login.loginCustomer("Thato Moyo", "1234");
        accounts.startSession();

        accounts.deposit(c, cheque.getAccountNumber(), "250.50");
        accounts.withdraw(c, cheque.getAccountNumber(), "50.25", "1234");

        Account saved = reload().findAccount(cheque.getAccountNumber());
        assertEquals(200.25, saved.getBalance());
        List<Transaction> history = saved.getTransactionHistory();
        assertEquals(2, history.size());
        assertEquals(TransactionType.DEPOSIT, history.get(0).getType());
        assertEquals(TransactionType.WITHDRAWAL, history.get(1).getType());
        assertEquals(200.25, history.get(1).getBalanceAfter());
        assertNotNull(history.get(1).getDate());
    }

    @Test
    void IT5_withdrawalRulesAndSessionLimit() throws Exception {
        registerThato();
        Account savings = staff.openAccount("CUS0001", "Savings", "");
        Account investment = staff.openAccount("CUS0001", "Investment", "20000");
        Customer c = login.loginCustomer("Thato Moyo", "1234");
        accounts.startSession();
        accounts.deposit(c, savings.getAccountNumber(), "200");

        assertThrows(BankException.class, () -> accounts.withdraw(c, savings.getAccountNumber(), "10", "1234"));    // savings
        assertThrows(BankException.class, () -> accounts.withdraw(c, investment.getAccountNumber(), "10", "9999")); // wrong PIN
        assertThrows(BankException.class, () -> accounts.withdraw(c, investment.getAccountNumber(), "abc", "1234"));// not a number

        accounts.withdraw(c, investment.getAccountNumber(), "3000", "1234");
        assertThrows(BankException.class, () -> accounts.withdraw(c, investment.getAccountNumber(), "2500", "1234")); // over BWP5000
        accounts.withdraw(c, investment.getAccountNumber(), "2000", "1234");

        Bank again = reload();
        assertEquals(200.0, again.findAccount(savings.getAccountNumber()).getBalance());
        assertEquals(15000.0, again.findAccount(investment.getAccountNumber()).getBalance());

        accounts.startSession();                                                        // a new login = a new allowance
        assertDoesNotThrow(() -> accounts.withdraw(c, investment.getAccountNumber(), "100", "1234"));
    }

    @Test
    void IT6_overdrawingIsRejected() throws Exception {
        registerThato();
        Account cheque = staff.openAccount("CUS0001", "Cheque", "");
        Customer c = login.loginCustomer("Thato Moyo", "1234");
        accounts.startSession();
        accounts.deposit(c, cheque.getAccountNumber(), "100");
        assertThrows(BankException.class, () -> accounts.withdraw(c, cheque.getAccountNumber(), "100.01", "1234"));
        accounts.withdraw(c, cheque.getAccountNumber(), "100.00", "1234");
        assertEquals(0.0, reload().findAccount(cheque.getAccountNumber()).getBalance());
    }

    @Test
    void IT7_monthlyInterestIsPaidOncePerMonthAndSaved() throws Exception {
        registerThato();
        Account savings = staff.openAccount("CUS0001", "Savings", "1000");
        Account investment = staff.openAccount("CUS0001", "Investment", "1000");
        Account cheque = staff.openAccount("CUS0001", "Cheque", "1000");

        assertEquals(2, staff.payMonthlyInterest());
        assertEquals(0, staff.payMonthlyInterest());                                   // already paid this month

        Bank again = reload();
        assertEquals(1000.50, again.findAccount(savings.getAccountNumber()).getBalance());     // 0.05%
        assertEquals(1050.00, again.findAccount(investment.getAccountNumber()).getBalance());  // 5%
        assertEquals(1000.00, again.findAccount(cheque.getAccountNumber()).getBalance());      // none
    }
}

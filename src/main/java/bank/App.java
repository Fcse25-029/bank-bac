package bank;

import bank.controller.AccountController;
import bank.controller.LoginController;
import bank.controller.StaffController;
import bank.dao.AccountDAO;
import bank.dao.CustomerDAO;
import bank.dao.Database;
import bank.model.Bank;
import bank.model.Customer;
import bank.view.AccountView;
import bank.view.LoginView;
import bank.view.StaffView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.util.Timer;
import java.util.TimerTask;

/** Starts the program and connects the layers: database -> Bank -> DAOs -> controllers -> screens. */
public class App extends Application {
    private LoginController loginController;
    private AccountController accountController;
    private StaffController staffController;
    private Stage stage;

    @Override
    public void init() throws Exception {
        Database db = Database.mysql();           // XAMPP MySQL (start MySQL in the XAMPP Control Panel first)
        try {
            db.createTables();
            System.out.println("Connected to MySQL (XAMPP), database bank_bac.");
        } catch (java.sql.SQLException e) {
            System.err.println("Could not connect to MySQL: " + e.getMessage());
            System.err.println("Is MySQL started in XAMPP? Using the local H2 file database instead.");
            db = Database.file();
            db.createTables();
        }
        Bank bank = db.loadBank("Botswana Bank", "1001");
        CustomerDAO customerDAO = new CustomerDAO(db);
        AccountDAO accountDAO = new AccountDAO(db);
        loginController = new LoginController(bank, customerDAO);
        accountController = new AccountController(accountDAO);
        staffController = new StaffController(bank, accountDAO);
        startScheduler();
    }

    /** The System Scheduler: every 6 hours it checks if today is the last day of the month and pays interest. */
    private void startScheduler() {
        Timer timer = new Timer("interest-scheduler", true);
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                LocalDate today = LocalDate.now();
                if (today.getDayOfMonth() == today.lengthOfMonth()) {
                    try {
                        staffController.payMonthlyInterest();
                    } catch (Exception e) {
                        System.err.println("Scheduled interest failed: " + e.getMessage());
                    }
                }
            }
        }, 60_000, 6 * 60 * 60 * 1000);
    }

    @Override
    public void start(Stage stage) {
        this.stage = stage;
        stage.setTitle("BANK BAC");
        showLogin();
        stage.show();
    }

    private void showLogin() {
        LoginView view = new LoginView(loginController, this::showCustomer, this::showStaff);
        stage.setScene(new Scene(view.getRoot(), 520, 420));
    }

    private void showCustomer(Customer customer) {
        AccountView view = new AccountView(accountController, loginController, customer, this::showLogin);
        stage.setScene(new Scene(view.getRoot(), 860, 480));
    }

    private void showStaff() {
        StaffView view = new StaffView(staffController, loginController, this::showLogin);
        stage.setScene(new Scene(view.getRoot(), 920, 500));
    }

    public static void main(String[] args) { launch(args); }
}

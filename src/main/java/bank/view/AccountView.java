package bank.view;

import bank.controller.AccountController;
import bank.controller.LoginController;
import bank.model.Account;
import bank.model.Customer;
import bank.model.Transaction;
import javafx.animation.PauseTransition;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.util.Duration;

import java.time.format.DateTimeFormatter;
import java.util.function.Function;

/** The customer's screen: accounts, deposit, withdraw and history. Customers cannot open accounts. */
public class AccountView {
    private final BorderPane root = new BorderPane();
    private final TableView<Account> table = new TableView<>();
    private final Label message = new Label();
    private final AccountController controller;
    private final Customer customer;

    public AccountView(AccountController controller, LoginController loginController, Customer customer, Runnable onLogout) {
        this.controller = controller;
        this.customer = customer;
        controller.startSession();

        Label welcome = new Label("BANK BAC   |   Welcome, " + customer.getDisplayName() + "   (" + customer.getCustomerID() + ")");
        welcome.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        Button logout = new Button("Logout");
        logout.setOnAction(e -> { loginController.logout(customer); onLogout.run(); });
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox top = new HBox(10, welcome, spacer, logout);
        top.setAlignment(Pos.CENTER_LEFT);
        top.setPadding(new Insets(15));
        top.setStyle("-fx-background-color: #ecf0f1;");
        root.setTop(top);

        table.getColumns().add(column("Account number", 150, Account::getAccountNumber));
        table.getColumns().add(column("Type", 100, Account::getType));
        table.getColumns().add(column("Balance (BWP)", 130, a -> Account.format(a.getBalance())));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(new Label("You have no accounts yet. Ask bank staff to open one for you."));
        root.setCenter(table);
        BorderPane.setMargin(table, new Insets(15));

        Button deposit = new Button("Deposit");
        Button withdraw = new Button("Withdraw");
        Button history = new Button("View history");
        deposit.setMaxWidth(Double.MAX_VALUE);
        withdraw.setMaxWidth(Double.MAX_VALUE);
        history.setMaxWidth(Double.MAX_VALUE);
        deposit.setOnAction(e -> deposit());
        withdraw.setOnAction(e -> withdraw());
        history.setOnAction(e -> history());
        message.setWrapText(true);
        VBox right = new VBox(10, new Label("Selected account"), deposit, withdraw, history, message);
        right.setPadding(new Insets(15));
        right.setPrefWidth(220);
        root.setRight(right);

        // log out after 5 minutes with no mouse or keyboard activity (NFR-1)
        PauseTransition timer = new PauseTransition(Duration.minutes(5));
        timer.setOnFinished(e -> {
            loginController.logout(customer);
            new Alert(Alert.AlertType.INFORMATION, "You were logged out after 5 minutes of inactivity.").show();
            onLogout.run();
        });
        root.addEventFilter(MouseEvent.ANY, e -> timer.playFromStart());
        root.addEventFilter(KeyEvent.ANY, e -> timer.playFromStart());
        timer.playFromStart();

        refresh();
    }

    private void deposit() {
        Account a = selected();
        if (a == null) return;
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Deposit");
        dialog.setHeaderText("Deposit into " + a.getAccountNumber());
        dialog.setContentText("Amount (BWP):");
        Button ok = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        ok.addEventFilter(ActionEvent.ACTION, ev -> {
            try {
                controller.deposit(customer, a.getAccountNumber(), dialog.getEditor().getText());
            } catch (Exception ex) {
                dialog.setHeaderText(ex.getMessage());
                ev.consume();
            }
        });
        if (dialog.showAndWait().isPresent()) { refresh(); ok("Deposit successful."); }
    }

    private void withdraw() {
        Account a = selected();
        if (a == null) return;
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Withdraw");
        TextField amount = new TextField();
        PasswordField pin = new PasswordField();
        Label error = new Label();
        error.setStyle("-fx-text-fill: #c0392b;");
        error.setWrapText(true);
        GridPane g = new GridPane();
        g.setHgap(10);
        g.setVgap(10);
        g.setPadding(new Insets(15));
        g.add(new Label("Withdraw from " + a.getAccountNumber()), 0, 0, 2, 1);
        g.add(new Label("Amount (BWP)"), 0, 1);
        g.add(amount, 1, 1);
        g.add(new Label("4 digit PIN"), 0, 2);
        g.add(pin, 1, 2);
        g.add(error, 0, 3, 2, 1);
        dialog.getDialogPane().setContent(g);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.addEventFilter(ActionEvent.ACTION, ev -> {
            try {
                controller.withdraw(customer, a.getAccountNumber(), amount.getText(), pin.getText());
            } catch (Exception ex) {
                error.setText(ex.getMessage());
                ev.consume();
            }
        });
        if (dialog.showAndWait().filter(b -> b == ButtonType.OK).isPresent()) { refresh(); ok("Withdrawal successful."); }
    }

    private void history() {
        Account a = selected();
        if (a == null) return;
        try {
            DateTimeFormatter f = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            ListView<String> list = new ListView<>();
            for (Transaction t : controller.getHistory(customer, a.getAccountNumber())) {
                list.getItems().add(t.getDate().format(f) + "   " + t.getType() + "   BWP" + Account.format(t.getAmount())
                        + "   balance BWP" + Account.format(t.getBalanceAfter()));
            }
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Transaction history");
            alert.setHeaderText(a.getType() + " account " + a.getAccountNumber());
            alert.getDialogPane().setContent(list);
            alert.showAndWait();
        } catch (Exception ex) {
            error(ex.getMessage());
        }
    }

    private Account selected() {
        Account a = table.getSelectionModel().getSelectedItem();
        if (a == null) error("Select an account in the table first.");
        return a;
    }

    private void refresh() {
        table.setItems(FXCollections.observableArrayList(controller.getAccounts(customer)));
        table.refresh();
    }

    private void ok(String text) { message.setStyle("-fx-text-fill: #27ae60;"); message.setText(text); }
    private void error(String text) { message.setStyle("-fx-text-fill: #c0392b;"); message.setText(text); }

    private static TableColumn<Account, String> column(String title, int width, Function<Account, String> value) {
        TableColumn<Account, String> col = new TableColumn<>(title);
        col.setPrefWidth(width);
        col.setCellValueFactory(cd -> new SimpleStringProperty(value.apply(cd.getValue())));
        return col;
    }

    public Parent getRoot() { return root; }
}

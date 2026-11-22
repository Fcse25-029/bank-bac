package bank.view;

import bank.controller.LoginController;
import bank.controller.StaffController;
import bank.model.*;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.function.Consumer;
import java.util.function.Function;

/** The bank staff screen: register customers, open accounts for them, update records, run monthly interest. */
public class StaffView {
    private final BorderPane root = new BorderPane();
    private final TableView<Customer> table = new TableView<>();
    private final Label message = new Label();
    private final StaffController controller;

    public StaffView(StaffController controller, LoginController loginController, Runnable onLogout) {
        this.controller = controller;

        Label title = new Label("BANK BAC   |   Bank staff");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        Button logout = new Button("Logout");
        logout.setOnAction(e -> onLogout.run());
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox top = new HBox(10, title, spacer, logout);
        top.setAlignment(Pos.CENTER_LEFT);
        top.setPadding(new Insets(15));
        top.setStyle("-fx-background-color: #ecf0f1;");
        root.setTop(top);

        table.getColumns().add(column("Customer ID", 90, Customer::getCustomerID));
        table.getColumns().add(column("Type", 90, c -> c instanceof IndividualCustomer ? "Individual" : "Company"));
        table.getColumns().add(column("Name", 170, Customer::getDisplayName));
        table.getColumns().add(column("Phone", 110, Customer::getPhone));
        table.getColumns().add(column("Accounts", 70, c -> String.valueOf(c.getAccounts().size())));
        table.getColumns().add(column("Login", 80, c -> c.isLocked() ? "LOCKED" : "OK"));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(new Label("No customers yet. Click Register customer."));
        root.setCenter(table);
        BorderPane.setMargin(table, new Insets(15));

        Button register = wide("Register customer");
        Button open = wide("Open account");
        Button accounts = wide("View accounts");
        Button interest = wide("Run monthly interest");
        register.setOnAction(e -> {
            Customer c = RegisterDialog.show(loginController);
            refresh();
            if (c != null) ok("Registered " + c.getDisplayName() + ". Customer ID: " + c.getCustomerID());
        });
        open.setOnAction(e -> withSelected(this::openAccount));
        accounts.setOnAction(e -> withSelected(this::showAccounts));
        interest.setOnAction(e -> {
            try {
                int n = controller.payMonthlyInterest();
                refresh();
                ok(n == 0 ? "No interest due: it was already paid this month." : "Interest paid on " + n + " account(s).");
            } catch (Exception ex) {
                error(ex.getMessage());
            }
        });

        message.setWrapText(true);
        VBox right = new VBox(10, register, new Separator(), new Label("Selected customer"), open, accounts,
                new Separator(), interest, message);
        right.setPadding(new Insets(15));
        right.setPrefWidth(220);
        root.setRight(right);
        refresh();
    }

    private void openAccount(Customer customer) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Open account");
        ComboBox<String> type = new ComboBox<>(FXCollections.observableArrayList("Savings", "Investment", "Cheque"));
        type.getSelectionModel().selectFirst();
        TextField deposit = new TextField();
        deposit.setPromptText("Investment: at least 500.00");
        Label error = new Label();
        error.setStyle("-fx-text-fill: #c0392b;");
        error.setWrapText(true);

        GridPane g = new GridPane();
        g.setHgap(10);
        g.setVgap(8);
        g.setPadding(new Insets(15));
        g.add(new Label("For " + customer.getDisplayName() + " (" + customer.getCustomerID() + ")"), 0, 0, 2, 1);
        g.add(new Label("Account type"), 0, 1);
        g.add(type, 1, 1);
        g.add(new Label("Initial deposit"), 0, 2);
        g.add(deposit, 1, 2);
        g.add(error, 0, 3, 2, 1);
        dialog.getDialogPane().setContent(g);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Account[] opened = new Account[1];
        Button ok = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        ok.addEventFilter(ActionEvent.ACTION, ev -> {
            try {
                opened[0] = controller.openAccount(customer.getCustomerID(), type.getValue(), deposit.getText());
            } catch (Exception ex) {
                error.setText(ex.getMessage());
                ev.consume();
            }
        });
        dialog.showAndWait();
        if (opened[0] != null) {
            refresh();
            ok("Account " + opened[0].getAccountNumber() + " opened for " + customer.getDisplayName() + ".");
        }
    }

    private void showAccounts(Customer customer) {
        ListView<String> list = new ListView<>();
        for (Account a : customer.getAccounts()) list.getItems().add(a.toString());
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Accounts");
        alert.setHeaderText("Accounts of " + customer.getDisplayName());
        alert.getDialogPane().setContent(list);
        alert.showAndWait();
    }

    private void withSelected(Consumer<Customer> action) {
        Customer c = table.getSelectionModel().getSelectedItem();
        if (c == null) error("Select a customer first.");
        else action.accept(c);
    }

    private void refresh() {
        table.setItems(FXCollections.observableArrayList(controller.getCustomers()));
        table.refresh();
    }

    private static Button wide(String text) {
        Button b = new Button(text);
        b.setMaxWidth(Double.MAX_VALUE);
        return b;
    }

    private void ok(String text) { message.setStyle("-fx-text-fill: #27ae60;"); message.setText(text); }
    private void error(String text) { message.setStyle("-fx-text-fill: #c0392b;"); message.setText(text); }

    private static TableColumn<Customer, String> column(String title, int width, Function<Customer, String> value) {
        TableColumn<Customer, String> col = new TableColumn<>(title);
        col.setPrefWidth(width);
        col.setCellValueFactory(cd -> new SimpleStringProperty(value.apply(cd.getValue())));
        return col;
    }

    public Parent getRoot() { return root; }
}

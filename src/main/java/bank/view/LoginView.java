package bank.view;

import bank.controller.LoginController;
import bank.model.Customer;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.function.Consumer;

/** Login screen for customers and bank staff. It only shows things and collects input. */
public class LoginView {
    private final VBox root = new VBox(15);

    public LoginView(LoginController controller, Consumer<Customer> onCustomerLogin, Runnable onStaffLogin) {
        Label title = new Label("BANK BAC");
        title.setStyle("-fx-font-size: 26px; -fx-font-weight: bold;");

        ComboBox<String> role = new ComboBox<>(FXCollections.observableArrayList("Customer", "Bank staff"));
        role.getSelectionModel().selectFirst();
        TextField name = new TextField();
        name.setPromptText("Full name or company name");
        PasswordField pin = new PasswordField();
        pin.setPromptText("4 digit PIN");
        Label message = new Label();
        message.setWrapText(true);

        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(10);
        form.setAlignment(Pos.CENTER);
        form.add(new Label("Login as"), 0, 0);
        form.add(role, 1, 0);
        form.add(new Label("Full name / company name"), 0, 1);
        form.add(name, 1, 1);
        form.add(new Label("PIN"), 0, 2);
        form.add(pin, 1, 2);

        Button login = new Button("Login");
        login.setDefaultButton(true);
        Button register = new Button("Register as new customer");

        login.setOnAction(e -> {
            try {
                if ("Customer".equals(role.getValue())) {
                    onCustomerLogin.accept(controller.loginCustomer(name.getText(), pin.getText()));
                } else {
                    controller.loginStaff(name.getText(), pin.getText());
                    onStaffLogin.run();
                }
            } catch (Exception ex) {
                message.setStyle("-fx-text-fill: #c0392b;");
                message.setText(ex.getMessage());
            }
        });
        register.setOnAction(e -> {
            Customer c = RegisterDialog.show(controller);
            if (c != null) {
                name.setText(c.getDisplayName());
                pin.clear();
                message.setStyle("-fx-text-fill: #27ae60;");
                message.setText("Registered. Your customer ID is " + c.getCustomerID()
                        + ". Bank staff will open an account for you. Then log in with your name and PIN.");
            }
        });

        Label hint = new Label("BANK STAFF LOGIN : Staff/ 1234");
        hint.setStyle("-fx-text-fill: #7f8c8d;");
        HBox buttons = new HBox(10, login, register);
        buttons.setAlignment(Pos.CENTER);
        root.getChildren().addAll(title, form, buttons, message, hint);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(40));
    }

    public Parent getRoot() { return root; }
}

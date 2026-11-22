package bank.view;

import bank.controller.LoginController;
import bank.model.Customer;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

/** Registration form for an individual or a company. Used by customers and by bank staff. */
public final class RegisterDialog {
    private RegisterDialog() {}

    /** Returns the new customer, or null if cancelled. */
    public static Customer show(LoginController controller) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Register customer");

        ComboBox<String> type = new ComboBox<>(FXCollections.observableArrayList("Individual", "Company"));
        type.getSelectionModel().selectFirst();

        TextField fullName = new TextField(), dob = new TextField();
        dob.setPromptText("yyyy-mm-dd");
        GridPane individual = grid();
        add(individual, 0, "Full name", fullName);
        add(individual, 1, "Date of birth", dob);

        TextField companyName = new TextField(), regNumber = new TextField();
        GridPane company = grid();
        add(company, 0, "Company name", companyName);
        add(company, 1, "Registration no.", regNumber);
        company.setVisible(false);
        company.setManaged(false);

        TextField phone = new TextField();
        PasswordField pin = new PasswordField();
        pin.setPromptText("4 digits");
        GridPane common = grid();
        add(common, 0, "Phone", phone);
        add(common, 1, "PIN (to log in and withdraw)", pin);

        type.setOnAction(e -> {
            boolean isIndividual = "Individual".equals(type.getValue());
            individual.setVisible(isIndividual);
            individual.setManaged(isIndividual);
            company.setVisible(!isIndividual);
            company.setManaged(!isIndividual);
            dialog.getDialogPane().getScene().getWindow().sizeToScene();
        });

        Label error = new Label();
        error.setStyle("-fx-text-fill: #c0392b;");
        error.setWrapText(true);
        VBox box = new VBox(10, type, individual, company, common, error);
        box.setPadding(new Insets(15));
        dialog.getDialogPane().setContent(box);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Customer[] result = new Customer[1];
        Button ok = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        ok.addEventFilter(ActionEvent.ACTION, ev -> {
            try {
                if ("Individual".equals(type.getValue())) {
                    result[0] = controller.registerIndividual(fullName.getText(), dob.getText(), phone.getText(),
                            pin.getText());
                } else {
                    result[0] = controller.registerCompany(companyName.getText(), regNumber.getText(), phone.getText(),
                            pin.getText());
                }
            } catch (Exception ex) {
                error.setText(ex.getMessage());
                ev.consume();          // keep the form open so the mistake can be fixed
            }
        });
        dialog.showAndWait();
        return result[0];
    }

    private static GridPane grid() {
        GridPane g = new GridPane();
        g.setHgap(10);
        g.setVgap(8);
        return g;
    }

    private static void add(GridPane g, int row, String label, Control field) {
        g.add(new Label(label), 0, row);
        g.add(field, 1, row);
    }
}

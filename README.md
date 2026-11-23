# BANK BAC

A JavaFX banking system built with Java 17 and Maven. The project is split into model, DAO, controller and view layers.

## Run in GitHub Codespaces
1. Open the repo and click Code > Codespaces > Create codespace on main.
2. In the terminal run the tests: `mvn test`
3. Start the app: `mvn javafx:run`
4. Open the Ports tab, open port 6080 in the browser, click Connect and enter the password `vscode` to see the window.

In Codespaces there is no MySQL, so the app prints a warning and automatically uses a local H2 database.

## Run locally with XAMPP
1. Start MySQL in the XAMPP Control Panel.
2. Run `Launcher.java` or `mvn javafx:run`.
3. The database `bank_bac` is created automatically (user `root`, no password).

## Logins
- Staff: name `Staff`, PIN `1234`
- Customers: register from the login screen, then log in with the name and your 4-digit PIN.

## Structure
- `bank.model`: Customer, Account, Bank and the business rules
- `bank.dao`: Database access (SQL)
- `bank.controller`: Login, Account and Staff controllers
- `bank.view`: JavaFX screens (LoginView, AccountView, StaffView, RegisterDialog)
- `src/test`: ModelTest and IntegrationTest

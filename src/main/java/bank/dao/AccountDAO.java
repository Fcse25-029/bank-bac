package bank.dao;

import bank.model.*;

import java.sql.*;
import java.time.LocalDate;

/** Create, read, update and delete accounts, and save their transactions. */
public class AccountDAO {
    private final Database db;

    public AccountDAO(Database db) { this.db = db; }

    // ---- Create ----
    public void create(Account a) throws SQLException {
        String sql = "INSERT INTO accounts (account_number, customer_id, account_type, branch, date_opened, status, "
                + "balance, last_interest_date, employer_name, employer_address, employer_phone, salary_date) "
                + "VALUES (?,?,?,?,?,?,?,?,?,?,?,?)";
        try (Connection con = db.connect(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, a.getAccountNumber());
            ps.setString(2, a.getOwner().getCustomerID());
            ps.setString(3, a.getType());
            ps.setString(4, a.getBranch());
            ps.setDate(5, Date.valueOf(a.getDateOpened()));
            ps.setString(6, a.getStatus());
            ps.setDouble(7, a.getBalance());
            bindOptional(ps, a, 8);
            ps.executeUpdate();
        }
    }

    // ---- Read: loads the customer's accounts (with their history) and adds them to the customer ----
    public void loadAccounts(Customer owner) throws SQLException {
        try (Connection con = db.connect();
             PreparedStatement ps = con.prepareStatement("SELECT * FROM accounts WHERE customer_id = ? ORDER BY account_number")) {
            ps.setString(1, owner.getCustomerID());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Account a = toAccount(rs, owner);
                    loadTransactions(a);
                    owner.addAccount(a);
                }
            }
        }
    }

    // ---- Update ----
    public void update(Account a) throws SQLException {
        String sql = "UPDATE accounts SET status=?, balance=?, last_interest_date=?, employer_name=?, "
                + "employer_address=?, employer_phone=?, salary_date=? WHERE account_number=?";
        try (Connection con = db.connect(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, a.getStatus());
            ps.setDouble(2, a.getBalance());
            bindOptional(ps, a, 3);
            ps.setString(8, a.getAccountNumber());
            ps.executeUpdate();
        }
    }

    // ---- Delete (the account and its transactions) ----
    public void delete(String accountNumber) throws SQLException {
        try (Connection con = db.connect()) {
            try (PreparedStatement ps = con.prepareStatement("DELETE FROM transactions WHERE account_number = ?")) {
                ps.setString(1, accountNumber);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = con.prepareStatement("DELETE FROM accounts WHERE account_number = ?")) {
                ps.setString(1, accountNumber);
                ps.executeUpdate();
            }
        }
    }

    /** Saves a transaction and sets its transactionID. */
    public void saveTransaction(Transaction t) throws SQLException {
        String sql = "INSERT INTO transactions (account_number, tx_date, tx_type, amount, balance_after, description) "
                + "VALUES (?,?,?,?,?,?)";
        try (Connection con = db.connect(); PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, t.getAccountNumber());
            ps.setTimestamp(2, Timestamp.valueOf(t.getDate()));
            ps.setString(3, t.getType().name());
            ps.setDouble(4, t.getAmount());
            ps.setDouble(5, t.getBalanceAfter());
            ps.setString(6, t.getDescription());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) t.setTransactionID(keys.getInt(1));
            }
        }
    }

    // ---- helpers ----
    /** Sets last_interest_date, employer_name, employer_address, employer_phone, salary_date (5 values from index). */
    private void bindOptional(PreparedStatement ps, Account a, int index) throws SQLException {
        LocalDate last = null;
        if (a instanceof SavingsAccount) last = ((SavingsAccount) a).getLastInterestDate();
        if (a instanceof InvestmentAccount) last = ((InvestmentAccount) a).getLastInterestDate();
        ps.setDate(index, last == null ? null : Date.valueOf(last));
        if (a instanceof ChequeAccount) {
            ChequeAccount c = (ChequeAccount) a;
            ps.setString(index + 1, c.getEmployerName());
            ps.setString(index + 2, c.getEmployerAddress());
            ps.setString(index + 3, c.getEmployerPhone());
            ps.setDate(index + 4, c.getSalaryDate() == null ? null : Date.valueOf(c.getSalaryDate()));
        } else {
            ps.setNull(index + 1, Types.VARCHAR);
            ps.setNull(index + 2, Types.VARCHAR);
            ps.setNull(index + 3, Types.VARCHAR);
            ps.setNull(index + 4, Types.DATE);
        }
    }

    private void loadTransactions(Account a) throws SQLException {
        try (Connection con = db.connect();
             PreparedStatement ps = con.prepareStatement("SELECT * FROM transactions WHERE account_number = ? ORDER BY transaction_id")) {
            ps.setString(1, a.getAccountNumber());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    a.addTransaction(new Transaction(rs.getInt("transaction_id"), rs.getString("account_number"),
                            rs.getTimestamp("tx_date").toLocalDateTime(), TransactionType.valueOf(rs.getString("tx_type")),
                            rs.getDouble("amount"), rs.getDouble("balance_after"), rs.getString("description")));
                }
            }
        }
    }

    private Account toAccount(ResultSet rs, Customer owner) throws SQLException {
        String number = rs.getString("account_number");
        String branch = rs.getString("branch");
        String type = rs.getString("account_type");
        LocalDate last = rs.getDate("last_interest_date") == null ? null : rs.getDate("last_interest_date").toLocalDate();
        Account a;
        if (type.equals("Savings")) {
            SavingsAccount s = new SavingsAccount(number, branch, owner);
            s.setLastInterestDate(last);
            a = s;
        } else if (type.equals("Investment")) {
            InvestmentAccount i = new InvestmentAccount(number, branch, owner);
            i.setLastInterestDate(last);
            a = i;
        } else {
            ChequeAccount c = new ChequeAccount(number, branch, owner, rs.getString("employer_name"), rs.getString("employer_address"));
            c.setEmployerPhone(rs.getString("employer_phone"));
            if (rs.getDate("salary_date") != null) c.setSalaryDate(rs.getDate("salary_date").toLocalDate());
            a = c;
        }
        a.setBalance(rs.getDouble("balance"));
        a.setStatus(rs.getString("status"));
        a.setDateOpened(rs.getDate("date_opened").toLocalDate());
        return a;
    }
}

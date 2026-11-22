package bank.dao;

import bank.model.CompanyCustomer;
import bank.model.Customer;
import bank.model.IndividualCustomer;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/** Create, read, update and delete customers (individuals and companies share one table). */
public class CustomerDAO {
    private final Database db;

    public CustomerDAO(Database db) { this.db = db; }

    // ---- Create ----
    public void create(Customer c) throws SQLException {
        String sql = "INSERT INTO customers (customer_id, customer_type, address, phone, email, "
                + "pin_hash, registration_date, failed_login_attempts, full_name, date_of_birth, company_name, "
                + "registration_number) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)";
        try (Connection con = db.connect(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, c.getCustomerID());
            ps.setString(2, c instanceof IndividualCustomer ? "INDIVIDUAL" : "COMPANY");
            ps.setString(3, c.getAddress());
            ps.setString(4, c.getPhone());
            ps.setString(5, c.getEmail());
            ps.setString(6, c.getPinHash());
            ps.setDate(7, Date.valueOf(c.getRegistrationDate()));
            ps.setInt(8, c.getFailedLoginAttempts());
            if (c instanceof IndividualCustomer) {
                IndividualCustomer i = (IndividualCustomer) c;
                ps.setString(9, i.getFullName());
                ps.setDate(10, Date.valueOf(i.getDateOfBirth()));
                ps.setNull(11, Types.VARCHAR);
                ps.setNull(12, Types.VARCHAR);
            } else {
                CompanyCustomer co = (CompanyCustomer) c;
                ps.setNull(9, Types.VARCHAR);
                ps.setNull(10, Types.DATE);
                ps.setString(11, co.getCompanyName());
                ps.setString(12, co.getRegistrationNumber());
            }
            ps.executeUpdate();
        }
    }

    // ---- Read ----
    public List<Customer> findAll() throws SQLException {
        List<Customer> list = new ArrayList<>();
        try (Connection con = db.connect(); Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM customers ORDER BY customer_id")) {
            while (rs.next()) list.add(toCustomer(rs));
        }
        return list;
    }

    // ---- Update ----
    public void update(Customer c) throws SQLException {
        String sql = "UPDATE customers SET address=?, phone=?, email=?, failed_login_attempts=? WHERE customer_id=?";
        try (Connection con = db.connect(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, c.getAddress());
            ps.setString(2, c.getPhone());
            ps.setString(3, c.getEmail());
            ps.setInt(4, c.getFailedLoginAttempts());
            ps.setString(5, c.getCustomerID());
            ps.executeUpdate();
        }
    }

    // ---- Delete ----
    public void delete(String customerId) throws SQLException {
        try (Connection con = db.connect();
             PreparedStatement ps = con.prepareStatement("DELETE FROM customers WHERE customer_id = ?")) {
            ps.setString(1, customerId);
            ps.executeUpdate();
        }
    }

    private Customer toCustomer(ResultSet rs) throws SQLException {
        Customer c;
        if ("INDIVIDUAL".equals(rs.getString("customer_type"))) {
            c = new IndividualCustomer(rs.getString("customer_id"), rs.getString("full_name"),
                    rs.getDate("date_of_birth").toLocalDate(), rs.getString("phone"));
        } else {
            c = new CompanyCustomer(rs.getString("customer_id"), rs.getString("company_name"),
                    rs.getString("registration_number"), rs.getString("phone"));
        }
        c.restore(rs.getString("pin_hash"), rs.getDate("registration_date").toLocalDate(), rs.getInt("failed_login_attempts"));
        return c;
    }
}

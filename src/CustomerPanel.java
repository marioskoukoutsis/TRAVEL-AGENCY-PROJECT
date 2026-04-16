import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

class CustomerPanel extends JPanel {
    private JTable table;
    private DefaultTableModel model;

    public CustomerPanel() {
        setLayout(new BorderLayout());

        model = new DefaultTableModel(new String[]{"ID", "First Name", "Last Name", "Email", "Phone Number", "Address", "Birthdate"}, 0);
        table = new JTable(model);
        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel btnPanel = new JPanel();
        JButton btnLoad = new JButton("Refresh Data");
        JButton btnAdd = new JButton("Add Customer");
        JButton btnEdit = new JButton("Edit Customer");
        JButton btnDelete = new JButton("Delete Customer");

        btnPanel.add(btnLoad);
        btnPanel.add(btnAdd);
        btnPanel.add(btnEdit);
        btnPanel.add(btnDelete);
        add(btnPanel, BorderLayout.SOUTH);

        btnLoad.addActionListener(e -> loadCustomers());
        btnAdd.addActionListener(e -> addCustomer());
        btnEdit.addActionListener(e -> editCustomer());
        btnDelete.addActionListener(e -> deleteCustomer());

        loadCustomers();
    }

    private void loadCustomers() {
        model.setRowCount(0);
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM customer")) {
            while (rs.next()) {
                model.addRow(new Object[]{
                        rs.getInt("cust_id"),
                        rs.getString("cust_name"),
                        rs.getString("cust_lname"),
                        rs.getString("cust_email"),
                        rs.getString("cust_phone"),
                        rs.getString("cust_address"),
                        rs.getString("cust_birth_date")
                });
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void addCustomer() {
        JTextField name = new JTextField();
        JTextField lname = new JTextField();
        JTextField email = new JTextField();
        JTextField phone = new JTextField();
        JTextField address = new JTextField();
        JTextField birthdate = new JTextField();

        Object[] message = {"Name:", name, "Last Name:", lname, "Email:", email, "Phone:", phone, "Address:", address, "Birthdate:", birthdate};
        int option = JOptionPane.showConfirmDialog(this, message, "Add Customer", JOptionPane.OK_CANCEL_OPTION);

        if (option == JOptionPane.OK_OPTION) {
            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement("INSERT INTO customer (cust_name, cust_lname, cust_email, cust_phone, cust_address, cust_birth_date) VALUES (?, ?, ?, ?, ?, ?)")) {
                pstmt.setString(1, name.getText());
                pstmt.setString(2, lname.getText());
                pstmt.setString(3, email.getText());
                pstmt.setString(4, phone.getText());
                pstmt.setString(5, address.getText());
                pstmt.setString(6, birthdate.getText());
                pstmt.executeUpdate();
                loadCustomers();
            } catch (SQLException e) { JOptionPane.showMessageDialog(this, "Error: " + e.getMessage()); }
        }
    }

    private void editCustomer() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a customer to edit first.");
            return;
        }

        int customerId = (int) model.getValueAt(selectedRow, 0);

        String[] columnNames = {"First Name", "Last Name", "Email", "Phone Number", "Address", "Birthdate"};

        String[] dbColumns = {"cust_name", "cust_lname", "cust_email", "cust_phone", "cust_address", "cust_birth_date"};

        JComboBox<String> colCombo = new JComboBox<>(columnNames);

        int input = JOptionPane.showConfirmDialog(this, colCombo, "Select column to edit", JOptionPane.OK_CANCEL_OPTION);

        if (input == JOptionPane.OK_OPTION) {
            int selectedIndex = colCombo.getSelectedIndex();
            String selectedDbColumn = dbColumns[selectedIndex];

            String newValue = JOptionPane.showInputDialog(this, "Enter new value for " + columnNames[selectedIndex] + ":");

            if (newValue != null && !newValue.trim().isEmpty()) {
                try (Connection conn = DBConnection.getConnection()) {
                    String sql = "UPDATE customer SET " + selectedDbColumn + " = ? WHERE cust_id = ?";

                    PreparedStatement pstmt = conn.prepareStatement(sql);
                    pstmt.setString(1, newValue);
                    pstmt.setInt(2, customerId);

                    int rows = pstmt.executeUpdate();
                    if (rows > 0) {
                        JOptionPane.showMessageDialog(this, "Updated successfully!");
                        loadCustomers();
                    }
                } catch (SQLException e) {
                    JOptionPane.showMessageDialog(this, "Error updating: " + e.getMessage());
                }
            }
        }
    }

    private void deleteCustomer() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a customer to delete.");
            return;
        }

        int id = (int) model.getValueAt(selectedRow, 0);

        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete this customer?", "Confirm Delete", JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement("DELETE FROM customer WHERE cust_id = ?")) {

                pstmt.setInt(1, id);
                int rows = pstmt.executeUpdate();

                if (rows > 0) {
                    JOptionPane.showMessageDialog(this, "Customer deleted successfully.");
                    loadCustomers(); // Refresh
                }
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Error deleting customer (Check logic for existing reservations): " + e.getMessage());
            }
        }
    }
}
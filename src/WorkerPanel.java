import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class WorkerPanel extends JPanel {
    private JTable table;
    private DefaultTableModel model;

    public WorkerPanel() {
        setLayout(new BorderLayout());

        // Στήλες Πίνακα
        model = new DefaultTableModel(new String[]{"AT", "Name", "Last Name", "Email", "Salary", "Branch Code"}, 0){
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = new JTable(model);
        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel btnPanel = new JPanel();
        JButton btnLoad = new JButton("Refresh");
        JButton btnAdd = new JButton("Add Worker");
        JButton btnEdit = new JButton("Edit Worker");
        JButton btnDelete = new JButton("Delete Worker");

        btnPanel.add(btnLoad);
        btnPanel.add(btnAdd);
        btnPanel.add(btnEdit);
        btnPanel.add(btnDelete);
        add(btnPanel, BorderLayout.SOUTH);

        btnLoad.addActionListener(e -> loadWorkers());
        btnAdd.addActionListener(e -> addWorker());
        btnEdit.addActionListener(e -> editWorker());
        btnDelete.addActionListener(e -> deleteWorker());

        loadWorkers();
    }

    private void loadWorkers() {
        model.setRowCount(0);
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM worker")) {
            while (rs.next()) {
                model.addRow(new Object[]{
                        rs.getString("wrk_AT"),
                        rs.getString("wrk_name"),
                        rs.getString("wrk_lname"),
                        rs.getString("wrk_email"),
                        rs.getDouble("wrk_salary"),
                        rs.getInt("wrk_br_code")
                });
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void addWorker() {
        JTextField at = new JTextField();
        JTextField name = new JTextField();
        JTextField lname = new JTextField();
        JTextField email = new JTextField();
        JTextField salary = new JTextField();
        JComboBox<String> branchCombo = getBranchDropdown();

        Object[] message = {
                "AT (Identity):", at,
                "First Name:", name,
                "Last Name:", lname,
                "Email:", email,
                "Salary:", salary,
                "Branch:", branchCombo
        };

        if (JOptionPane.showConfirmDialog(this, message, "Add Worker", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement("INSERT INTO worker (wrk_AT, wrk_name, wrk_lname, wrk_email, wrk_salary, wrk_br_code) VALUES (?, ?, ?, ?, ?, ?)")) {

                pstmt.setString(1, at.getText());
                pstmt.setString(2, name.getText());
                pstmt.setString(3, lname.getText());
                pstmt.setString(4, email.getText());
                pstmt.setDouble(5, Double.parseDouble(salary.getText()));

                String selectedBranch = (String) branchCombo.getSelectedItem();
                pstmt.setInt(6, Integer.parseInt(selectedBranch.split(" - ")[0]));

                pstmt.executeUpdate();
                loadWorkers();
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
            }
        }
    }

    private void editWorker() {
        int row = table.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Please select a worker first.");
            return;
        }

        String workerAT = (String) model.getValueAt(row, 0);

        String[] columnNames = {"First Name", "Last Name", "Email", "Salary", "Branch"};
        String[] dbColumns = {"wrk_name", "wrk_lname", "wrk_email", "wrk_salary", "wrk_br_code"};

        JComboBox<String> colCombo = new JComboBox<>(columnNames);

        if (JOptionPane.showConfirmDialog(this, colCombo, "Select column to edit", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {

            int index = colCombo.getSelectedIndex();
            String selectedDbCol = dbColumns[index];
            String newValue = null;

            if (selectedDbCol.equals("wrk_br_code")) {
                JComboBox<String> brBox = getBranchDropdown();
                if (JOptionPane.showConfirmDialog(this, brBox, "Select New Branch", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
                    newValue = ((String) brBox.getSelectedItem()).split(" - ")[0];
                }
            } else {
                newValue = JOptionPane.showInputDialog(this, "Enter new value for " + columnNames[index] + ":");
            }

            if (newValue != null && !newValue.trim().isEmpty()) {
                try (Connection conn = DBConnection.getConnection();
                     PreparedStatement pstmt = conn.prepareStatement("UPDATE worker SET " + selectedDbCol + " = ? WHERE wrk_AT = ?")) {

                    if (selectedDbCol.equals("wrk_salary")) {
                        pstmt.setDouble(1, Double.parseDouble(newValue));
                    } else if (selectedDbCol.equals("wrk_br_code")) {
                        pstmt.setInt(1, Integer.parseInt(newValue));
                    } else {
                        pstmt.setString(1, newValue);
                    }

                    pstmt.setString(2, workerAT);

                    pstmt.executeUpdate();
                    JOptionPane.showMessageDialog(this, "Worker updated successfully!");
                    loadWorkers();

                } catch (Exception e) {
                    JOptionPane.showMessageDialog(this, "Error updating: " + e.getMessage());
                }
            }
        }
    }

    private void deleteWorker() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Select a worker to delete.");
            return;
        }

        String workerAT = (String) model.getValueAt(selectedRow, 0);

        if (JOptionPane.showConfirmDialog(this, "Delete worker " + workerAT + "?", "Confirm", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement("DELETE FROM worker WHERE wrk_AT = ?")) {
                pstmt.setString(1, workerAT);
                pstmt.executeUpdate();
                loadWorkers();
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
            }
        }
    }

    private JComboBox<String> getBranchDropdown() {
        JComboBox<String> combo = new JComboBox<>();
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT br_code, br_city FROM branch")) {
            while (rs.next()) {
                combo.addItem(rs.getInt("br_code") + " - " + rs.getString("br_city"));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return combo;
    }
}
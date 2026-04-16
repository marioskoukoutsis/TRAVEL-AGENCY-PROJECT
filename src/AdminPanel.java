import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class AdminPanel extends JPanel {
    private JTable table;
    private DefaultTableModel model;

    public AdminPanel() {
        setLayout(new BorderLayout());

        model = new DefaultTableModel(new String[]{"AT", "First Name", "Last Name", "Type", "Diploma"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = new JTable(model);
        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel btnPanel = new JPanel();
        JButton btnLoad = new JButton("Refresh");
        JButton btnAdd = new JButton("Add Admin");
        JButton btnEdit = new JButton("Edit Admin");
        JButton btnDelete = new JButton("Delete Admin");

        btnPanel.add(btnLoad);
        btnPanel.add(btnAdd);
        btnPanel.add(btnEdit);
        btnPanel.add(btnDelete);
        add(btnPanel, BorderLayout.SOUTH);

        btnLoad.addActionListener(e -> loadAdmins());
        btnAdd.addActionListener(e -> addAdmin());
        btnEdit.addActionListener(e -> editAdmin());
        btnDelete.addActionListener(e -> deleteAdmin());

        loadAdmins();
    }

    private void loadAdmins() {
        model.setRowCount(0);
        String sql = "SELECT a.adm_AT, w.wrk_name, w.wrk_lname, a.adm_type, a.adm_diploma " +
                "FROM admin a " +
                "JOIN worker w ON a.adm_AT = w.wrk_AT";

        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                model.addRow(new Object[]{
                        rs.getString("adm_AT"),
                        rs.getString("wrk_name"),
                        rs.getString("wrk_lname"),
                        rs.getString("adm_type"),
                        rs.getString("adm_diploma")
                });
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading data: " + e.getMessage());
        }
    }

    private void addAdmin() {
        JComboBox<String> workerCombo = getWorkerDropdown();
        JComboBox<String> typeCombo = new JComboBox<>(new String[]{"ADMINISTRATIVE", "ACCOUNTING", "LOGISTICS"});
        JTextField diplomaField = new JTextField();

        Object[] message = {
                "Select Worker:", workerCombo,
                "Admin Type:", typeCombo,
                "Diploma:", diplomaField
        };

        int option = JOptionPane.showConfirmDialog(this, message, "Add Admin Role", JOptionPane.OK_CANCEL_OPTION);

        if (option == JOptionPane.OK_OPTION) {
            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement("INSERT INTO admin (adm_AT, adm_type, adm_diploma) VALUES (?, ?, ?)")) {

                String selectedWorker = (String) workerCombo.getSelectedItem();
                if (selectedWorker == null) return;

                String at = selectedWorker.split(" - ")[0];

                pstmt.setString(1, at);
                pstmt.setString(2, (String) typeCombo.getSelectedItem());
                pstmt.setString(3, diplomaField.getText());

                pstmt.executeUpdate();
                loadAdmins();
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
            }
        }
    }

    private void editAdmin() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select an admin to edit first.");
            return;
        }

        String adminAT = (String) model.getValueAt(selectedRow, 0);

        String[] columnNames = {"Type", "Diploma"};
        String[] dbColumns = {"adm_type", "adm_diploma"};

        JComboBox<String> colCombo = new JComboBox<>(columnNames);

        int input = JOptionPane.showConfirmDialog(this, colCombo, "Select column to edit", JOptionPane.OK_CANCEL_OPTION);

        if (input == JOptionPane.OK_OPTION) {
            int selectedIndex = colCombo.getSelectedIndex();
            String selectedDbColumn = dbColumns[selectedIndex];
            String newValue = null;

            if (selectedDbColumn.equals("adm_type")) {
                JComboBox<String> typeBox = new JComboBox<>(new String[]{"ADMINISTRATIVE", "ACCOUNTING", "LOGISTICS"});
                if (JOptionPane.showConfirmDialog(this, typeBox, "Select New Type", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
                    newValue = (String) typeBox.getSelectedItem();
                }
            } else {
                newValue = JOptionPane.showInputDialog(this, "Enter new value for " + columnNames[selectedIndex] + ":");
            }

            if (newValue != null && !newValue.trim().isEmpty()) {
                try (Connection conn = DBConnection.getConnection()) {
                    String sql = "UPDATE admin SET " + selectedDbColumn + " = ? WHERE adm_AT = ?";

                    PreparedStatement pstmt = conn.prepareStatement(sql);
                    pstmt.setString(1, newValue);
                    pstmt.setString(2, adminAT);

                    int rows = pstmt.executeUpdate();
                    if (rows > 0) {
                        JOptionPane.showMessageDialog(this, "Updated successfully!");
                        loadAdmins();
                    }
                } catch (SQLException e) {
                    JOptionPane.showMessageDialog(this, "Error updating: " + e.getMessage());
                }
            }
        }
    }

    private void deleteAdmin() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select an admin to delete.");
            return;
        }

        String adminAT = (String) model.getValueAt(selectedRow, 0);

        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to remove Admin role from " + adminAT + "?", "Confirm Delete", JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement("DELETE FROM admin WHERE adm_AT = ?")) {

                pstmt.setString(1, adminAT);
                int rows = pstmt.executeUpdate();

                if (rows > 0) {
                    JOptionPane.showMessageDialog(this, "Admin deleted successfully.");
                    loadAdmins();
                }
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Error deleting admin: " + e.getMessage());
            }
        }
    }

    private JComboBox<String> getWorkerDropdown() {
        JComboBox<String> combo = new JComboBox<>();
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT wrk_AT, wrk_name, wrk_lname FROM worker")) {
            while (rs.next()) {
                combo.addItem(rs.getString("wrk_AT") + " - " + rs.getString("wrk_name") + " " + rs.getString("wrk_lname"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return combo;
    }
}

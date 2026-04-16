import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class DriverPanel extends JPanel {
    private JTable table;
    private DefaultTableModel model;

    public DriverPanel() {
        setLayout(new BorderLayout());
        model = new DefaultTableModel(new String[]{"Driver AT", "License", "Route", "Experience"}, 0){
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = new JTable(model);
        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel btnPanel = new JPanel();
        JButton btnLoad = new JButton("Refresh");
        JButton btnAdd = new JButton("Assign Role");
        JButton btnEdit = new JButton("Edit Info");
        JButton btnDelete = new JButton("Remove Role");

        btnPanel.add(btnLoad); btnPanel.add(btnAdd); btnPanel.add(btnEdit); btnPanel.add(btnDelete);
        add(btnPanel, BorderLayout.SOUTH);

        btnLoad.addActionListener(e -> loadDrivers());
        btnAdd.addActionListener(e -> addDriver());
        btnEdit.addActionListener(e -> editDriver());
        btnDelete.addActionListener(e -> deleteDriver());
        loadDrivers();
    }

    private void loadDrivers() {
        model.setRowCount(0);
        try (Connection conn = DBConnection.getConnection(); ResultSet rs = conn.createStatement().executeQuery("SELECT * FROM driver")) {
            while (rs.next()) model.addRow(new Object[]{rs.getString("drv_AT"), rs.getString("drv_license"), rs.getString("drv_route"), rs.getInt("drv_experience")});
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void addDriver() {
        JComboBox<String> workerCombo = getWorkerDropdown(); // Επιλέγουμε Worker
        JComboBox<String> licenseCombo = new JComboBox<>(new String[]{"A", "B", "C", "D"});
        JComboBox<String> routeCombo = new JComboBox<>(new String[]{"LOCAL", "ABROAD"});
        JTextField expField = new JTextField();

        Object[] msg = {"Worker:", workerCombo, "License:", licenseCombo, "Route:", routeCombo, "Experience:", expField};

        if (JOptionPane.showConfirmDialog(this, msg, "Assign Driver Role", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement("INSERT INTO driver VALUES (?, ?, ?, ?)")) {
                ps.setString(1, ((String)workerCombo.getSelectedItem()).split(" - ")[0]);
                ps.setString(2, (String)licenseCombo.getSelectedItem());
                ps.setString(3, (String)routeCombo.getSelectedItem());
                ps.setInt(4, Integer.parseInt(expField.getText()));
                ps.executeUpdate();
                loadDrivers();
            } catch (Exception e) { JOptionPane.showMessageDialog(this, "Error: " + e.getMessage()); }
        }
    }

    private void editDriver() {
        int row = table.getSelectedRow();
        if (row == -1) { JOptionPane.showMessageDialog(this, "Select a driver."); return; }

        String at = (String) model.getValueAt(row, 0);
        String[] cols = {"License", "Route", "Experience"};
        String[] dbCols = {"drv_license", "drv_route", "drv_experience"};

        JComboBox<String> cb = new JComboBox<>(cols);
        if (JOptionPane.showConfirmDialog(this, cb, "Select Column", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            String val = JOptionPane.showInputDialog(this, "New Value:");
            if (val != null) {
                try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement("UPDATE driver SET " + dbCols[cb.getSelectedIndex()] + "=? WHERE drv_AT=?")) {
                    if (cb.getSelectedIndex() == 2) ps.setInt(1, Integer.parseInt(val));
                    else ps.setString(1, val);
                    ps.setString(2, at);
                    ps.executeUpdate();
                    loadDrivers();
                } catch (Exception e) { JOptionPane.showMessageDialog(this, "Error: " + e.getMessage()); }
            }
        }
    }

    private void deleteDriver() {
        int row = table.getSelectedRow();
        if (row != -1 && JOptionPane.showConfirmDialog(this, "Remove Role?", "Confirm", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement("DELETE FROM driver WHERE drv_AT=?")) {
                ps.setString(1, (String) model.getValueAt(row, 0));
                ps.executeUpdate();
                loadDrivers();
            } catch (SQLException e) { JOptionPane.showMessageDialog(this, "Error: " + e.getMessage()); }
        }
    }

    private JComboBox<String> getWorkerDropdown() {
        JComboBox<String> c = new JComboBox<>();
        String sql = "SELECT wrk_AT, wrk_lname FROM worker WHERE wrk_AT NOT IN (SELECT drv_AT FROM driver)";
        try (Connection conn = DBConnection.getConnection(); ResultSet rs = conn.createStatement().executeQuery(sql)) {
            while (rs.next()) c.addItem(rs.getString(1) + " - " + rs.getString(2));
        } catch (Exception e) {}
        return c;
    }
}
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class BranchPanel extends JPanel {
    private JTable table;
    private DefaultTableModel model;

    public BranchPanel() {
        setLayout(new BorderLayout());
        model = new DefaultTableModel(new String[]{"Code", "Street", "Num", "City", "Manager AT"}, 0){
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = new JTable(model);
        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel btnPanel = new JPanel();
        JButton btnLoad = new JButton("Refresh");
        JButton btnAdd = new JButton("Add Branch");
        JButton btnEdit = new JButton("Edit Branch");
        JButton btnDelete = new JButton("Delete Branch");

        btnPanel.add(btnLoad); btnPanel.add(btnAdd); btnPanel.add(btnEdit); btnPanel.add(btnDelete);
        add(btnPanel, BorderLayout.SOUTH);

        btnLoad.addActionListener(e -> loadBranches());
        btnAdd.addActionListener(e -> addBranch());
        btnEdit.addActionListener(e -> editBranch());
        btnDelete.addActionListener(e -> deleteBranch());

        loadBranches();
    }

    private void loadBranches() {
        model.setRowCount(0);
        try (Connection conn = DBConnection.getConnection(); Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM branch")) {
            while (rs.next()) model.addRow(new Object[]{rs.getInt("br_code"), rs.getString("br_street"), rs.getInt("br_num"), rs.getString("br_city"), rs.getString("br_manager_AT")});
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void addBranch() {
        JTextField street = new JTextField();
        JTextField num = new JTextField();
        JTextField city = new JTextField();
        JComboBox<String> managerCombo = getAdminDropdown();

        Object[] message = {"Street:", street, "Number:", num, "City:", city, "Manager:", managerCombo};

        if (JOptionPane.showConfirmDialog(this, message, "Add Branch", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement("INSERT INTO branch (br_street, br_num, br_city, br_manager_AT) VALUES (?, ?, ?, ?)")) {
                ps.setString(1, street.getText());
                ps.setInt(2, Integer.parseInt(num.getText()));
                ps.setString(3, city.getText());
                ps.setString(4, ((String)managerCombo.getSelectedItem()).split(" - ")[0]);
                ps.executeUpdate();
                loadBranches();
            } catch (Exception e) { JOptionPane.showMessageDialog(this, "Error: " + e.getMessage()); }
        }
    }

    private void editBranch() {
        int row = table.getSelectedRow();
        if (row == -1) { JOptionPane.showMessageDialog(this, "Select a branch."); return; }

        int id = (int) model.getValueAt(row, 0);
        String[] cols = {"Street", "Number", "City"};
        String[] dbCols = {"br_street", "br_num", "br_city"};

        JComboBox<String> cb = new JComboBox<>(cols);
        if (JOptionPane.showConfirmDialog(this, cb, "Select Column", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            String val = JOptionPane.showInputDialog(this, "New Value:");
            if (val != null) {
                try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement("UPDATE branch SET " + dbCols[cb.getSelectedIndex()] + "=? WHERE br_code=?")) {
                    if (cb.getSelectedIndex() == 1) ps.setInt(1, Integer.parseInt(val));
                    else ps.setString(1, val);
                    ps.setInt(2, id);
                    ps.executeUpdate();
                    loadBranches();
                } catch (Exception e) { JOptionPane.showMessageDialog(this, "Error: " + e.getMessage()); }
            }
        }
    }

    private void deleteBranch() {
        int row = table.getSelectedRow();
        if (row != -1 && JOptionPane.showConfirmDialog(this, "Delete Branch?", "Confirm", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement("DELETE FROM branch WHERE br_code=?")) {
                ps.setInt(1, (int) model.getValueAt(row, 0));
                ps.executeUpdate();
                loadBranches();
            } catch (SQLException e) { JOptionPane.showMessageDialog(this, "Error: " + e.getMessage()); }
        }
    }

    private JComboBox<String> getAdminDropdown() {
        JComboBox<String> c = new JComboBox<>();
        try (Connection conn = DBConnection.getConnection(); ResultSet rs = conn.createStatement().executeQuery("SELECT adm_AT FROM admin")) {
            while (rs.next()) c.addItem(rs.getString(1));
        } catch (Exception e) {}
        return c;
    }
}
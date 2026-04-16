import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class GuidePanel extends JPanel {
    private JTable table;
    private DefaultTableModel model;

    public GuidePanel() {
        setLayout(new BorderLayout());
        model = new DefaultTableModel(new String[]{"Guide AT", "CV"}, 0){
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
        JButton btnEdit = new JButton("Edit CV");
        JButton btnDelete = new JButton("Remove Role");

        btnPanel.add(btnLoad); btnPanel.add(btnAdd); btnPanel.add(btnEdit); btnPanel.add(btnDelete);
        add(btnPanel, BorderLayout.SOUTH);

        btnLoad.addActionListener(e -> loadGuides());
        btnAdd.addActionListener(e -> addGuide());
        btnEdit.addActionListener(e -> editGuide());
        btnDelete.addActionListener(e -> deleteGuide());
        loadGuides();
    }

    private void loadGuides() {
        model.setRowCount(0);
        try (Connection conn = DBConnection.getConnection(); ResultSet rs = conn.createStatement().executeQuery("SELECT * FROM guide")) {
            while (rs.next()) model.addRow(new Object[]{rs.getString("gui_AT"), rs.getString("gui_cv")});
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void addGuide() {
        JComboBox<String> workerCombo = getWorkerDropdown();
        JTextField cvField = new JTextField();
        Object[] msg = {"Worker:", workerCombo, "CV (Description):", cvField};

        if (JOptionPane.showConfirmDialog(this, msg, "Assign Guide Role", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement("INSERT INTO guide VALUES (?, ?)")) {
                ps.setString(1, ((String)workerCombo.getSelectedItem()).split(" - ")[0]);
                ps.setString(2, cvField.getText());
                ps.executeUpdate();
                loadGuides();
            } catch (Exception e) { JOptionPane.showMessageDialog(this, "Error: " + e.getMessage()); }
        }
    }

    private void editGuide() {
        int row = table.getSelectedRow();
        if (row == -1) { JOptionPane.showMessageDialog(this, "Select a guide."); return; }

        String at = (String) model.getValueAt(row, 0);
        String val = JOptionPane.showInputDialog(this, "Enter new CV:");

        if (val != null) {
            try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement("UPDATE guide SET gui_cv=? WHERE gui_AT=?")) {
                ps.setString(1, val);
                ps.setString(2, at);
                ps.executeUpdate();
                loadGuides();
            } catch (Exception e) { JOptionPane.showMessageDialog(this, "Error: " + e.getMessage()); }
        }
    }

    private void deleteGuide() {
        int row = table.getSelectedRow();
        if (row != -1 && JOptionPane.showConfirmDialog(this, "Remove Role?", "Confirm", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement("DELETE FROM guide WHERE gui_AT=?")) {
                ps.setString(1, (String) model.getValueAt(row, 0));
                ps.executeUpdate();
                loadGuides();
            } catch (SQLException e) { JOptionPane.showMessageDialog(this, "Error: " + e.getMessage()); }
        }
    }

    private JComboBox<String> getWorkerDropdown() {
        JComboBox<String> c = new JComboBox<>();
        String sql = "SELECT wrk_AT, wrk_lname FROM worker WHERE wrk_AT NOT IN (SELECT gui_AT FROM guide)";
        try (Connection conn = DBConnection.getConnection(); ResultSet rs = conn.createStatement().executeQuery(sql)) {
            while (rs.next()) c.addItem(rs.getString(1) + " - " + rs.getString(2));
        } catch (Exception e) {}
        return c;
    }
}
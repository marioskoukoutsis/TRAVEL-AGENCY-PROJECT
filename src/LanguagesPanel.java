import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class LanguagesPanel extends JPanel {
    private JTable table;
    private DefaultTableModel model;

    public LanguagesPanel() {
        setLayout(new BorderLayout());
        model = new DefaultTableModel(new String[]{"Code", "Name"}, 0){
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = new JTable(model);
        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel btnPanel = new JPanel();
        JButton btnLoad = new JButton("Refresh");
        JButton btnAdd = new JButton("Add Language");
        JButton btnEdit = new JButton("Edit Name");
        JButton btnDelete = new JButton("Delete");

        btnPanel.add(btnLoad); btnPanel.add(btnAdd); btnPanel.add(btnEdit); btnPanel.add(btnDelete);
        add(btnPanel, BorderLayout.SOUTH);

        btnLoad.addActionListener(e -> loadLang());
        btnAdd.addActionListener(e -> addLang());
        btnEdit.addActionListener(e -> editLang());
        btnDelete.addActionListener(e -> deleteLang());
        loadLang();
    }

    private void loadLang() {
        model.setRowCount(0);
        try(Connection c = DBConnection.getConnection(); ResultSet rs = c.createStatement().executeQuery("SELECT * FROM language_ref")) {
            while(rs.next()) model.addRow(new Object[]{rs.getString(1), rs.getString(2)});
        } catch (SQLException e) {}
    }

    private void addLang() {
        JTextField c = new JTextField(); JTextField n = new JTextField();
        Object[] msg = {"Code (e.g., EN):", c, "Name (e.g., English):", n};
        if (JOptionPane.showConfirmDialog(this, msg, "Add Language", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement("INSERT INTO language_ref VALUES (?, ?)")) {
                ps.setString(1, c.getText()); ps.setString(2, n.getText()); ps.executeUpdate(); loadLang();
            } catch (Exception e) { JOptionPane.showMessageDialog(this, "Error: " + e.getMessage()); }
        }
    }

    private void editLang() {
        int row = table.getSelectedRow();
        if (row == -1) { JOptionPane.showMessageDialog(this, "Select a language."); return; }

        String code = (String) model.getValueAt(row, 0);
        String val = JOptionPane.showInputDialog(this, "New Name for " + code + ":");
        if (val != null) {
            try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement("UPDATE language_ref SET lang_name=? WHERE lang_code=?")) {
                ps.setString(1, val); ps.setString(2, code); ps.executeUpdate(); loadLang();
            } catch (Exception e) { JOptionPane.showMessageDialog(this, "Error: " + e.getMessage()); }
        }
    }

    private void deleteLang() {
        int row = table.getSelectedRow();
        if (row != -1 && JOptionPane.showConfirmDialog(this, "Delete Language?", "Confirm", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement("DELETE FROM language_ref WHERE lang_code=?")) {
                ps.setString(1, (String) model.getValueAt(row, 0)); ps.executeUpdate(); loadLang();
            } catch (Exception e) { JOptionPane.showMessageDialog(this, "Error: " + e.getMessage()); }
        }
    }
}
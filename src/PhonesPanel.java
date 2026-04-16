import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class PhonesPanel extends JPanel {
    private JTable table;
    private DefaultTableModel model;

    public PhonesPanel() {
        setLayout(new BorderLayout());
        model = new DefaultTableModel(new String[]{"Branch Code", "Phone Number"}, 0){
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = new JTable(model);
        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel btnPanel = new JPanel();
        JButton btnLoad = new JButton("Refresh");
        JButton btnAdd = new JButton("Add Phone");
        JButton btnEdit = new JButton("Edit Number");
        JButton btnDelete = new JButton("Delete");

        btnPanel.add(btnLoad); btnPanel.add(btnAdd); btnPanel.add(btnEdit); btnPanel.add(btnDelete);
        add(btnPanel, BorderLayout.SOUTH);

        btnLoad.addActionListener(e -> loadPhones());
        btnAdd.addActionListener(e -> addPhone());
        btnEdit.addActionListener(e -> editPhone());
        btnDelete.addActionListener(e -> deletePhone());
        loadPhones();
    }

    private void loadPhones() {
        model.setRowCount(0);
        try(Connection c = DBConnection.getConnection(); ResultSet rs = c.createStatement().executeQuery("SELECT * FROM phones")) {
            while(rs.next()) model.addRow(new Object[]{rs.getInt(1), rs.getString(2)});
        } catch (SQLException e) {}
    }

    private void addPhone() {
        JComboBox<String> brCombo = new JComboBox<>();
        try(Connection c = DBConnection.getConnection(); ResultSet rs = c.createStatement().executeQuery("SELECT br_code, br_city FROM branch")) {
            while(rs.next()) brCombo.addItem(rs.getInt(1) + " - " + rs.getString(2));
        } catch(Exception e){}

        JTextField ph = new JTextField();
        Object[] msg = {"Branch:", brCombo, "Phone Number:", ph};

        if (JOptionPane.showConfirmDialog(this, msg, "Add Phone", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement("INSERT INTO phones VALUES (?, ?)")) {
                ps.setInt(1, Integer.parseInt(((String)brCombo.getSelectedItem()).split(" - ")[0]));
                ps.setString(2, ph.getText());
                ps.executeUpdate();
                loadPhones();
            } catch (Exception e) { JOptionPane.showMessageDialog(this, "Error: " + e.getMessage()); }
        }
    }

    private void editPhone() {
        int row = table.getSelectedRow();
        if (row == -1) { JOptionPane.showMessageDialog(this, "Select a phone."); return; }

        int brCode = (int) model.getValueAt(row, 0);
        String oldNum = (String) model.getValueAt(row, 1);
        String val = JOptionPane.showInputDialog(this, "Enter new number:");

        if (val != null) {
            try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement("UPDATE phones SET ph_number=? WHERE ph_br_code=? AND ph_number=?")) {
                ps.setString(1, val);
                ps.setInt(2, brCode);
                ps.setString(3, oldNum);
                ps.executeUpdate();
                loadPhones();
            } catch (SQLException e) { JOptionPane.showMessageDialog(this, "Error: " + e.getMessage()); }
        }
    }

    private void deletePhone() {
        int row = table.getSelectedRow();
        if (row != -1 && JOptionPane.showConfirmDialog(this, "Delete Phone?", "Confirm", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement("DELETE FROM phones WHERE ph_br_code=? AND ph_number=?")) {
                ps.setInt(1, (int) model.getValueAt(row, 0));
                ps.setString(2, (String) model.getValueAt(row, 1));
                ps.executeUpdate();
                loadPhones();
            } catch (SQLException e) { JOptionPane.showMessageDialog(this, "Error: " + e.getMessage()); }
        }
    }
}
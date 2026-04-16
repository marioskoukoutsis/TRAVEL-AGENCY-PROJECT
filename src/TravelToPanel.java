import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class TravelToPanel extends JPanel {
    private JTable table;
    private DefaultTableModel model;

    public TravelToPanel() {
        setLayout(new BorderLayout());

        model = new DefaultTableModel(new String[]{"Trip ID", "Dest ID", "Destination Name", "Arrival", "Departure", "Sequence"}, 0){
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = new JTable(model);
        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel btnPanel = new JPanel();
        JButton btnLoad = new JButton("Refresh");
        JButton btnAdd = new JButton("Add Stop");
        JButton btnEdit = new JButton("Edit Stop"); // NEW
        JButton btnDelete = new JButton("Delete Stop");

        btnPanel.add(btnLoad);
        btnPanel.add(btnAdd);
        btnPanel.add(btnEdit);
        btnPanel.add(btnDelete);
        add(btnPanel, BorderLayout.SOUTH);

        btnLoad.addActionListener(e -> loadData());
        btnAdd.addActionListener(e -> addStop());
        btnEdit.addActionListener(e -> editStop());
        btnDelete.addActionListener(e -> deleteStop());

        loadData();
    }

    private void loadData() {
        model.setRowCount(0);
        String sql = "SELECT tt.*, d.dst_name FROM travel_to tt JOIN destination d ON tt.to_dst_id = d.dst_id ORDER BY tt.to_tr_id, tt.to_sequence";
        try (Connection conn = DBConnection.getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                model.addRow(new Object[]{
                        rs.getInt("to_tr_id"), rs.getInt("to_dst_id"), rs.getString("dst_name"),
                        rs.getString("to_arrival"), rs.getString("to_departure"), rs.getInt("to_sequence")
                });
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void addStop() {
        JComboBox<ComboItem> tripCombo = new JComboBox<>();
        JComboBox<ComboItem> destCombo = new JComboBox<>();
        JTextField arr = new JTextField();
        JTextField dep = new JTextField();
        JTextField seq = new JTextField();

        try (Connection conn = DBConnection.getConnection(); Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT tr_id, tr_departure FROM trip");
            while (rs.next()) tripCombo.addItem(new ComboItem(rs.getInt(1), "Trip " + rs.getInt(1)));
            rs = stmt.executeQuery("SELECT dst_id, dst_name FROM destination");
            while (rs.next()) destCombo.addItem(new ComboItem(rs.getInt(1), rs.getString(2)));
        } catch (SQLException e) { e.printStackTrace(); }

        JPanel p = new JPanel(new GridLayout(0, 2));
        p.add(new JLabel("Trip:")); p.add(tripCombo);
        p.add(new JLabel("Destination:")); p.add(destCombo);
        p.add(new JLabel("Arrival:")); p.add(arr);
        p.add(new JLabel("Departure:")); p.add(dep);
        p.add(new JLabel("Sequence:")); p.add(seq);

        if (JOptionPane.showConfirmDialog(this, p, "Add Trip Stop", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement("INSERT INTO travel_to VALUES (?, ?, ?, ?, ?)")) {
                ps.setInt(1, ((ComboItem) tripCombo.getSelectedItem()).getId());
                ps.setInt(2, ((ComboItem) destCombo.getSelectedItem()).getId());
                ps.setString(3, arr.getText());
                ps.setString(4, dep.getText());
                ps.setInt(5, Integer.parseInt(seq.getText()));
                ps.executeUpdate();
                loadData();
            } catch (Exception e) { JOptionPane.showMessageDialog(this, "Error: " + e.getMessage()); }
        }
    }

    private void editStop() {
        int row = table.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Please select a row to edit.");
            return;
        }

        int trId = (int) model.getValueAt(row, 0);
        int dstId = (int) model.getValueAt(row, 1);

        String[] columnNames = {"Arrival", "Departure", "Sequence"};
        String[] dbColumns = {"to_arrival", "to_departure", "to_sequence"};

        JComboBox<String> colCombo = new JComboBox<>(columnNames);
        if (JOptionPane.showConfirmDialog(this, colCombo, "Select column to edit", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {

            int selectedIndex = colCombo.getSelectedIndex();
            String selectedDbColumn = dbColumns[selectedIndex];

            String newValue = JOptionPane.showInputDialog(this, "Enter new value for " + columnNames[selectedIndex] + ":");

            if (newValue != null && !newValue.trim().isEmpty()) {
                try (Connection conn = DBConnection.getConnection()) {
                    String sql = "UPDATE travel_to SET " + selectedDbColumn + " = ? WHERE to_tr_id = ? AND to_dst_id = ?";
                    PreparedStatement pstmt = conn.prepareStatement(sql);
                    pstmt.setString(1, newValue);
                    pstmt.setInt(2, trId);
                    pstmt.setInt(3, dstId);

                    pstmt.executeUpdate();
                    JOptionPane.showMessageDialog(this, "Updated successfully!");
                    loadData();
                } catch (SQLException e) {
                    JOptionPane.showMessageDialog(this, "Error updating: " + e.getMessage());
                }
            }
        }
    }

    private void deleteStop() {
        int row = table.getSelectedRow();
        if (row != -1) {
            int tId = (int) model.getValueAt(row, 0);
            int dId = (int) model.getValueAt(row, 1);
            if (JOptionPane.showConfirmDialog(this, "Delete stop?") == JOptionPane.YES_OPTION) {
                try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement("DELETE FROM travel_to WHERE to_tr_id=? AND to_dst_id=?")) {
                    ps.setInt(1, tId);
                    ps.setInt(2, dId);
                    ps.executeUpdate();
                    loadData();
                } catch (SQLException e) { JOptionPane.showMessageDialog(this, "Error: " + e.getMessage()); }
            }
        }
    }
}
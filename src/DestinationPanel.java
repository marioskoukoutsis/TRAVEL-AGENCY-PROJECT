import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class DestinationPanel extends JPanel {
    private JTable table;
    private DefaultTableModel model;

    public DestinationPanel() {
        setLayout(new BorderLayout());

        model = new DefaultTableModel(new String[]{"ID", "Name", "Description", "Type", "Language", "Location ID"}, 0){
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = new JTable(model);
        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel btnPanel = new JPanel();
        JButton btnLoad = new JButton("Refresh");
        JButton btnAdd = new JButton("Add Destination");
        JButton btnEdit = new JButton("Edit Destination");
        JButton btnDelete = new JButton("Delete Destination");

        btnPanel.add(btnLoad);
        btnPanel.add(btnAdd);
        btnPanel.add(btnEdit);
        btnPanel.add(btnDelete);
        add(btnPanel, BorderLayout.SOUTH);

        btnLoad.addActionListener(e -> loadDestinations());
        btnAdd.addActionListener(e -> addDestination());
        btnEdit.addActionListener(e -> editDestination());
        btnDelete.addActionListener(e -> deleteDestination());

        loadDestinations();
    }

    private void loadDestinations() {
        model.setRowCount(0);
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM destination")) {
            while (rs.next()) {
                model.addRow(new Object[]{
                        rs.getInt("dst_id"), rs.getString("dst_name"), rs.getString("dst_descr"),
                        rs.getString("dst_rtype"), rs.getString("dst_language_code"), rs.getObject("dst_location")
                });
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void addDestination() {
        JTextField name = new JTextField();
        JTextField descr = new JTextField();
        JComboBox<String> type = new JComboBox<>(new String[]{"LOCAL", "ABROAD"});
        JTextField lang = new JTextField();
        JTextField loc = new JTextField();

        Object[] msg = {"Name:", name, "Description:", descr, "Type:", type, "Language:", lang, "Parent Location ID:", loc};

        if (JOptionPane.showConfirmDialog(this, msg, "Add Destination", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement("INSERT INTO destination (dst_name, dst_descr, dst_rtype, dst_language_code, dst_location) VALUES (?,?,?,?,?)")) {
                pstmt.setString(1, name.getText());
                pstmt.setString(2, descr.getText());
                pstmt.setString(3, (String)type.getSelectedItem());
                pstmt.setString(4, lang.getText());
                if(loc.getText().isEmpty()) pstmt.setNull(5, Types.INTEGER);
                else pstmt.setInt(5, Integer.parseInt(loc.getText()));

                pstmt.executeUpdate();
                loadDestinations();
            } catch (Exception e) { JOptionPane.showMessageDialog(this, "Error: " + e.getMessage()); }
        }
    }

    private void editDestination() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a destination to edit.");
            return;
        }

        int id = (int) model.getValueAt(selectedRow, 0);

        String[] columnNames = {"Name", "Description", "Type (LOCAL/ABROAD)", "Language", "Location ID"};
        String[] dbColumns = {"dst_name", "dst_descr", "dst_rtype", "dst_language_code", "dst_location"};

        JComboBox<String> colCombo = new JComboBox<>(columnNames);
        if (JOptionPane.showConfirmDialog(this, colCombo, "Select column to edit", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {

            int selectedIndex = colCombo.getSelectedIndex();
            String selectedDbColumn = dbColumns[selectedIndex];

            String newValue = JOptionPane.showInputDialog(this, "Enter new value for " + columnNames[selectedIndex] + ":");

            if (newValue != null) { // Επιτρέπουμε κενό string αν ο χρήστης θέλει να σβήσει κάτι
                try (Connection conn = DBConnection.getConnection()) {
                    String sql = "UPDATE destination SET " + selectedDbColumn + " = ? WHERE dst_id = ?";
                    PreparedStatement pstmt = conn.prepareStatement(sql);

                    if (selectedDbColumn.equals("dst_location") && newValue.trim().isEmpty()) {
                        pstmt.setNull(1, Types.INTEGER);
                    } else {
                        pstmt.setString(1, newValue);
                    }
                    pstmt.setInt(2, id);

                    pstmt.executeUpdate();
                    JOptionPane.showMessageDialog(this, "Updated successfully!");
                    loadDestinations();
                } catch (SQLException e) {
                    JOptionPane.showMessageDialog(this, "Error updating: " + e.getMessage());
                }
            }
        }
    }

    private void deleteDestination() {
        int row = table.getSelectedRow();
        if (row != -1) {
            int id = (int) model.getValueAt(row, 0);
            if (JOptionPane.showConfirmDialog(this, "Delete Destination ID " + id + "?") == JOptionPane.YES_OPTION) {
                try (Connection conn = DBConnection.getConnection();
                     PreparedStatement pstmt = conn.prepareStatement("DELETE FROM destination WHERE dst_id=?")) {
                    pstmt.setInt(1, id);
                    pstmt.executeUpdate();
                    loadDestinations();
                } catch (SQLException e) { JOptionPane.showMessageDialog(this, "Error: " + e.getMessage()); }
            }
        }
    }
}
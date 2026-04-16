import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

class AmenitiesPanel extends JPanel {
    private JTable table;
    private DefaultTableModel model;

    public AmenitiesPanel() {
        setLayout(new BorderLayout());

        model = new DefaultTableModel(new String[]{"ID", "Name"}, 0);
        table = new JTable(model);
        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel btnPanel = new JPanel();
        JButton btnLoad = new JButton("Refresh Data");
        JButton btnAdd = new JButton("Add Amenity");
        JButton btnEdit = new JButton("Edit Amenity");
        JButton btnDelete = new JButton("Delete Amenity");

        btnPanel.add(btnLoad);
        btnPanel.add(btnAdd);
        btnPanel.add(btnEdit);
        btnPanel.add(btnDelete);
        add(btnPanel, BorderLayout.SOUTH);

        btnLoad.addActionListener(e -> loadAmenities());
        btnAdd.addActionListener(e -> addAmenity());
        btnEdit.addActionListener(e -> editAmenity());
        btnDelete.addActionListener(e -> deleteAmenity());

        loadAmenities();
    }

    private void loadAmenities() {
        model.setRowCount(0);
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM amenity")) {
            while (rs.next()) {
                model.addRow(new Object[]{
                        rs.getInt("am_id"),
                        rs.getString("am_name")
                });
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void addAmenity() {
        JTextField name = new JTextField();

        Object[] message = {"Name:", name};
        int option = JOptionPane.showConfirmDialog(this, message, "Add Amenity", JOptionPane.OK_CANCEL_OPTION);

        if (option == JOptionPane.OK_OPTION) {
            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement("INSERT INTO amenity (am_name) VALUES (?)")) {
                pstmt.setString(1, name.getText());
                pstmt.executeUpdate();
                loadAmenities();
            } catch (SQLException e) { JOptionPane.showMessageDialog(this, "Error: " + e.getMessage()); }
        }
    }

    private void editAmenity() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select an amenity to edit first.");
            return;
        }

        int amenityId = (int) model.getValueAt(selectedRow, 0);

        String[] columnNames = {"Name"};

        String[] dbColumns = {"am_name"};

        JComboBox<String> colCombo = new JComboBox<>(columnNames);

        int input = JOptionPane.showConfirmDialog(this, colCombo, "Select column to edit", JOptionPane.OK_CANCEL_OPTION);

        if (input == JOptionPane.OK_OPTION) {
            int selectedIndex = colCombo.getSelectedIndex();
            String selectedDbColumn = dbColumns[selectedIndex];

            String newValue = JOptionPane.showInputDialog(this, "Enter new value for " + columnNames[selectedIndex] + ":");

            if (newValue != null && !newValue.trim().isEmpty()) {
                try (Connection conn = DBConnection.getConnection()) {
                    String sql = "UPDATE amenity SET " + selectedDbColumn + " = ? WHERE am_id = ?";

                    PreparedStatement pstmt = conn.prepareStatement(sql);
                    pstmt.setString(1, newValue);
                    pstmt.setInt(2, amenityId);

                    int rows = pstmt.executeUpdate();
                    if (rows > 0) {
                        JOptionPane.showMessageDialog(this, "Updated successfully!");
                        loadAmenities();
                    }
                } catch (SQLException e) {
                    JOptionPane.showMessageDialog(this, "Error updating: " + e.getMessage());
                }
            }
        }
    }

    private void deleteAmenity() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select an amenity to delete.");
            return;
        }

        int id = (int) model.getValueAt(selectedRow, 0);

        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete this amenity?", "Confirm Delete", JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement("DELETE FROM amenity WHERE am_id = ?")) {

                pstmt.setInt(1, id);
                int rows = pstmt.executeUpdate();

                if (rows > 0) {
                    JOptionPane.showMessageDialog(this, "Amenity deleted successfully.");
                    loadAmenities();
                }
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Error deleting amenity" + e.getMessage());
            }
        }
    }
}

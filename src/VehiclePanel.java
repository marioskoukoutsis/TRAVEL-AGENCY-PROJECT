import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

class VehiclePanel extends JPanel {
    private JTable table;
    private DefaultTableModel model;

    public VehiclePanel() {
        setLayout(new BorderLayout());

        model = new DefaultTableModel(new String[]{"ID", "Brand", "Model", "Plate", "Capacity", "Type", "Status", "Total Kilometers"}, 0){
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = new JTable(model);
        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel btnPanel = new JPanel();
        JButton btnLoad = new JButton("Refresh");
        JButton btnAdd = new JButton("Add Vehicle");
        JButton btnEdit = new JButton("Edit Vehicle");
        JButton btnDelete = new JButton("Delete Vehicle");

        btnPanel.add(btnLoad);
        btnPanel.add(btnAdd);
        btnPanel.add(btnEdit);
        btnPanel.add(btnDelete);
        add(btnPanel, BorderLayout.SOUTH);

        btnLoad.addActionListener(e -> loadVehicles());
        btnAdd.addActionListener(e -> addVehicle());
        btnEdit.addActionListener(e -> editVehicle());
        btnDelete.addActionListener(e -> deleteVehicle());

        loadVehicles();
    }

    private void loadVehicles() {
        model.setRowCount(0);
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM vehicle")) {
            while (rs.next()) {
                model.addRow(new Object[]{
                        rs.getInt("vehicle_id"), rs.getString("brand"), rs.getString("model"),
                        rs.getString("plate_number"), rs.getString("capacity"), rs.getString("type"), rs.getString("status"), rs.getString("total_miles")
                });
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void addVehicle() {
        JTextField brand = new JTextField();
        JTextField modelField = new JTextField();
        JTextField plate = new JTextField();
        JTextField capacity = new JTextField();
        JTextField total_miles = new JTextField();

        String[] types = {"CAR", "VAN", "MINIBUS", "BUS"};
        JComboBox<String> typeCombo = new JComboBox<>(types);

        Object[] message = {
                "Brand:", brand, "Model:", modelField, "Plate:", plate,
                "Capacity:", capacity, "Type:", typeCombo, "Total Kilometres:", total_miles
        };

        int option = JOptionPane.showConfirmDialog(this, message, "Add Vehicle", JOptionPane.OK_CANCEL_OPTION);

        if (option == JOptionPane.OK_OPTION) {
            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(
                         "INSERT INTO vehicle (brand, model, plate_number, capacity, type, status, total_miles) VALUES (?, ?, ?, ?, ?, 'AVAILABLE', ?)")) {

                pstmt.setString(1, brand.getText());
                pstmt.setString(2, modelField.getText());
                pstmt.setString(3, plate.getText());
                pstmt.setInt(4, Integer.parseInt(capacity.getText()));
                pstmt.setString(5, (String) typeCombo.getSelectedItem());
                pstmt.setInt(6, Integer.parseInt(total_miles.getText()));

                pstmt.executeUpdate();
                loadVehicles();
            } catch (Exception e) { JOptionPane.showMessageDialog(this, "Error: " + e.getMessage()); }
        }
    }

    private void editVehicle() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a vehicle to edit first.");
            return;
        }

        int vehicleId = (int) model.getValueAt(selectedRow, 0);

        String[] columnNames = {"Brand", "Model", "Plate", "Capacity", "Total Kilometers"};

        String[] dbColumns = {"brand", "model", "plate_number", "capacity", "total_miles"};

        JComboBox<String> colCombo = new JComboBox<>(columnNames);

        int input = JOptionPane.showConfirmDialog(this, colCombo, "Select column to edit", JOptionPane.OK_CANCEL_OPTION);

        if (input == JOptionPane.OK_OPTION) {
            int selectedIndex = colCombo.getSelectedIndex();
            String selectedDbColumn = dbColumns[selectedIndex];

            String newValue = JOptionPane.showInputDialog(this, "Enter new value for " + columnNames[selectedIndex] + ":");

            if (newValue != null && !newValue.trim().isEmpty()) {
                try (Connection conn = DBConnection.getConnection()) {
                    String sql = "UPDATE vehicle SET " + selectedDbColumn + " = ? WHERE vehicle_id = ?";

                    PreparedStatement pstmt = conn.prepareStatement(sql);
                    pstmt.setString(1, newValue);
                    pstmt.setInt(2, vehicleId);

                    int rows = pstmt.executeUpdate();
                    if (rows > 0) {
                        JOptionPane.showMessageDialog(this, "Updated successfully!");
                        loadVehicles();
                    }
                } catch (SQLException e) {
                    JOptionPane.showMessageDialog(this, "Error updating: " + e.getMessage());
                }
            }
        }
    }

    private void deleteVehicle() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a vehicle to delete.");
            return;
        }

        int id = (int) model.getValueAt(selectedRow, 0);

        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete this vehicle?", "Confirm Delete", JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement("DELETE FROM vehicle WHERE vehicle_id = ?")) {

                pstmt.setInt(1, id);
                int rows = pstmt.executeUpdate();

                if (rows > 0) {
                    JOptionPane.showMessageDialog(this, "Vehicle deleted successfully.");
                    loadVehicles();
                }
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Error deleting vehicle: " + e.getMessage());
            }
        }
    }
}
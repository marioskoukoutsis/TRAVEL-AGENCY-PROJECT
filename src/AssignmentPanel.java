import javax.swing.*;
import java.awt.*;
import java.sql.*;

class AssignmentPanel extends JPanel {
    private JComboBox<String> tripCombo;
    private JComboBox<String> vehicleCombo;
    private JTextField kmField;
    private JButton btnFilter;
    private JButton btnAssign;

    public AssignmentPanel() {
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        tripCombo = new JComboBox<>();
        vehicleCombo = new JComboBox<>();
        kmField = new JTextField(10);
        btnFilter = new JButton("Find Suitable Vehicles");
        btnAssign = new JButton("Assign Vehicle");

        loadTrips();

        gbc.gridx = 0; gbc.gridy = 0; add(new JLabel("Select Trip:"), gbc);
        gbc.gridx = 1; gbc.gridy = 0; add(tripCombo, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 2; add(btnFilter, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 1; add(new JLabel("Select Available Vehicle:"), gbc);
        gbc.gridx = 1; gbc.gridy = 2; add(vehicleCombo, gbc);

        gbc.gridx = 0; gbc.gridy = 3; add(new JLabel("Current Odometer (KM):"), gbc);
        gbc.gridx = 1; gbc.gridy = 3; add(kmField, gbc);

        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2; add(btnAssign, gbc);

        btnFilter.addActionListener(e -> {
            vehicleCombo.removeAllItems();
            String selectedTrip = (String) tripCombo.getSelectedItem();
            if (selectedTrip == null) return;

            int trId = Integer.parseInt(selectedTrip.split(" - ")[0]);

            String query =
                    "SELECT v.vehicle_id, v.brand, v.model, v.capacity " +
                            "FROM vehicle v " +
                            "JOIN trip t ON t.tr_id = ? " +
                            "JOIN driver d ON t.tr_drv_AT = d.drv_AT " +
                            "WHERE v.status = 'AVAILABLE' " +
                            "AND v.capacity >= (SELECT COUNT(*) FROM reservation WHERE res_tr_id = t.tr_id AND res_status != 'CANCELLED') " +
                            "AND NOT (v.capacity > 9 AND (d.drv_license = 'A' OR d.drv_license = 'B'))";

            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(query)) {

                pstmt.setInt(1, trId);
                ResultSet rs = pstmt.executeQuery();

                boolean found = false;
                while (rs.next()) {
                    vehicleCombo.addItem(rs.getInt("vehicle_id") + " - " +
                            rs.getString("brand") + " " +
                            rs.getString("model") +
                            " (" + rs.getInt("capacity") + " seats)");
                    found = true;
                }

                if (!found) {
                    JOptionPane.showMessageDialog(this, "No suitable vehicles found (Check Availability, Capacity, or Driver License)!");
                } else {
                    JOptionPane.showMessageDialog(this, "Vehicles filtered based on Capacity, Availability & Driver License!");
                }

            } catch (SQLException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Database Error: " + ex.getMessage());
            }
        });

        btnAssign.addActionListener(e -> {
            if (tripCombo.getSelectedItem() == null || vehicleCombo.getSelectedItem() == null) {
                JOptionPane.showMessageDialog(this, "Please select Trip and Vehicle!");
                return;
            }

            try {
                int trId = Integer.parseInt(((String) tripCombo.getSelectedItem()).split(" - ")[0]);
                int vId = Integer.parseInt(((String) vehicleCombo.getSelectedItem()).split(" - ")[0]);
                int km = Integer.parseInt(kmField.getText());

                try (Connection conn = DBConnection.getConnection();
                     CallableStatement cs = conn.prepareCall("{CALL vehicle_assignment(?, ?, ?)}")) {

                    cs.setInt(1, trId);
                    cs.setInt(2, vId);
                    cs.setInt(3, km);

                    boolean hasResult = cs.execute();
                    if (hasResult) {
                        ResultSet rs = cs.getResultSet();
                        if (rs.next()) JOptionPane.showMessageDialog(this, rs.getString(1));
                    }

                    // Refresh
                    loadTrips();
                    vehicleCombo.removeAllItems();
                    kmField.setText("");

                } catch (SQLException ex) {
                    JOptionPane.showMessageDialog(this, "Procedure Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Please enter valid Kilometers!");
            }
        });

    }

    private void loadTrips() {
        tripCombo.removeAllItems();
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT tr_id, GROUP_CONCAT(dst_name SEPARATOR ', ') as dests FROM trip JOIN travel_to ON tr_id = to_tr_id JOIN destination ON to_dst_id = dst_id WHERE tr_vehicle_id IS NULL AND tr_status IN ('PLANNED', 'CONFIRMED') GROUP BY tr_id")) {
            while (rs.next()) {
                tripCombo.addItem(rs.getInt("tr_id") + " - " + rs.getString("dests"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
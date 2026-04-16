import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

class ReservationPanel extends JPanel {
    private JTable table;
    private DefaultTableModel model;

    public ReservationPanel() {
        setLayout(new BorderLayout());

        model = new DefaultTableModel(new String[]{"Trip ID", "Seat #", "Customer ID", "Status", "Cost"}, 0){
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = new JTable(model);
        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel btnPanel = new JPanel();
        JButton btnLoad = new JButton("Refresh Reservations");
        JButton btnAdd = new JButton("New Reservation");
        JButton btnEdit = new JButton("Edit Reservation");
        JButton btnDelete = new JButton("Delete Reservation");

        btnPanel.add(btnLoad);
        btnPanel.add(btnAdd);
        btnPanel.add(btnEdit);
        btnPanel.add(btnDelete);
        add(btnPanel, BorderLayout.SOUTH);

        btnLoad.addActionListener(e -> loadReservations());
        btnAdd.addActionListener(e -> createReservation());
        btnEdit.addActionListener(e -> editReservation());
        btnDelete.addActionListener(e -> deleteReservation());

        loadReservations();
    }

    private void loadReservations() {
        model.setRowCount(0);
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM reservation")) {
            while (rs.next()) {
                model.addRow(new Object[]{
                        rs.getInt("res_tr_id"), rs.getInt("res_seatnum"), rs.getInt("res_cust_id"),
                        rs.getString("res_status"), rs.getDouble("res_total_cost")
                });
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void createReservation() {
        JComboBox<String> tripCombo = new JComboBox<>();
        JComboBox<String> custCombo = new JComboBox<>();
        JTextField seatField = new JTextField();
        JTextField costField = new JTextField();

        try (Connection conn = DBConnection.getConnection(); Statement stmt = conn.createStatement()) {
            ResultSet rsTrips = stmt.executeQuery("SELECT tr_id, GROUP_CONCAT(dst_name SEPARATOR ', ') AS dst_name FROM trip JOIN travel_to ON tr_id=to_tr_id JOIN destination ON to_dst_id=dst_id GROUP BY tr_id");
            while (rsTrips.next()) tripCombo.addItem(rsTrips.getInt("tr_id") + " - " + rsTrips.getString("dst_name"));

            ResultSet rsCust = stmt.executeQuery("SELECT cust_id, cust_lname, cust_name FROM customer");
            while (rsCust.next()) custCombo.addItem(rsCust.getInt("cust_id") + " - " + rsCust.getString("cust_lname") + " " + rsCust.getString("cust_name"));

        } catch (SQLException e) { e.printStackTrace(); }

        Object[] message = {
                "Select Trip:", tripCombo,
                "Select Customer:", custCombo,
                "Seat Number:", seatField,
                "Total Cost:", costField
        };

        int option = JOptionPane.showConfirmDialog(this, message, "New Reservation", JOptionPane.OK_CANCEL_OPTION);

        if (option == JOptionPane.OK_OPTION) {
            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement("INSERT INTO reservation (res_tr_id, res_seatnum, res_cust_id, res_status, res_total_cost) VALUES (?, ?, ?, 'CONFIRMED', ?)")) {

                int trId = Integer.parseInt(((String) tripCombo.getSelectedItem()).split(" - ")[0]);
                int custId = Integer.parseInt(((String) custCombo.getSelectedItem()).split(" - ")[0]);

                pstmt.setInt(1, trId);
                pstmt.setInt(2, Integer.parseInt(seatField.getText()));
                pstmt.setInt(3, custId);
                pstmt.setDouble(4, Double.parseDouble(costField.getText()));

                pstmt.executeUpdate();
                loadReservations();
            } catch (Exception e) { JOptionPane.showMessageDialog(this, "Error: " + e.getMessage()); }
        }
    }
    private void editReservation() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a reservation to edit first.");
            return;
        }

        int trId = (int) model.getValueAt(selectedRow, 0);
        int custId = (int) model.getValueAt(selectedRow, 2);

        String[] columnNames = {"Seat #", "Status", "Cost"};
        String[] dbColumns = {"res_seatnum", "res_status", "res_total_cost"};

        JComboBox<String> colCombo = new JComboBox<>(columnNames);
        int input = JOptionPane.showConfirmDialog(this, colCombo, "Select column to edit", JOptionPane.OK_CANCEL_OPTION);

        if (input == JOptionPane.OK_OPTION) {
            int selectedIndex = colCombo.getSelectedIndex();
            String selectedDbColumn = dbColumns[selectedIndex];
            String newValue = JOptionPane.showInputDialog(this, "Enter new value for " + columnNames[selectedIndex] + ":");

            if (newValue != null && !newValue.trim().isEmpty()) {
                try (Connection conn = DBConnection.getConnection()) {

                    String sql = "UPDATE reservation SET " + selectedDbColumn + " = ? WHERE res_tr_id = ? AND res_cust_id = ?";

                    PreparedStatement pstmt = conn.prepareStatement(sql);

                    if (selectedDbColumn.equals("res_total_cost") || selectedDbColumn.equals("res_seatnum")) {
                        pstmt.setString(1, newValue);
                    } else {
                        pstmt.setString(1, newValue);
                    }

                    pstmt.setInt(2, trId);   // WHERE res_tr_id = ...
                    pstmt.setInt(3, custId); // AND res_cust_id = ...

                    int rows = pstmt.executeUpdate();
                    if (rows > 0) {
                        JOptionPane.showMessageDialog(this, "Updated successfully!");
                        loadReservations();
                    }
                } catch (SQLException e) {
                    JOptionPane.showMessageDialog(this, "Error updating: " + e.getMessage());
                }
            }
        }
    }

    private void deleteReservation() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a reservation to delete.");
            return;
        }

        int trId = (int) model.getValueAt(selectedRow, 0);
        int custId = (int) model.getValueAt(selectedRow, 2);

        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete this reservation?", "Confirm Delete", JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement("DELETE FROM reservation WHERE res_tr_id = ? AND res_cust_id = ?")) {

                pstmt.setInt(1, trId);
                pstmt.setInt(2, custId);

                int rows = pstmt.executeUpdate();

                if (rows > 0) {
                    JOptionPane.showMessageDialog(this, "Reservation deleted successfully.");
                    loadReservations();
                }
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Error deleting reservation: " + e.getMessage());
            }
        }
    }
}
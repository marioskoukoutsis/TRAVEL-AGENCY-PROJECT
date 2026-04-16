import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.util.Vector;

public class TripPanel extends JPanel {
    private JTable table;
    private DefaultTableModel model;

    public TripPanel() {
        setLayout(new BorderLayout());

        String[] columns = {
                "ID", "Departure", "Return", "Status", "Seats", "Min Part.", "Cost Adult", "Cost Child", "Miles",
                "Branch", "Guide", "Driver", "Vehicle",
                "HID_BR", "HID_GUI", "HID_DRV"
        };

        model = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };

        table = new JTable(model);
        for(int i=13; i<=15; i++) {
            table.getColumnModel().getColumn(i).setMinWidth(0);
            table.getColumnModel().getColumn(i).setMaxWidth(0);
            table.getColumnModel().getColumn(i).setWidth(0);
        }

        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel btnPanel = new JPanel();
        JButton btnLoad = new JButton("Refresh");
        JButton btnAdd = new JButton("Add Trip");
        JButton btnEdit = new JButton("Edit Trip");
        JButton btnDelete = new JButton("Delete Trip");

        btnPanel.add(btnLoad);
        btnPanel.add(btnAdd);
        btnPanel.add(btnEdit);
        btnPanel.add(btnDelete);
        add(btnPanel, BorderLayout.SOUTH);

        btnLoad.addActionListener(e -> loadTrips());
        btnAdd.addActionListener(e -> openTripDialog(null));
        btnEdit.addActionListener(e -> editTripAction());
        btnDelete.addActionListener(e -> deleteTripAction());

        loadTrips();
    }

    private void loadTrips() {
        model.setRowCount(0);
        String sql = "SELECT t.*, b.br_city, " +
                "CONCAT(wg.wrk_lname, ' ', wg.wrk_name) as guide_name, " +
                "CONCAT(wd.wrk_lname, ' ', wd.wrk_name) as driver_name, " +
                "CONCAT(v.model,' (', v.plate_number, ')') as vehicle_info " +
                "FROM trip t " +
                "JOIN branch b ON t.tr_br_code = b.br_code " +
                "JOIN guide g ON t.tr_gui_AT = g.gui_AT " +
                "JOIN worker wg ON g.gui_AT = wg.wrk_AT " +
                "JOIN driver d ON t.tr_drv_AT = d.drv_AT " +
                "JOIN worker wd ON d.drv_AT = wd.wrk_AT " +
                "LEFT JOIN vehicle v ON t.tr_vehicle_id = v.vehicle_id " +
                "ORDER BY t.tr_departure DESC";

        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Vector<Object> row = new Vector<>();
                row.add(rs.getInt("tr_id"));
                row.add(rs.getString("tr_departure"));
                row.add(rs.getString("tr_return"));
                row.add(rs.getString("tr_status"));
                row.add(rs.getInt("tr_maxseats"));
                row.add(rs.getInt("tr_min_participants"));
                row.add(rs.getDouble("tr_cost_adult"));
                row.add(rs.getDouble("tr_cost_child"));
                row.add(rs.getInt("miles_driven"));
                row.add(rs.getString("br_city"));
                row.add(rs.getString("guide_name"));
                row.add(rs.getString("driver_name"));

                String veh = rs.getString("vehicle_info");
                row.add(veh != null ? veh : "-");

                row.add(rs.getInt("tr_br_code"));
                row.add(rs.getString("tr_gui_AT"));
                row.add(rs.getString("tr_drv_AT"));

                model.addRow(row);
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void openTripDialog(Integer tripIdToEdit) {
        JTextField departField = new JTextField(15);
        JTextField returnField = new JTextField(15);
        JTextField seatsField = new JTextField(5);
        JTextField minPartField = new JTextField(5);
        JTextField costAdultField = new JTextField(8);
        JTextField costChildField = new JTextField(8);
        JTextField milesField = new JTextField(8);

        JComboBox<String> statusCombo = new JComboBox<>(new String[]{"PLANNED", "CONFIRMED", "ACTIVE", "COMPLETED", "CANCELLED"});
        JComboBox<ComboItem> branchCombo = new JComboBox<>();
        JComboBox<StringComboItem> guideCombo = new JComboBox<>();
        JComboBox<StringComboItem> driverCombo = new JComboBox<>();

        fillCombos(branchCombo, guideCombo, driverCombo);

        if (tripIdToEdit != null) {
            int row = table.getSelectedRow();
            departField.setText(model.getValueAt(row, 1).toString());
            returnField.setText(model.getValueAt(row, 2).toString());
            statusCombo.setSelectedItem(model.getValueAt(row, 3).toString());
            seatsField.setText(model.getValueAt(row, 4).toString());
            minPartField.setText(model.getValueAt(row, 5).toString());
            costAdultField.setText(model.getValueAt(row, 6).toString());
            costChildField.setText(model.getValueAt(row, 7).toString());
            milesField.setText(model.getValueAt(row, 8).toString());

            setSelectedInt(branchCombo, (int)model.getValueAt(row, 13));
            setSelectedString(guideCombo, (String)model.getValueAt(row, 14));
            setSelectedString(driverCombo, (String)model.getValueAt(row, 15));
        }

        JPanel panel = new JPanel(new GridLayout(0, 2));
        panel.add(new JLabel("Departure (YYYY-MM-DD HH:MM:SS):")); panel.add(departField);
        panel.add(new JLabel("Return (YYYY-MM-DD HH:MM:SS):")); panel.add(returnField);
        panel.add(new JLabel("Status:")); panel.add(statusCombo);
        panel.add(new JLabel("Max Seats:")); panel.add(seatsField);
        panel.add(new JLabel("Min Participants:")); panel.add(minPartField);
        panel.add(new JLabel("Cost Adult:")); panel.add(costAdultField);
        panel.add(new JLabel("Cost Child:")); panel.add(costChildField);
        panel.add(new JLabel("Miles Driven:")); panel.add(milesField);
        panel.add(new JLabel("Branch:")); panel.add(branchCombo);
        panel.add(new JLabel("Guide:")); panel.add(guideCombo);
        panel.add(new JLabel("Driver:")); panel.add(driverCombo);

        int result = JOptionPane.showConfirmDialog(null, panel,
                tripIdToEdit == null ? "Add New Trip" : "Edit Trip", JOptionPane.OK_CANCEL_OPTION);

        if (result == JOptionPane.OK_OPTION) {
            saveTrip(tripIdToEdit, departField, returnField, seatsField, minPartField,
                    costAdultField, costChildField, milesField, statusCombo,
                    branchCombo, guideCombo, driverCombo);
        }
    }

    private void saveTrip(Integer id, JTextField dep, JTextField ret, JTextField seats, JTextField min,
                          JTextField cAd, JTextField cCh, JTextField miles, JComboBox<String> stat,
                          JComboBox<ComboItem> br, JComboBox<StringComboItem> gui,
                          JComboBox<StringComboItem> drv) {
        try (Connection conn = DBConnection.getConnection()) {
            String sql;
            if (id == null) {
                sql = "INSERT INTO trip (tr_departure, tr_return, tr_maxseats, tr_min_participants, tr_cost_adult, tr_cost_child, miles_driven, tr_status, tr_br_code, tr_gui_AT, tr_drv_AT, tr_vehicle_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NULL)";
            } else {
                sql = "UPDATE trip SET tr_departure=?, tr_return=?, tr_maxseats=?, tr_min_participants=?, tr_cost_adult=?, tr_cost_child=?, miles_driven=?, tr_status=?, tr_br_code=?, tr_gui_AT=?, tr_drv_AT=? WHERE tr_id=?";
            }

            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, dep.getText());
            stmt.setString(2, ret.getText());
            stmt.setInt(3, Integer.parseInt(seats.getText()));
            stmt.setInt(4, Integer.parseInt(min.getText()));
            stmt.setDouble(5, Double.parseDouble(cAd.getText()));
            stmt.setDouble(6, Double.parseDouble(cCh.getText()));
            stmt.setInt(7, miles.getText().isEmpty() ? 0 : Integer.parseInt(miles.getText()));
            stmt.setString(8, (String) stat.getSelectedItem());
            stmt.setInt(9, ((ComboItem) br.getSelectedItem()).getId());
            stmt.setString(10, ((StringComboItem) gui.getSelectedItem()).getId());
            stmt.setString(11, ((StringComboItem) drv.getSelectedItem()).getId());

            if (id != null) stmt.setInt(12, id);

            stmt.executeUpdate();
            loadTrips();
            JOptionPane.showMessageDialog(this, "Success!");

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
        }
    }

    private void deleteTripAction() {
        int row = table.getSelectedRow();
        if(row != -1 && JOptionPane.showConfirmDialog(this, "Delete Trip?") == JOptionPane.YES_OPTION) {
            try(Connection conn = DBConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement("DELETE FROM trip WHERE tr_id=?")) {
                stmt.setInt(1, (int) model.getValueAt(row, 0));
                stmt.executeUpdate();
                loadTrips();
            } catch(SQLException ex) { JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage()); }
        }
    }

    private void editTripAction() {
        int row = table.getSelectedRow();
        if(row != -1) openTripDialog((int) model.getValueAt(row, 0));
    }

    private void fillCombos(JComboBox<ComboItem> br, JComboBox<StringComboItem> gui, JComboBox<StringComboItem> drv) {
        try (Connection conn = DBConnection.getConnection(); Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT br_code, br_city FROM branch");
            while(rs.next()) br.addItem(new ComboItem(rs.getInt(1), rs.getString(2)));

            rs = stmt.executeQuery("SELECT g.gui_AT, w.wrk_lname, w.wrk_name FROM guide g JOIN worker w ON g.gui_AT=w.wrk_AT");
            while(rs.next()) gui.addItem(new StringComboItem(rs.getString(1), rs.getString(2) + " " + rs.getString(3)));

            rs = stmt.executeQuery("SELECT d.drv_AT, w.wrk_lname, w.wrk_name FROM driver d JOIN worker w ON d.drv_AT=w.wrk_AT");
            while(rs.next()) drv.addItem(new StringComboItem(rs.getString(1), rs.getString(2) + " " + rs.getString(3)));

        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void setSelectedInt(JComboBox<ComboItem> cb, int id) {
        for(int i=0; i<cb.getItemCount(); i++) if(cb.getItemAt(i).getId() == id) { cb.setSelectedIndex(i); return; }
    }
    private void setSelectedString(JComboBox<StringComboItem> cb, String id) {
        for(int i=0; i<cb.getItemCount(); i++) if(cb.getItemAt(i).getId().equals(id)) { cb.setSelectedIndex(i); return; }
    }
}
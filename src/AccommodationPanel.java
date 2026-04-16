import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class AccommodationPanel extends JPanel {
    private JTable table;
    private DefaultTableModel model;

    public AccommodationPanel() {
        setLayout(new BorderLayout());

        String[] cols = {"ID", "Name", "Type", "Stars", "Rating", "Status", "Address", "City", "Rooms", "Price", "Dest ID"};
        model = new DefaultTableModel(cols, 0){
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = new JTable(model);
        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel btnPanel = new JPanel();
        JButton btnLoad = new JButton("Refresh");
        JButton btnAdd = new JButton("Add Accommodation");
        JButton btnEdit = new JButton("Edit Accommodation");
        JButton btnDelete = new JButton("Delete Accommodation");

        btnPanel.add(btnLoad);
        btnPanel.add(btnAdd);
        btnPanel.add(btnEdit);
        btnPanel.add(btnDelete);
        add(btnPanel, BorderLayout.SOUTH);

        btnLoad.addActionListener(e -> loadData());
        btnAdd.addActionListener(e -> addAccommodation());
        btnEdit.addActionListener(e -> editAccommodation());
        btnDelete.addActionListener(e -> deleteAccommodation());

        loadData();
    }

    private void loadData() {
        model.setRowCount(0);
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM accommodation")) {
            while (rs.next()) {
                model.addRow(new Object[]{
                        rs.getInt("acc_id"), rs.getString("acc_name"), rs.getString("acc_type"),
                        rs.getInt("acc_stars"), rs.getDouble("acc_rating"), rs.getString("acc_status"),
                        rs.getString("acc_address"), rs.getString("acc_city"), rs.getInt("acc_rooms"),
                        rs.getDouble("acc_room_price"), rs.getInt("acc_destination_id")
                });
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void addAccommodation() {
        JTextField name = new JTextField();
        JComboBox<String> type = new JComboBox<>(new String[]{"Hotel", "Hostel", "Resort", "Apartment", "Room"});
        JTextField stars = new JTextField("0");
        JTextField rating = new JTextField("0.0");
        JComboBox<String> status = new JComboBox<>(new String[]{"Active", "Renovation", "Ended"});
        JTextField address = new JTextField();
        JTextField city = new JTextField();
        JTextField rooms = new JTextField();
        JTextField price = new JTextField();
        JComboBox<ComboItem> destCombo = new JComboBox<>();

        try (Connection conn = DBConnection.getConnection(); Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT dst_id, dst_name FROM destination")) {
            while (rs.next()) destCombo.addItem(new ComboItem(rs.getInt(1), rs.getString(2)));
        } catch (SQLException e) { e.printStackTrace(); }

        JPanel p = new JPanel(new GridLayout(0, 2));
        p.add(new JLabel("Name:")); p.add(name);
        p.add(new JLabel("Type:")); p.add(type);
        p.add(new JLabel("Stars (1-5):")); p.add(stars);
        p.add(new JLabel("Rating (0-5):")); p.add(rating);
        p.add(new JLabel("Status:")); p.add(status);
        p.add(new JLabel("Address:")); p.add(address);
        p.add(new JLabel("City:")); p.add(city);
        p.add(new JLabel("Rooms:")); p.add(rooms);
        p.add(new JLabel("Price/Night:")); p.add(price);
        p.add(new JLabel("Destination:")); p.add(destCombo);

        if (JOptionPane.showConfirmDialog(this, p, "Add Accommodation", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            String sql = "INSERT INTO accommodation (acc_name, acc_type, acc_stars, acc_rating, acc_status, acc_address, acc_city, acc_rooms, acc_room_price, acc_destination_id) VALUES (?,?,?,?,?,?,?,?,?,?)";
            try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, name.getText());
                ps.setString(2, (String) type.getSelectedItem());
                ps.setInt(3, Integer.parseInt(stars.getText()));
                ps.setDouble(4, Double.parseDouble(rating.getText()));
                ps.setString(5, (String) status.getSelectedItem());
                ps.setString(6, address.getText());
                ps.setString(7, city.getText());
                ps.setInt(8, Integer.parseInt(rooms.getText()));
                ps.setDouble(9, Double.parseDouble(price.getText()));
                ps.setInt(10, ((ComboItem) destCombo.getSelectedItem()).getId());

                ps.executeUpdate();
                loadData();
            } catch (Exception e) { JOptionPane.showMessageDialog(this, "Error: " + e.getMessage()); }
        }
    }

    private void editAccommodation() {
        int row = table.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Please select an accommodation.");
            return;
        }

        int id = (int) model.getValueAt(row, 0);

        String[] columnNames = {"Name", "Type", "Stars", "Rating", "Status", "Address", "City", "Rooms", "Price"};
        String[] dbColumns = {"acc_name", "acc_type", "acc_stars", "acc_rating", "acc_status", "acc_address", "acc_city", "acc_rooms", "acc_room_price"};

        JComboBox<String> colCombo = new JComboBox<>(columnNames);
        if (JOptionPane.showConfirmDialog(this, colCombo, "Select column to edit", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {

            int index = colCombo.getSelectedIndex();
            String dbCol = dbColumns[index];

            String val = JOptionPane.showInputDialog(this, "Enter new value for " + columnNames[index] + ":");

            if (val != null && !val.trim().isEmpty()) {
                try (Connection conn = DBConnection.getConnection()) {
                    String sql = "UPDATE accommodation SET " + dbCol + " = ? WHERE acc_id = ?";
                    PreparedStatement ps = conn.prepareStatement(sql);

                    if (dbCol.equals("acc_stars") || dbCol.equals("acc_rooms")) {
                        ps.setInt(1, Integer.parseInt(val));
                    } else if (dbCol.equals("acc_rating") || dbCol.equals("acc_room_price")) {
                        ps.setDouble(1, Double.parseDouble(val));
                    } else {
                        ps.setString(1, val);
                    }

                    ps.setInt(2, id);
                    ps.executeUpdate();
                    loadData();
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
                }
            }
        }
    }

    private void deleteAccommodation() {
        int row = table.getSelectedRow();
        if (row != -1) {
            int id = (int) model.getValueAt(row, 0);
            if (JOptionPane.showConfirmDialog(this, "Delete ID " + id + "?") == JOptionPane.YES_OPTION) {
                try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement("DELETE FROM accommodation WHERE acc_id=?")) {
                    ps.setInt(1, id);
                    ps.executeUpdate();
                    loadData();
                } catch (SQLException e) { JOptionPane.showMessageDialog(this, "Error: " + e.getMessage()); }
            }
        }
    }
}
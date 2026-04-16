import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class EventPanel extends JPanel {
    private JTable table;
    private DefaultTableModel model;

    public EventPanel() {
        setLayout(new BorderLayout());

        model = new DefaultTableModel(new String[]{"Trip ID", "Start", "End", "Description"}, 0){
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = new JTable(model);
        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel btnPanel = new JPanel();
        JButton btnLoad = new JButton("Refresh");
        JButton btnAdd = new JButton("Add Event");
        JButton btnEdit = new JButton("Edit Event");
        JButton btnDelete = new JButton("Delete Event");

        btnPanel.add(btnLoad);
        btnPanel.add(btnAdd);
        btnPanel.add(btnEdit);
        btnPanel.add(btnDelete);
        add(btnPanel, BorderLayout.SOUTH);

        btnLoad.addActionListener(e -> loadEvents());
        btnAdd.addActionListener(e -> openEventDialog(null, null));
        btnEdit.addActionListener(e -> editEventAction());
        btnDelete.addActionListener(e -> deleteEventAction());

        loadEvents();
    }

    private void loadEvents() {
        model.setRowCount(0);
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM event ORDER BY ev_start ASC")) {
            while (rs.next()) {
                model.addRow(new Object[]{
                        rs.getInt("ev_tr_id"),
                        rs.getString("ev_start"),
                        rs.getString("ev_end"),
                        rs.getString("ev_descr")
                });
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void openEventDialog(Integer tripId, String startTime) {
        JComboBox<ComboItem> tripCombo = new JComboBox<>();
        JTextField startField = new JTextField(15);
        JTextField endField = new JTextField(15);
        JTextField descrField = new JTextField(20);

        try (Connection conn = DBConnection.getConnection(); Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT tr_id, tr_departure FROM trip")) {
            while (rs.next()) {
                tripCombo.addItem(new ComboItem(rs.getInt(1), "Trip " + rs.getInt(1) + " (" + rs.getString(2) + ")"));
            }
        } catch (SQLException e) { e.printStackTrace(); }

        if (tripId != null && startTime != null) {
            int row = table.getSelectedRow();
            setSelectedTrip(tripCombo, tripId);
            startField.setText(startTime);
            endField.setText(model.getValueAt(row, 2).toString());
            descrField.setText(model.getValueAt(row, 3).toString());
            startField.setEditable(false);
            tripCombo.setEnabled(false);
        }

        JPanel panel = new JPanel(new GridLayout(0, 2));
        panel.add(new JLabel("Select Trip:")); panel.add(tripCombo);
        panel.add(new JLabel("Start (YYYY-MM-DD HH:MM:SS):")); panel.add(startField);
        panel.add(new JLabel("End (YYYY-MM-DD HH:MM:SS):")); panel.add(endField);
        panel.add(new JLabel("Description:")); panel.add(descrField);

        int res = JOptionPane.showConfirmDialog(this, panel, tripId == null ? "Add Event" : "Edit Event", JOptionPane.OK_CANCEL_OPTION);

        if (res == JOptionPane.OK_OPTION) {
            String sql;
            if (tripId == null) sql = "INSERT INTO event (ev_tr_id, ev_start, ev_end, ev_descr) VALUES (?, ?, ?, ?)";
            else sql = "UPDATE event SET ev_end=?, ev_descr=? WHERE ev_tr_id=? AND ev_start=?";

            try (Connection conn = DBConnection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
                if (tripId == null) {
                    pstmt.setInt(1, ((ComboItem) tripCombo.getSelectedItem()).getId());
                    pstmt.setString(2, startField.getText());
                    pstmt.setString(3, endField.getText());
                    pstmt.setString(4, descrField.getText());
                } else {
                    pstmt.setString(1, endField.getText());
                    pstmt.setString(2, descrField.getText());
                    pstmt.setInt(3, tripId);
                    pstmt.setString(4, startTime);
                }
                pstmt.executeUpdate();
                loadEvents();
            } catch (SQLException ex) { JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage()); }
        }
    }

    private void editEventAction() {
        int row = table.getSelectedRow();
        if (row != -1) {
            int tId = (int) model.getValueAt(row, 0);
            String start = (String) model.getValueAt(row, 1);
            openEventDialog(tId, start);
        }
    }

    private void deleteEventAction() {
        int row = table.getSelectedRow();
        if (row != -1) {
            int tId = (int) model.getValueAt(row, 0);
            String start = (String) model.getValueAt(row, 1);
            if (JOptionPane.showConfirmDialog(this, "Delete event?") == JOptionPane.YES_OPTION) {
                try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement("DELETE FROM event WHERE ev_tr_id=? AND ev_start=?")) {
                    ps.setInt(1, tId);
                    ps.setString(2, start);
                    ps.executeUpdate();
                    loadEvents();
                } catch (SQLException e) { JOptionPane.showMessageDialog(this, "Error: " + e.getMessage()); }
            }
        }
    }

    private void setSelectedTrip(JComboBox<ComboItem> cb, int id) {
        for (int i = 0; i < cb.getItemCount(); i++) {
            if (cb.getItemAt(i).getId() == id) { cb.setSelectedIndex(i); return; }
        }
    }
}
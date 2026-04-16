import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class TripHistoryPanel extends JPanel {
    private JTable table;
    private DefaultTableModel model;

    public TripHistoryPanel() {
        setLayout(new BorderLayout());

        model = new DefaultTableModel(new String[]{"Historic ID", "Trip ID", "Departure Date", "Return Date", "Destinations #", "Participants #", "Total Revenue"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table = new JTable(model);

        table.setFillsViewportHeight(true);

        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel btnPanel = new JPanel();
        JButton btnRefresh = new JButton("Refresh");

        btnRefresh.setFont(new Font("Arial", Font.BOLD, 14));

        btnPanel.add(btnRefresh);
        add(btnPanel, BorderLayout.SOUTH);

        btnRefresh.addActionListener(e -> loadTripHistory());

        loadTripHistory();
    }

    private void loadTripHistory() {
        model.setRowCount(0);

        String query = "SELECT * FROM trip_history ORDER BY hist_id";

        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                model.addRow(new Object[]{
                        rs.getInt("hist_id"),
                        rs.getInt("trip_id"),
                        rs.getDate("departure_date"),
                        rs.getDate("return_date"),
                        rs.getInt("count_destinations"),
                        rs.getInt("count_participants"),
                        rs.getDouble("total_revenue")
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading trip history: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

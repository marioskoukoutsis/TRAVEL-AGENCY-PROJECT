import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class LogPanel extends JPanel {
    private JTable table;
    private DefaultTableModel model;

    public LogPanel() {
        setLayout(new BorderLayout());

        model = new DefaultTableModel(new String[]{"Log ID", "User", "Action Type", "Table Affected", "Date/Time", "Description"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table = new JTable(model);

        table.setFillsViewportHeight(true);

        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel btnPanel = new JPanel();
        JButton btnRefresh = new JButton("Refresh Logs");

        btnRefresh.setFont(new Font("Arial", Font.BOLD, 14));

        btnPanel.add(btnRefresh);
        add(btnPanel, BorderLayout.SOUTH);

        btnRefresh.addActionListener(e -> loadLogs());

        loadLogs();
    }

    private void loadLogs() {
        model.setRowCount(0);

        String query = "SELECT * FROM system_log ORDER BY log_id";

        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                model.addRow(new Object[]{
                        rs.getInt("log_id"),
                        rs.getString("log_user"),
                        rs.getString("log_action_type"),
                        rs.getString("log_table_name"),
                        rs.getString("log_date"),
                        rs.getString("log_description")
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading logs: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

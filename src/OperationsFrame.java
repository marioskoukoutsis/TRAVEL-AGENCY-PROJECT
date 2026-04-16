import javax.swing.*;

public class OperationsFrame extends JFrame {

    public OperationsFrame() {
        setTitle("Travel Agency Management System");
        setSize(1300, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JTabbedPane mainTabs = new JTabbedPane();

        mainTabs.addTab("Destinations", new DestinationPanel());
        mainTabs.addTab("Trips", new TripPanel());
        mainTabs.addTab("Trip Program", new TravelToPanel());
        mainTabs.addTab("Trip History", new TripHistoryPanel());
        mainTabs.addTab("Events", new EventPanel());
        mainTabs.addTab("Accommodation", new AccommodationPanel());
        mainTabs.addTab("Amenities", new AmenitiesPanel());
        mainTabs.addTab("Customers", new CustomerPanel());
        mainTabs.addTab("Vehicles", new VehiclePanel());
        mainTabs.addTab("Reservations", new ReservationPanel());
        mainTabs.addTab("Assign Vehicle", new AssignmentPanel());
        mainTabs.addTab("Branches", new BranchPanel());
        mainTabs.addTab("Workers", new WorkerPanel());
        mainTabs.addTab("Drivers", new DriverPanel());
        mainTabs.addTab("Guides", new GuidePanel());
        mainTabs.addTab("Languages", new LanguagesPanel());
        mainTabs.addTab("Phones", new PhonesPanel());
        mainTabs.addTab("Admin", new AdminPanel());
        mainTabs.addTab("System Logs", new LogPanel());
        add(mainTabs);
    }

    public static void main(String[] args) {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception e) {}

        SwingUtilities.invokeLater(() -> new OperationsFrame().setVisible(true));
    }
}
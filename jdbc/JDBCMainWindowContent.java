package jdbcAssignment;
import java.awt.*;
import java.awt.event.*;
import java.io.FileWriter;
import java.io.PrintWriter;
import javax.swing.*;
import javax.swing.border.*;
import java.sql.*;
import java.math.BigDecimal;

@SuppressWarnings("serial")
public class JDBCMainWindowContent extends JInternalFrame implements ActionListener
{
    String cmd = null;

    // DB Connectivity Attributes
    private Connection con = null;
    private Statement stmt = null;
    private ResultSet rs = null;

    private Container content;

    private JPanel detailsPanel;
    private JPanel exportButtonPanel;
    private JScrollPane dbContentsPanel;

    private Border lineBorder;

    // Labels and TextFields for Trim (sports car trim/variant)
    private JLabel trimIDLabel = new JLabel("Trim ID:                 ");
    private JLabel modelIDLabel = new JLabel("Model ID:               ");
    private JLabel trimNameLabel = new JLabel("Trim Name:      ");
    private JLabel msrpLabel = new JLabel("MSRP ($):        ");
    private JLabel engineIDLabel = new JLabel("Engine ID:                 ");
    private JLabel transmissionLabel = new JLabel("Transmission:               ");
    private JLabel drivetrainLabel = new JLabel("Drivetrain:      ");

    private JTextField trimIDTF = new JTextField(10);
    private JTextField modelIDTF = new JTextField(10);
    private JTextField trimNameTF = new JTextField(10);
    private JTextField msrpTF = new JTextField(10);
    private JTextField engineIDTF = new JTextField(10);
    private JTextField transmissionTF = new JTextField(10);
    private JTextField drivetrainTF = new JTextField(10);

    private static QueryTableModel TableModel = new QueryTableModel();
    private JTable TableofDBContents = new JTable(TableModel);

    // CRUD Buttons
    private JButton updateButton = new JButton("Update");
    private JButton insertButton = new JButton("Insert");
    private JButton exportButton = new JButton("Export Trims");
    private JButton deleteButton = new JButton("Delete");
    private JButton clearButton = new JButton("Clear");

    // Query/Export Buttons
    private JButton topModelsButton = new JButton("Top Models by Revenue");
    private JButton orderDetailsButton = new JButton("All Order Details");
    private JButton trimsByMakeButton = new JButton("Trims by Make:");
    private JTextField trimsByMakeTF = new JTextField(12);
    private JButton trimsAbovePriceButton = new JButton("Trims Above Price:");
    private JTextField trimsAbovePriceTF = new JTextField(12);

    public JDBCMainWindowContent(String aTitle)
    {
        super(aTitle, false, false, false, false);
        setEnabled(true);

        initiate_db_conn();

        content = getContentPane();
        content.setLayout(null);
        content.setBackground(Color.lightGray);
        lineBorder = BorderFactory.createEtchedBorder(15, Color.red, Color.black);

        // Setup details panel (CRUD panel)
        detailsPanel = new JPanel();
        detailsPanel.setLayout(new GridLayout(8, 2));
        detailsPanel.setBackground(Color.lightGray);
        detailsPanel.setBorder(BorderFactory.createTitledBorder(lineBorder, "CRUD Actions - Trim Table"));

        detailsPanel.add(trimIDLabel);
        detailsPanel.add(trimIDTF);
        detailsPanel.add(modelIDLabel);
        detailsPanel.add(modelIDTF);
        detailsPanel.add(trimNameLabel);
        detailsPanel.add(trimNameTF);
        detailsPanel.add(msrpLabel);
        detailsPanel.add(msrpTF);
        detailsPanel.add(engineIDLabel);
        detailsPanel.add(engineIDTF);
        detailsPanel.add(transmissionLabel);
        detailsPanel.add(transmissionTF);
        detailsPanel.add(drivetrainLabel);
        detailsPanel.add(drivetrainTF);

        // Setup export/query panel
        exportButtonPanel = new JPanel();
        exportButtonPanel.setLayout(new GridLayout(3, 2));
        exportButtonPanel.setBackground(Color.lightGray);
        exportButtonPanel.setBorder(BorderFactory.createTitledBorder(lineBorder, "Export Data"));
        exportButtonPanel.add(topModelsButton);
        exportButtonPanel.add(orderDetailsButton);
        exportButtonPanel.add(trimsByMakeButton);
        exportButtonPanel.add(trimsByMakeTF);
        exportButtonPanel.add(trimsAbovePriceButton);
        exportButtonPanel.add(trimsAbovePriceTF);
        exportButtonPanel.setSize(500, 200);
        exportButtonPanel.setLocation(3, 300);
        content.add(exportButtonPanel);

        // CRUD Button sizing and positioning
        insertButton.setSize(120, 30);
        updateButton.setSize(120, 30);
        exportButton.setSize(120, 30);
        deleteButton.setSize(120, 30);
        clearButton.setSize(120, 30);

        insertButton.setLocation(370, 10);
        updateButton.setLocation(370, 50);
        deleteButton.setLocation(370, 90);
        exportButton.setLocation(370, 130);
        clearButton.setLocation(370, 170);

        // Add listeners
        insertButton.addActionListener(this);
        updateButton.addActionListener(this);
        exportButton.addActionListener(this);
        deleteButton.addActionListener(this);
        clearButton.addActionListener(this);
        topModelsButton.addActionListener(this);
        orderDetailsButton.addActionListener(this);
        trimsByMakeButton.addActionListener(this);
        trimsAbovePriceButton.addActionListener(this);

        content.add(insertButton);
        content.add(updateButton);
        content.add(exportButton);
        content.add(deleteButton);
        content.add(clearButton);

        TableofDBContents.setPreferredScrollableViewportSize(new Dimension(900, 300));

        dbContentsPanel = new JScrollPane(TableofDBContents, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        dbContentsPanel.setBackground(Color.lightGray);
        dbContentsPanel.setBorder(BorderFactory.createTitledBorder(lineBorder, "Database Content"));

        detailsPanel.setSize(360, 300);
        detailsPanel.setLocation(3, 0);
        dbContentsPanel.setSize(700, 300);
        dbContentsPanel.setLocation(500, 0);

        content.add(detailsPanel);
        content.add(dbContentsPanel);

        setSize(982, 645);
        setVisible(true);

        // Load initial data
        refreshTable("SELECT * FROM `trim`");
    }

    public void initiate_db_conn()
    {
        try
        {
            // Load the JConnector Driver
            Class.forName("com.mysql.cj.jdbc.Driver");
            
            // CHANGE PORT TO 3306 if XAMPP uses default, or keep 3307 if that's your config
            String url = "jdbc:mysql://localhost:3307/sportscars?useSSL=false&serverTimezone=UTC";

            // Connect to DB using DB URL, Username and password
            con = DriverManager.getConnection(url, "root", "");

            // Create a generic statement
            stmt = con.createStatement();
            
            System.out.println("Database connection established successfully!");
        }
        catch(Exception e)
        {
            System.out.println("Error: Failed to connect to database\n" + e.getMessage());
            e.printStackTrace();
        }
    }

    // Event handling 
    public void actionPerformed(ActionEvent e)
    {
        Object target = e.getSource();
        
        if (target == clearButton)
        {
            trimIDTF.setText("");
            modelIDTF.setText("");
            trimNameTF.setText("");
            msrpTF.setText("");
            engineIDTF.setText("");
            transmissionTF.setText("");
            drivetrainTF.setText("");
        }

        // INSERT - uses PreparedStatement to prevent SQL injection
        if (target == insertButton)
        { 
            try
            {
                String sql = "INSERT INTO `trim` (model_id, name, msrp, engine_id, transmission, drivetrain) VALUES (?, ?, ?, ?, ?, ?)";
                PreparedStatement pstmt = con.prepareStatement(sql);
                
                pstmt.setInt(1, Integer.parseInt(modelIDTF.getText()));
                pstmt.setString(2, trimNameTF.getText());
                pstmt.setBigDecimal(3, new BigDecimal(msrpTF.getText()));
                pstmt.setInt(4, Integer.parseInt(engineIDTF.getText()));
                pstmt.setString(5, transmissionTF.getText());
                pstmt.setString(6, drivetrainTF.getText());
                
                pstmt.executeUpdate();
                pstmt.close();
                
                JOptionPane.showMessageDialog(this, "Trim inserted successfully!");
            }
            catch (Exception ex)
            {
                System.err.println("Error with insert:\n" + ex.toString());
                JOptionPane.showMessageDialog(this, "Insert failed: " + ex.getMessage());
            }
            finally
            {
                refreshTable("SELECT * FROM `trim`");
            }
        }

        // UPDATE - updates based on trim_id
        if (target == updateButton)
        {
            try
            {
                String sql = "UPDATE `trim` SET model_id=?, name=?, msrp=?, engine_id=?, transmission=?, drivetrain=? WHERE trim_id=?";
                PreparedStatement pstmt = con.prepareStatement(sql);
                
                pstmt.setInt(1, Integer.parseInt(modelIDTF.getText()));
                pstmt.setString(2, trimNameTF.getText());
                pstmt.setBigDecimal(3, new BigDecimal(msrpTF.getText()));
                pstmt.setInt(4, Integer.parseInt(engineIDTF.getText()));
                pstmt.setString(5, transmissionTF.getText());
                pstmt.setString(6, drivetrainTF.getText());
                pstmt.setInt(7, Integer.parseInt(trimIDTF.getText()));
                
                int rows = pstmt.executeUpdate();
                pstmt.close();
                
                if(rows > 0) {
                    JOptionPane.showMessageDialog(this, "Trim updated successfully!");
                } else {
                    JOptionPane.showMessageDialog(this, "No trim found with that ID.");
                }
            }
            catch (Exception ex)
            {
                System.err.println("Error with update:\n" + ex.toString());
                JOptionPane.showMessageDialog(this, "Update failed: " + ex.getMessage());
            }
            finally
            {
                refreshTable("SELECT * FROM `trim`");
            }
        }

        // DELETE - removes trim by trim_id
        if (target == deleteButton)
        {
            try
            {
                String sql = "DELETE FROM `trim` WHERE trim_id=?";
                PreparedStatement pstmt = con.prepareStatement(sql);
                pstmt.setInt(1, Integer.parseInt(trimIDTF.getText()));
                
                int rows = pstmt.executeUpdate();
                pstmt.close();
                
                if(rows > 0) {
                    JOptionPane.showMessageDialog(this, "Trim deleted successfully!");
                } else {
                    JOptionPane.showMessageDialog(this, "No trim found with that ID.");
                }
            }
            catch (Exception ex)
            {
                System.err.println("Error with delete:\n" + ex.toString());
                JOptionPane.showMessageDialog(this, "Delete failed: " + ex.getMessage());
            }
            finally
            {
                refreshTable("SELECT * FROM `trim`");
            }
        }

        // EXPORT all trims to CSV
        if (target == exportButton)
        {
            cmd = "SELECT * FROM `trim`";
            try {
                rs = stmt.executeQuery(cmd);
                writeToFile(rs, "all_trims.csv");
                JOptionPane.showMessageDialog(this, "Exported to all_trims.csv");
            }
            catch(Exception e1) {
                e1.printStackTrace();
            }
        }

        // Export top models by revenue (uses view)
        if (target == topModelsButton)
        {
            cmd = "SELECT * FROM v_top_models";
            try {
                rs = stmt.executeQuery(cmd);
                writeToFile(rs, "top_models_revenue.csv");
                JOptionPane.showMessageDialog(this, "Exported to top_models_revenue.csv");
            }
            catch(Exception e1) {
                e1.printStackTrace();
            }
        }

        // Export all order details with joins
        if (target == orderDetailsButton)
        {
            cmd = "SELECT o.order_id, o.order_date, CONCAT(c.first_name, ' ', c.last_name) AS customer, " +
                  "d.name AS dealership, mk.name AS make, m.name AS model, t.name AS trim, " +
                  "oi.quantity, oi.unit_price, o.total_amount " +
                  "FROM orders o " +
                  "JOIN customer c ON c.customer_id = o.customer_id " +
                  "JOIN dealership d ON d.dealership_id = o.dealership_id " +
                  "JOIN order_item oi ON oi.order_id = o.order_id " +
                  "JOIN `trim` t ON t.trim_id = oi.trim_id " +
                  "JOIN model m ON m.model_id = t.model_id " +
                  "JOIN make mk ON mk.make_id = m.make_id " +
                  "ORDER BY o.order_id";
            try {
                rs = stmt.executeQuery(cmd);
                writeToFile(rs, "order_details.csv");
                JOptionPane.showMessageDialog(this, "Exported to order_details.csv");
            }
            catch(Exception e1) {
                e1.printStackTrace();
            }
        }

        // Trims by make name (parameterized query)
        if (target == trimsByMakeButton)
        {
            String makeName = trimsByMakeTF.getText();
            cmd = "SELECT mk.name AS make, m.name AS model, t.name AS trim, t.msrp " +
                  "FROM `trim` t " +
                  "JOIN model m ON m.model_id = t.model_id " +
                  "JOIN make mk ON mk.make_id = m.make_id " +
                  "WHERE mk.name = '" + makeName + "'";
            try {
                rs = stmt.executeQuery(cmd);
                writeToFile(rs, "trims_by_make.csv");
                JOptionPane.showMessageDialog(this, "Exported to trims_by_make.csv");
            }
            catch(Exception e1) {
                e1.printStackTrace();
            }
        }

        // Trims above a certain price
        if (target == trimsAbovePriceButton)
        {
            String price = trimsAbovePriceTF.getText();
            cmd = "SELECT mk.name AS make, m.name AS model, t.name AS trim, t.msrp " +
                  "FROM `trim` t " +
                  "JOIN model m ON m.model_id = t.model_id " +
                  "JOIN make mk ON mk.make_id = m.make_id " +
                  "WHERE t.msrp > " + price + " ORDER BY t.msrp DESC";
            try {
                rs = stmt.executeQuery(cmd);
                writeToFile(rs, "trims_above_price.csv");
                JOptionPane.showMessageDialog(this, "Exported to trims_above_price.csv");
            }
            catch(Exception e1) {
                e1.printStackTrace();
            }
        }
    }

    // Helper to refresh the table with a custom query
    private void refreshTable(String query)
    {
        TableModel.refreshFromDB(stmt, query);
    }

    // Write ResultSet to CSV file
    private void writeToFile(ResultSet rs, String filename)
    {
        try {
            System.out.println("Writing to " + filename);
            FileWriter outputFile = new FileWriter(filename);
            PrintWriter printWriter = new PrintWriter(outputFile);
            ResultSetMetaData rsmd = rs.getMetaData();
            int numColumns = rsmd.getColumnCount();

            // Write headers
            for(int i = 0; i < numColumns; i++) {
                printWriter.print(rsmd.getColumnLabel(i + 1) + ",");
            }
            printWriter.print("\n");

            // Write data rows
            while(rs.next()) {
                for(int i = 0; i < numColumns; i++) {
                    printWriter.print(rs.getString(i + 1) + ",");
                }
                printWriter.print("\n");
                printWriter.flush();
            }
            printWriter.close();
            System.out.println("Export complete: " + filename);
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }
}

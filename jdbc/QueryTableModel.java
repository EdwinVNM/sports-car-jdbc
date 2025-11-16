package jdbcAssignment;
import javax.swing.*;
import javax.swing.table.*;
import java.awt.event.*;
import java.awt.*;
import java.sql.*;
import java.io.*;
import java.util.*;

@SuppressWarnings("serial")
class QueryTableModel extends AbstractTableModel
{
    Vector modelData; // will hold String[] objects
    int colCount;
    String[] headers = new String[0];
    Connection con;
    Statement stmt = null;
    String[] record;
    ResultSet rs = null;

    public QueryTableModel() {
        modelData = new Vector();
    }

    public String getColumnName(int i) {
        return headers[i];
    }

    public int getColumnCount() {
        return colCount;
    }

    public int getRowCount() {
        return modelData.size();
    }

    public Object getValueAt(int row, int col) {
        return ((String[])modelData.elementAt(row))[col];
    }

    // Original method for backward compatibility (queries trim table by default)
    public void refreshFromDB(Statement stmt1)
    {
        refreshFromDB(stmt1, "SELECT * FROM `trim`");
    }

    // Overloaded method that accepts a custom query
    public void refreshFromDB(Statement stmt1, String query)
    {
        // modelData is the data stored by the table
        // when this method is called, the DB is queried using the provided query
        // and the data from the result set is copied into the modelData.
        // Every time refreshFromDB is called, a new modelData is created

        modelData = new Vector();
        stmt = stmt1;
        try {
            // Execute the query and store the result set and its metadata
            rs = stmt.executeQuery(query);
            ResultSetMetaData meta = rs.getMetaData();

            // Get the number of columns
            colCount = meta.getColumnCount();
            
            // Rebuild the headers array with the new column names
            headers = new String[colCount];

            for(int h = 0; h < colCount; h++)
            {
                headers[h] = meta.getColumnName(h + 1);
            }

            // Fill the cache with the records from the query (get all rows)
            while(rs.next())
            {
                record = new String[colCount];
                for(int i = 0; i < colCount; i++)
                {
                    record[i] = rs.getString(i + 1);
                }
                modelData.addElement(record);
            }
            
            // Tell the listeners a new table has arrived
            fireTableChanged(null);
            
            System.out.println("Table refreshed with " + modelData.size() + " rows");
        }
        catch(Exception e) {
            System.out.println("Error with refreshFromDB Method\n" + e.toString());
            e.printStackTrace();
        }
    }
}

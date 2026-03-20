import java.sql.*;

class draftline {
    /***
     * Main method that runs our application
     * 
     * @param args
     * @throws SQLException
     */
    public static void main(String[] args) throws SQLException {
        String tableName = "";
        int sqlCode = 0; // Variable to hold SQLCODE
        String sqlState = "00000"; // Variable to hold SQLSTATE

        // Initialize the connection to the database
        Connection connection = openConnection();

        // Create the statement object for sending SQL statements to the database
        Statement statement = connection.createStatement();

        // Finally but importantly close the statement and connection
        statement.close();
        connection.close();
    }

    /**
     * Method used to establish the connection to our DB2 instance
     * 
     * @return A java.sql.Connection instance
     * @throws SQLException in the event that you cannot conenct to the DB2
     *                      drivers or your username and password environment
     *                      variables are not set
     */
    // Package-visible so local test classes can call the same connection logic.
    static Connection openConnection() throws SQLException {
        // Register the DB2 JDBC driver so DriverManager can create DB2 connections
        try {
            DriverManager.registerDriver(new com.ibm.db2.jcc.DB2Driver());
        } catch (Exception e) {
            throw new SQLException("Failed to register DB2 driver", e);
        }

        // Url we connect to to gain access to the application
        String url = "jdbc:db2://winter2026-comp421.cs.mcgill.ca:50000/comp421";

        // User credentials for our shared database account, cannot be null
        String user = System.getenv("SOCSUSER");
        String pass = System.getenv("SOCSPASSWD");
        if (user == null || pass == null) {
            throw new SQLException("Missing SOCSUSER or SOCSPASSWD environment variable");
        }

        // Return the connection we we use
        return DriverManager.getConnection(url, user, pass);
    }

}

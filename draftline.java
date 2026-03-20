import java.sql.* ; 

class draftline
{
    public static void main ( String [ ] args ) throws SQLException{

    }

    /**
     * Method used to establish the connection to our DB2 instance
     * 
     * @return A java.sql.Connection instance
     * @throws SQLException in the event that you cannot conenct to the DB2 
     * drivers or your username and password environment variables are not set
     */
    private static Connection openConnection() throws SQLException {
        try {
            DriverManager.registerDriver(new com.ibm.db2.jcc.DB2Driver());
        } catch (Exception e) {
            throw new SQLException("Failed to register DB2 driver", e);
        }

        // Url we connect to to gain access to the application
        String url = "jdbc:db2://winter2024-comp421.cs.mcgill.ca:50000/comp421";
        
        // User credentials for our shared database account
        String user = System.getenv("SOCSUSER");
        String pass = System.getenv("SOCSPASSWD");

        if (user == null || pass == null) {
            throw new SQLException("Missing SOCSUSER or SOCSPASSWD environment variable");
        }

        return DriverManager.getConnection(url, user, pass);
    }

}
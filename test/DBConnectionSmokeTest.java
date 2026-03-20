import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Smoke test for database connectivity.
 *
 * <p>This test reuses {@code draftline.openConnection()} to verify that the JDBC
 * driver, environment variables, and DB2 connection settings are configured
 * correctly. It executes a read-only system query
 * ({@code SELECT CURRENT DATE FROM SYSIBM.SYSDUMMY1}) and prints diagnostics on
 * failure. No data is inserted, updated, or deleted.
 */
class DBConnectionSmokeTest {
    public static void main(String[] args) {
        // Reuse the application's connection helper to validate real connection setup.
        try (Connection connection = draftline.openConnection();
                // Use a lightweight DB2 system query as a connectivity smoke test.
                Statement statement = connection.createStatement();
                ResultSet rs = statement.executeQuery("SELECT CURRENT DATE FROM SYSIBM.SYSDUMMY1")) {

            // A returned row confirms the query executed successfully.
            if (rs.next()) {
                System.out.println("Connection test passed.");
                System.out.println("DB current date: " + rs.getString(1));
            } else {
                System.out.println("Connected, but test query returned no rows.");
            }
        } catch (SQLException e) {
            // Print SQL diagnostics to make connection/config issues easier to debug.
            System.err.println("Connection test failed.");
            System.err.println("SQLCODE: " + e.getErrorCode() + ", SQLSTATE: " + e.getSQLState());
            System.err.println(e.getMessage());
        }
    }
}

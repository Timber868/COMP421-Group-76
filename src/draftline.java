import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Scanner;

import tasks.CreateGameAndGoalsTask;
import tasks.PlayerLookupTask;
import tasks.TeamRosterByExpiryTask;
import tasks.AssignPlayerContract;
import tasks.RegisterPersonTask;

/**
 * Entry point for the COMP 421 JDBC application.
 * Presents a menu in a loop until the user chooses Quit; opens one DB
 * connection for the whole session and closes it when the program exits.
 */
class draftline {

    /**
     * Runs the console menu. Opens the database connection once, then loops
     * until the user selects Quit. {@link Connection} and {@link Scanner} are
     * managed with try-with-resources so they always close, including on errors
     * or early exit (assignment: graceful shutdown).
     *
     * @param args unused for now; reserved for future flags if we want to debug
     */
    public static void main(String[] args) {
        // Auto-close connection and scanner when this block ends (quit, error, or
        // return).
        try (Connection connection = openConnection();
                Scanner scanner = new Scanner(System.in)) {

            // Stays true until the user selects Quit (option 6).
            boolean running = true;

            // display menu, parse input and run one action.
            while (running) {
                printMainMenu();

                System.out.print("Please enter your option: ");
                // Full line avoids issues with stray spaces and empty lines.
                String line = scanner.nextLine().trim();

                int choice;
                try {
                    // Non-numeric input would throw; we recover and re-show the menu.
                    choice = Integer.parseInt(line);
                } catch (NumberFormatException e) {
                    System.out.println("Invalid input. Enter a number listed on the menu.\n");
                    continue;
                }

                // Each branch calls a dedicated task class.
                switch (choice) {
                    case 1:
                        PlayerLookupTask.run(connection, scanner);
                        break;
                    case 2:
                        CreateGameAndGoalsTask.run(connection, scanner);
                        break;
                    case 3:
                        TeamRosterByExpiryTask.run(connection, scanner);
                        break;
                    case 4:
                        RegisterPersonTask.run(connection, scanner);
                        break;
                    case 5:
                        AssignPlayerContract.run(connection, scanner);
                        break;
                    case 6:
                        // Quit ends the loop; resources close in the outer try-with-resources.
                        System.out.println("Goodbye.");
                        running = false;
                        break;
                    default:
                        // Any integer outside 1–6 is rejected without leaving the loop.
                        System.out.println("Invalid option. Choose an integer between 1 to 6.\n");
                        break;
                }
            }
        } catch (SQLException e) {
            // Covers failed connect and any SQLException that escapes future task code.
            System.err.println("Database error: " + e.getMessage());
            System.err.println("SQLSTATE: " + e.getSQLState() + ", SQLCODE: " + e.getErrorCode());
        }
    }

    /**
     * Prints the main menu. Five application tasks plus Quit.
     * Options 2–5 wire to {@code tasks.*} handlers when implemented.
     */
    private static void printMainMenu() {
        System.out.println("========== Main Menu ==========");
        System.out.println("1. Look up player by last name");
        System.out.println("2. Create a game and record goals");
        System.out.println("3. View an active roster of a team by expiring contracts first");
        System.out.println("4. Register new person (player / coach / referee)");
        System.out.println("5. Sign a player (new contract)");
        System.out.println("6. Quit");
        System.out.println("================================");
    }

    /**
     * Establishes a connection to the course DB2 instance.
     *
     * @return an open JDBC {@link Connection}
     * @throws SQLException if the driver cannot be registered, credentials are
     *                      missing, or the database rejects the connection
     */
    // Package-visible so local test classes can call the same connection logic.
    static Connection openConnection() throws SQLException {
        try {
            // Ensures DB2 is available to DriverManager.getConnection for this JVM.
            DriverManager.registerDriver(new com.ibm.db2.jcc.DB2Driver());
        } catch (Exception e) {
            // Normalize registration failures as SQLException for one catch style in main.
            throw new SQLException("Failed to register DB2 driver", e);
        }

        // Course server and database name (Winter 2026).
        String url = "jdbc:db2://winter2026-comp421.cs.mcgill.ca:50000/comp421";

        // Retrieve SOCSUSER and SOCSPASSWD from the env.
        String user = System.getenv("SOCSUSER");
        String pass = System.getenv("SOCSPASSWD");
        if (user == null || pass == null) {
            throw new SQLException("Missing SOCSUSER or SOCSPASSWD environment variable");
        }

        // Opens the session; throws if network, auth, or DB name is wrong.
        return DriverManager.getConnection(url, user, pass);
    }
}

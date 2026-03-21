package tasks;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Console task: resolve a player by last name (with disambiguation), then print identity,
 * playing attributes, career aggregates, and contract snapshot.
 *
 * <p>Last-name matching treats {@code NAME} as {@code "... LAST"} (space before family name)
 * or a single token equal to the query. Adjust if {@code PERSON.NAME} format differs
 * (e.g. {@code "Last, First"}).
 */
public class PlayerLookupTask {

    // Qualifies table names in SQL (must match Project 2 DDL, e.g. CS421G76).
    private static final String SCHEMA = "CS421G76";

    // One row from the "who matches this last name?" query, shown in a numbered list.
    private static final class PlayerMatch {
        // DB stores PERSON.PID as UUID (string), not INTEGER.
        final String pid;
        // Name, pid, nationality, birth date — for menu display.
        final String menuLine;

        PlayerMatch(String pid, String menuLine) {
            this.pid = pid;
            this.menuLine = menuLine;
        }
    }

    // Entry point from the main menu: prompt, search, optional pick, then profile.
    public static void run(Connection conn, Scanner scanner) {
        // Retrieve the last name given
        System.out.print("Enter player last name: ");
        String lastName = scanner.nextLine().trim();

        if (lastName.isEmpty()) {
            System.out.println("Please enter a non-empty last name.\n");
            return;
        }

        try {
            // Find all the players whose last name matches the input.
            List<PlayerMatch> matches = findPlayersByLastName(conn, lastName);
            if (matches.isEmpty()) {
                System.out.println("Sorry — no player found with that last name.\n");
                return;
            }

            // If there are players of picked last name find which one specificly is picked.
            String chosenPid = resolveDisambiguation(scanner, matches);
            if (chosenPid == null) {
                return;
            }

            // Print player found information.
            printProfile(conn, chosenPid);
            System.out.println();
        } catch (SQLException e) {
            printSQLException("looking up player", e);
        }
    }

    /**
     * Returns every player whose {@code PERSON.NAME} matches the last name (suffix after a space,
     * or whole name equals the token), ordered by name.
     */
    private static List<PlayerMatch> findPlayersByLastName(Connection conn, String lastName)
            throws SQLException {
        String sql = "SELECT per.PID, per.NAME, per.NATIONALITY, per.BIRTH_DATE "
                + "FROM " + SCHEMA + ".PERSON per "
                + "INNER JOIN " + SCHEMA + ".PLAYER pl ON per.PID = pl.PID "
                + "WHERE UPPER(RTRIM(per.NAME)) LIKE ? "
                + "OR UPPER(RTRIM(per.NAME)) = UPPER(?) "
                + "ORDER BY per.NAME";

        String likePattern = "% " + lastName.toUpperCase();
        List<PlayerMatch> out = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, likePattern);
            ps.setString(2, lastName);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(new PlayerMatch(rs.getString("PID"), formatCandidateMenuLine(rs)));
                }
            }
        }
        return out;
    }

    // Builds one numbered-menu line for a row of the candidate ResultSet.
    private static String formatCandidateMenuLine(ResultSet rs) throws SQLException {
        String nationality = rs.getString("NATIONALITY");
        Object birth = rs.getObject("BIRTH_DATE");
        return rs.getString("NAME") + " | pid=" + rs.getString("PID")
                + " | " + (nationality != null ? nationality : "?")
                + " | born " + (birth != null ? birth.toString() : "?");
    }

    /**
     * If there is a single match, echoes it; otherwise prompts for a list index.
     *
     * @return chosen player id (UUID string), or {@code null} if the user input was
     *         invalid
     */
    private static String resolveDisambiguation(Scanner scanner, List<PlayerMatch> matches) {
        // If there is only one player we dont have to ask for further input.
        if (matches.size() == 1) {
            PlayerMatch only = matches.get(0);
            System.out.println("Found: " + only.menuLine);
            return only.pid;
        }

        // If not display all the players that match this last name.
        System.out.println("\nSeveral players match. Pick one:");
        for (int i = 0; i < matches.size(); i++) {
            System.out.println((i + 1) + ". " + matches.get(i).menuLine);
        }

        // And prompt user to select the player.
        System.out.print("\nEnter number (1-" + matches.size() + "): ");
        String line = scanner.nextLine().trim();
        int idx;

        // Validate the player id and choice.
        try {
            idx = Integer.parseInt(line);
        } catch (NumberFormatException e) {
            System.out.println("Invalid choice.\n");
            return null;
        }
        if (idx < 1 || idx > matches.size()) {
            System.out.println("That number is not on the list.\n");
            return null;
        }

        // Returns the found players id.
        return matches.get(idx - 1).pid;
    }

    // Prints core person/player fields, goal and game counts, then contract section.
    private static void printProfile(Connection conn, String pid) throws SQLException {
        printCoreProfile(conn, pid);
        printContractsSection(conn, pid);
    }

    // Name, demographics, position, debut, career goals and distinct games played.
    private static void printCoreProfile(Connection conn, String pid) throws SQLException {
        // Prepare the sql query.
        String sql = "SELECT per.NAME, per.NATIONALITY, per.BIRTH_DATE, "
                + "pl.POSITION, pl.DOMINANT_HAND, pl.DEBUT_DATE, "
                + "(SELECT COUNT(*) FROM " + SCHEMA + ".GOAL g WHERE g.PID = pl.PID) AS CAREER_GOALS, "
                + "(SELECT COUNT(DISTINCT pi.GID) FROM " + SCHEMA + ".PLAYEDIN pi WHERE pi.PID = pl.PID) AS GAMES_PLAYED "
                + "FROM " + SCHEMA + ".PERSON per "
                + "INNER JOIN " + SCHEMA + ".PLAYER pl ON per.PID = pl.PID "
                + "WHERE per.PID = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, pid);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    System.out.println("Player record missing.");
                    return;
                }
                System.out.println("\n--- Player profile ---");
                System.out.println("Name:          " + rs.getString("NAME"));
                System.out.println("Nationality:   " + rs.getString("NATIONALITY"));
                System.out.println("Birth date:    " + formatSqlValue(rs.getObject("BIRTH_DATE")));
                System.out.println("Position:      " + rs.getString("POSITION"));
                System.out.println("Dominant hand: " + rs.getString("DOMINANT_HAND"));
                System.out.println("Debut date:    " + formatSqlValue(rs.getObject("DEBUT_DATE")));
                System.out.println("Career goals:  " + rs.getInt("CAREER_GOALS"));
                System.out.println("Games played:  " + rs.getInt("GAMES_PLAYED"));
            }
        }
    }

    /**
     * Active contracts (today between {@code VALID_FROM} and {@code VALID_UNTIL}); if none,
     * up to three rows by latest {@code VALID_UNTIL} with an explanatory note.
     */
    private static void printContractsSection(Connection conn, String pid) throws SQLException {
        String activeSql = "SELECT TEAM_NAME, LEAGUE_NAME, JERSEY_NUMBER, VALID_FROM, VALID_UNTIL "
                + "FROM " + SCHEMA + ".CONTRACT "
                + "WHERE PID = ? AND CURRENT DATE BETWEEN VALID_FROM AND VALID_UNTIL "
                + "ORDER BY VALID_FROM DESC";

        List<String> activeLines = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(activeSql)) {
            ps.setString(1, pid);
            try (ResultSet rs = ps.executeQuery()) {
                activeLines.addAll(contractLinesFromResultSet(rs));
            }
        }

        System.out.println("Current contract(s) (as of today):");
        if (activeLines.isEmpty()) {
            printFallbackRecentContracts(conn, pid);
        } else {
            for (String line : activeLines) {
                System.out.println("  " + line);
            }
        }
    }

    // When no row covers CURRENT DATE, show recent history or "none on file".
    private static void printFallbackRecentContracts(Connection conn, String pid) throws SQLException {
        String recentSql = "SELECT TEAM_NAME, LEAGUE_NAME, JERSEY_NUMBER, VALID_FROM, VALID_UNTIL "
                + "FROM " + SCHEMA + ".CONTRACT WHERE PID = ? "
                + "ORDER BY VALID_UNTIL DESC FETCH FIRST 3 ROWS ONLY";

        try (PreparedStatement ps = conn.prepareStatement(recentSql)) {
            ps.setString(1, pid);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    System.out.println("  (none on file)");
                    return;
                }
                System.out.println("  (no active window for today — showing up to 3 most recent)");
                System.out.println("  " + formatContractRow(rs));
                while (rs.next()) {
                    System.out.println("  " + formatContractRow(rs));
                }
            }
        }
    }

    // Collects human-readable contract lines from an open ResultSet (exhausts it).
    private static List<String> contractLinesFromResultSet(ResultSet rs) throws SQLException {
        List<String> lines = new ArrayList<>();
        while (rs.next()) {
            lines.add(formatContractRow(rs));
        }
        return lines;
    }

    // One contract row: team (league), jersey, date range — used for active and recent lists.
    private static String formatContractRow(ResultSet rs) throws SQLException {
        return rs.getString("TEAM_NAME") + " (" + rs.getString("LEAGUE_NAME") + ")"
                + " | jersey " + rs.getObject("JERSEY_NUMBER")
                + " | " + rs.getObject("VALID_FROM") + " .. " + rs.getObject("VALID_UNTIL");
    }

    private static String formatSqlValue(Object value) {
        return value != null ? value.toString() : "n/a";
    }

    private static void printSQLException(String context, SQLException e) {
        System.err.println("Database error while " + context + ": " + e.getMessage());
        System.err.println("SQLSTATE: " + e.getSQLState() + ", SQLCODE: " + e.getErrorCode());
    }
}

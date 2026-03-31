package tasks;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;
import java.sql.CallableStatement;
import java.sql.Types;

// Task 2: walks the user through creating a game and recording goals for it.
// Inserts into GAME, GAMEHOMETEAM, GAMEAWAYTEAM, GAMEREFEREES, PLAYEDIN, and GOAL.
public class CreateGameAndGoalsTask {

    private static final String SCHEMA = "CS421G76";

    // Called from the main menu to kick off the task.
    public static void run(Connection conn, Scanner scanner) {
        try {
            // Pick which league the game is in
            String league = pickLeague(conn, scanner);
            if (league == null) return;

            // Pick home and away teams from that league
            List<String> teams = fetchTeams(conn, league);
            if (teams.isEmpty()) {
                System.out.println("No teams found in league \"" + league + "\".\n");
                return;
            }

            System.out.println("\nPick the HOME team:");
            String homeTeam = pickFromList(scanner, teams, "team");
            if (homeTeam == null) return;

            System.out.println("\nPick the AWAY team:");
            String awayTeam = pickFromList(scanner, teams, "team");
            if (awayTeam == null) return;

            if (homeTeam.equals(awayTeam)) {
                System.out.println("Home and away teams must be different.\n");
                return;
            }

            // Get date, stage, and venue
            Date gameDate = promptDate(scanner);
            if (gameDate == null) return;

            System.out.print("Enter stage (e.g. Regular Season, Playoffs): ");
            String stage = scanner.nextLine().trim();
            if (stage.isEmpty()) {
                System.out.println("Stage cannot be empty.\n");
                return;
            }

            System.out.print("Enter venue (optional, press Enter to skip): ");
            String venueRaw = scanner.nextLine().trim();
            String venue = venueRaw.isEmpty() ? null : venueRaw;

            // Need at least one referee
            List<String[]> referees = fetchReferees(conn); // each entry is [pid, name]
            if (referees.isEmpty()) {
                System.out.println("No referees found in the database.\n");
                return;
            }

            List<String> selectedRefPids = pickReferees(scanner, referees);
            if (selectedRefPids == null || selectedRefPids.isEmpty()) {
                System.out.println("At least one referee is required.\n");
                return;
            }

            // Everything goes in one transaction so we don't end up with a half-created game
            String gid = UUID.randomUUID().toString();

            conn.setAutoCommit(false);
            try {
                insertGame(conn, gid, stage, venue, gameDate);
                insertGameTeam(conn, gid, homeTeam, league, "GAMEHOMETEAM");
                insertGameTeam(conn, gid, awayTeam, league, "GAMEAWAYTEAM");
                insertGameReferees(conn, gid, selectedRefPids);
                // Enroll every player with an active contract for either team on the game date
                int enrolled = insertPlayedIn(conn, gid);
                conn.commit();
                System.out.println("\nGame created (GID: " + gid + ").");
                System.out.println("Enrolled " + enrolled + " player(s) into PlayedIn.");
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }

            // Now let the user record goals one by one
            recordGoals(conn, scanner, gid, homeTeam, awayTeam, league, gameDate);

        } catch (SQLException e) {
            printSQLException("creating game", e);
        }
    }

    // Pull all league names from the DB and let the user pick one.
    private static String pickLeague(Connection conn, Scanner scanner) throws SQLException {
        String sql = "SELECT LEAGUE_NAME FROM " + SCHEMA + ".LEAGUE ORDER BY LEAGUE_NAME";
        List<String> leagues = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                leagues.add(rs.getString("LEAGUE_NAME"));
            }
        }
        if (leagues.isEmpty()) {
            System.out.println("No leagues found in the database.\n");
            return null;
        }
        System.out.println("\nPick a league:");
        return pickFromList(scanner, leagues, "league");
    }

    // Get all teams in the given league.
    private static List<String> fetchTeams(Connection conn, String league) throws SQLException {
        String sql = "SELECT TEAM_NAME FROM " + SCHEMA + ".TEAM WHERE LEAGUE_NAME = ? ORDER BY TEAM_NAME";
        List<String> out = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, league);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(rs.getString("TEAM_NAME"));
                }
            }
        }
        return out;
    }

    // Ask the user for a date and parse it. Returns null on bad format.
    private static Date promptDate(Scanner scanner) {
        System.out.print("Enter game date (YYYY-MM-DD): ");
        String raw = scanner.nextLine().trim();
        try {
            return Date.valueOf(raw);
        } catch (IllegalArgumentException e) {
            System.out.println("Invalid date format. Expected YYYY-MM-DD.\n");
            return null;
        }
    }

    // Fetch all referees from the DB (joined with PERSON to get their names).
    private static List<String[]> fetchReferees(Connection conn) throws SQLException {
        String sql = "SELECT per.PID, per.NAME "
                + "FROM " + SCHEMA + ".PERSON per "
                + "INNER JOIN " + SCHEMA + ".REFEREE ref ON per.PID = ref.PID "
                + "ORDER BY per.NAME";
        List<String[]> out = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                out.add(new String[]{rs.getString("PID"), rs.getString("NAME")});
            }
        }
        return out;
    }

    private static List<String> pickReferees(Scanner scanner, List<String[]> referees) {
        System.out.println("\nAvailable referees (enter number to add, empty line when done):");
        for (int i = 0; i < referees.size(); i++) {
            System.out.println((i + 1) + ". " + referees.get(i)[1]);
        }

        List<String> selected = new ArrayList<>();
        while (true) {
            System.out.print("Add referee (1-" + referees.size() + ", or press Enter to finish): ");
            String line = scanner.nextLine().trim();
            if (line.isEmpty()) break;
            int idx;
            try {
                idx = Integer.parseInt(line);
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Enter a number.");
                continue;
            }
            if (idx < 1 || idx > referees.size()) {
                System.out.println("Number out of range.");
                continue;
            }
            String pid = referees.get(idx - 1)[0];
            String name = referees.get(idx - 1)[1];
            if (selected.contains(pid)) {
                System.out.println(name + " is already added.");
            } else {
                selected.add(pid);
                System.out.println("Added: " + name);
            }
        }
        return selected;
    }

    // Insert the main GAME row.
    private static void insertGame(Connection conn, String gid, String stage,
            String venue, Date gameDate) throws SQLException {
        String sql = "INSERT INTO " + SCHEMA + ".GAME (GID, STAGE, VENUE, DATE) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, gid);
            ps.setString(2, stage);
            if (venue != null) {
                ps.setString(3, venue);
            } else {
                ps.setNull(3, java.sql.Types.VARCHAR);
            }
            ps.setDate(4, gameDate);
            ps.executeUpdate();
        }
    }

    // Insert a row into GAMEHOMETEAM or GAMEAWAYTEAM. Goals and points start at 0.
    private static void insertGameTeam(Connection conn, String gid, String teamName,
            String league, String table) throws SQLException {
        String sql = "INSERT INTO " + SCHEMA + "." + table
                + " (GID, TEAM_NAME, LEAGUE_NAME, GOALS_SCORED, POINTS_EARNED) VALUES (?, ?, ?, 0, 0)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, gid);
            ps.setString(2, teamName);
            ps.setString(3, league);
            ps.executeUpdate();
        }
    }

    private static void insertGameReferees(Connection conn, String gid, List<String> refPids)
            throws SQLException {
        String sql = "INSERT INTO " + SCHEMA + ".GAMEREFEREES (GID, PID) VALUES (?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (String pid : refPids) {
                ps.setString(1, gid);
                ps.setString(2, pid);
                ps.executeUpdate();
            }
        }
    }

    // Calls the stored procedure that registers every eligible player for the game in PLAYEDIN.
    // Returns the number of newly inserted PLAYEDIN rows.
    private static int insertPlayedIn(Connection conn, String gid) throws SQLException {
        String callSql = "CALL " + SCHEMA + ".REGISTER_ELIGIBLE_PLAYERS_FOR_GAME(?, ?, ?)";

        try (CallableStatement cs = conn.prepareCall(callSql)) {
            cs.setString(1, gid);
            cs.registerOutParameter(2, Types.INTEGER);
            cs.registerOutParameter(3, Types.INTEGER);

            cs.execute();
            return cs.getInt(2);
        }
    }


    // Loop until the user is done entering goals. Each goal is its own transaction.
    private static void recordGoals(Connection conn, Scanner scanner, String gid,
            String homeTeam, String awayTeam, String league, Date gameDate) throws SQLException {
        System.out.println("\nRecord goals (press Enter with no name to finish).");

        while (true) {
            System.out.print("\nEnter scorer's last name (or press Enter to finish): ");
            String lastName = scanner.nextLine().trim();
            if (lastName.isEmpty()) break;

            String scorerPid = resolveScorer(conn, scanner, lastName);
            if (scorerPid == null) continue;

            int minute = promptInt(scanner, "Enter minute of goal: ");
            if (minute < 0) continue;

            int goalNo = promptInt(scanner, "Enter goal number: ");
            if (goalNo < 0) continue;

            System.out.print("Enter link (optional, press Enter to skip): ");
            String linkRaw = scanner.nextLine().trim();
            String link = linkRaw.isEmpty() ? null : linkRaw;

            conn.setAutoCommit(false);
            try {
                insertGoal(conn, scorerPid, gid, goalNo, minute, link);
                bumpTeamGoals(conn, scorerPid, gid, homeTeam, awayTeam, league, gameDate);
                conn.commit();
                System.out.println("Goal recorded.");
            } catch (SQLException e) {
                conn.rollback();
                printSQLException("recording goal", e);
            } finally {
                conn.setAutoCommit(true);
            }
        }

        System.out.println("Done recording goals.\n");
    }

    // Look up a player by last name. If multiple match, ask the user to pick one.
    private static String resolveScorer(Connection conn, Scanner scanner, String lastName)
            throws SQLException {
        String sql = "SELECT per.PID, per.NAME, per.NATIONALITY, per.BIRTH_DATE "
                + "FROM " + SCHEMA + ".PERSON per "
                + "INNER JOIN " + SCHEMA + ".PLAYER pl ON per.PID = pl.PID "
                + "WHERE UPPER(RTRIM(per.NAME)) LIKE ? "
                + "OR UPPER(RTRIM(per.NAME)) = UPPER(?) "
                + "ORDER BY per.NAME";

        String likePattern = "% " + lastName.toUpperCase();
        List<String[]> matches = new ArrayList<>();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, likePattern);
            ps.setString(2, lastName);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String pid = rs.getString("PID");
                    String name = rs.getString("NAME");
                    Object birth = rs.getObject("BIRTH_DATE");
                    String nat = rs.getString("NATIONALITY");
                    String line = name + " | " + (nat != null ? nat : "?")
                            + " | born " + (birth != null ? birth.toString() : "?");
                    matches.add(new String[]{pid, line});
                }
            }
        }

        if (matches.isEmpty()) {
            System.out.println("No player found with that last name.");
            return null;
        }
        if (matches.size() == 1) {
            System.out.println("Found: " + matches.get(0)[1]);
            return matches.get(0)[0];
        }

        System.out.println("Several players match. Pick one:");
        for (int i = 0; i < matches.size(); i++) {
            System.out.println((i + 1) + ". " + matches.get(i)[1]);
        }
        System.out.print("Enter number (1-" + matches.size() + "): ");
        String line = scanner.nextLine().trim();
        int idx;
        try {
            idx = Integer.parseInt(line);
        } catch (NumberFormatException e) {
            System.out.println("Invalid choice.");
            return null;
        }
        if (idx < 1 || idx > matches.size()) {
            System.out.println("Number out of range.");
            return null;
        }
        return matches.get(idx - 1)[0];
    }

    // Insert the goal record. MINUTE is stored as DOUBLE in the DB.
    private static void insertGoal(Connection conn, String pid, String gid,
            int goalNo, int minute, String link) throws SQLException {
        String sql = "INSERT INTO " + SCHEMA + ".GOAL (PID, GID, GOAL_NO, MINUTE, LINK) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, pid);
            ps.setString(2, gid);
            ps.setInt(3, goalNo);
            ps.setDouble(4, minute);
            if (link != null) {
                ps.setString(5, link);
            } else {
                ps.setNull(5, java.sql.Types.VARCHAR);
            }
            ps.executeUpdate();
        }
    }

    // Figure out which team the scorer is on and bump that team's goal count in GAMEHOMETEAM/GAMEAWAYTEAM.
    private static void bumpTeamGoals(Connection conn, String scorerPid, String gid,
            String homeTeam, String awayTeam, String league, Date gameDate) throws SQLException {
        String sql = "SELECT TEAM_NAME FROM " + SCHEMA + ".CONTRACT "
                + "WHERE PID = ? AND (TEAM_NAME = ? OR TEAM_NAME = ?) "
                + "AND LEAGUE_NAME = ? AND ? BETWEEN VALID_FROM AND VALID_UNTIL "
                + "FETCH FIRST 1 ROWS ONLY";

        String scorerTeam = null;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, scorerPid);
            ps.setString(2, homeTeam);
            ps.setString(3, awayTeam);
            ps.setString(4, league);
            ps.setDate(5, gameDate);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    scorerTeam = rs.getString("TEAM_NAME");
                }
            }
        }

        if (scorerTeam == null) {
            System.out.println("Warning: scorer not found in either team's contract — GOALS_SCORED not updated.");
            return;
        }

        String targetTable = scorerTeam.equals(homeTeam) ? "GAMEHOMETEAM" : "GAMEAWAYTEAM";
        String updateSql = "UPDATE " + SCHEMA + "." + targetTable
                + " SET GOALS_SCORED = GOALS_SCORED + 1 WHERE GID = ? AND TEAM_NAME = ?";
        try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
            ps.setString(1, gid);
            ps.setString(2, scorerTeam);
            ps.executeUpdate();
        }
    }

    // Print a numbered list and return whichever item the user picks.
    private static String pickFromList(Scanner scanner, List<String> items, String label) {
        for (int i = 0; i < items.size(); i++) {
            System.out.println((i + 1) + ". " + items.get(i));
        }
        System.out.print("Enter number (1-" + items.size() + "): ");
        String line = scanner.nextLine().trim();
        int idx;
        try {
            idx = Integer.parseInt(line);
        } catch (NumberFormatException e) {
            System.out.println("Invalid input.\n");
            return null;
        }
        if (idx < 1 || idx > items.size()) {
            System.out.println("That number is not on the list.\n");
            return null;
        }
        return items.get(idx - 1);
    }

    // Read an integer from the user. Returns -1 if they type something invalid.
    private static int promptInt(Scanner scanner, String prompt) {
        System.out.print(prompt);
        String line = scanner.nextLine().trim();
        try {
            return Integer.parseInt(line);
        } catch (NumberFormatException e) {
            System.out.println("Invalid number.\n");
            return -1;
        }
    }

    private static void printSQLException(String context, SQLException e) {
        System.err.println("Database error while " + context + ": " + e.getMessage());
        System.err.println("SQLSTATE: " + e.getSQLState() + ", SQLCODE: " + e.getErrorCode());
    }
}

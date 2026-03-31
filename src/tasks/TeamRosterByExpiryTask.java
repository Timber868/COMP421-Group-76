package tasks;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Choose a league, then a team within it, then print the players with active contracts today
 * ordered by the earliest expiry date.
 */
public class TeamRosterByExpiryTask {

    private static final String SCHEMA = "CS421G76";

    private static final class LeagueChoice {
        final String leagueName;
        final String menuLine;

        LeagueChoice(String leagueName, String menuLine) {
            this.leagueName = leagueName;
            this.menuLine = menuLine;
        }
    }

    private static final class TeamChoice {
        final String teamName;
        final String leagueName;
        final String menuLine;

        TeamChoice(String teamName, String leagueName, String menuLine) {
            this.teamName = teamName;
            this.leagueName = leagueName;
            this.menuLine = menuLine;
        }
    }

    public static void run(Connection conn, Scanner scanner) {
        try {
            // Step 1: pick a league
            List<LeagueChoice> leagues = fetchLeagues(conn);
            if (leagues.isEmpty()) {
                System.out.println("No leagues with active-contract teams found in the database\n");
                return;
            }

            LeagueChoice chosenLeague = chooseLeague(scanner, leagues);
            if (chosenLeague == null) {
                return;
            }

            // Step 2: pick a team within that league
            List<TeamChoice> teams = fetchTeams(conn, chosenLeague.leagueName);
            if (teams.isEmpty()) {
                System.out.println("No teams with active contracts found in that league\n");
                return;
            }

            TeamChoice chosenTeam = chooseTeam(scanner, teams);
            if (chosenTeam == null) {
                return;
            }

            printRoster(conn, chosenTeam);
            System.out.println();
        } catch (SQLException e) {
            printSQLException("loading active roster", e);
        }
    }

    private static List<LeagueChoice> fetchLeagues(Connection conn) throws SQLException {
        String sql = "SELECT DISTINCT t.LEAGUE_NAME "
                + "FROM " + SCHEMA + ".TEAM t "
                + "INNER JOIN " + SCHEMA + ".CONTRACT c "
                + "ON c.TEAM_NAME = t.TEAM_NAME AND c.LEAGUE_NAME = t.LEAGUE_NAME "
                + "AND CURRENT DATE BETWEEN c.VALID_FROM AND c.VALID_UNTIL "
                + "ORDER BY t.LEAGUE_NAME";

        List<LeagueChoice> leagues = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String leagueName = rs.getString("LEAGUE_NAME");
                leagues.add(new LeagueChoice(leagueName, leagueName));
            }
        }
        return leagues;
    }

    private static LeagueChoice chooseLeague(Scanner scanner, List<LeagueChoice> leagues) {
        System.out.println("\nChoose a league:");
        for (int i = 0; i < leagues.size(); i++) {
            System.out.println((i + 1) + ". " + leagues.get(i).menuLine);
        }

        System.out.print("\nEnter number (1-" + leagues.size() + "): ");
        String line = scanner.nextLine().trim();

        int idx;
        try {
            idx = Integer.parseInt(line);
        } catch (NumberFormatException e) {
            System.out.println("Invalid choice\n");
            return null;
        }

        if (idx < 1 || idx > leagues.size()) {
            System.out.println("That number is not on the list\n");
            return null;
        }

        return leagues.get(idx - 1);
    }

    private static List<TeamChoice> fetchTeams(Connection conn, String leagueName) throws SQLException {
        String sql = "SELECT DISTINCT t.TEAM_NAME, t.LEAGUE_NAME, t.CITY "
                + "FROM " + SCHEMA + ".TEAM t "
                + "INNER JOIN " + SCHEMA + ".CONTRACT c "
                + "ON c.TEAM_NAME = t.TEAM_NAME AND c.LEAGUE_NAME = t.LEAGUE_NAME "
                + "AND CURRENT DATE BETWEEN c.VALID_FROM AND c.VALID_UNTIL "
                + "WHERE t.LEAGUE_NAME = ? "
                + "ORDER BY t.TEAM_NAME";

        List<TeamChoice> teams = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, leagueName);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String teamName = rs.getString("TEAM_NAME");
                    String city = rs.getString("CITY");
                    String menuLine = teamName + (city != null ? " | " + city : "");
                    teams.add(new TeamChoice(teamName, leagueName, menuLine));
                }
            }
        }
        return teams;
    }

    private static TeamChoice chooseTeam(Scanner scanner, List<TeamChoice> teams) {
        System.out.println("\nChoose a team:");
        for (int i = 0; i < teams.size(); i++) {
            System.out.println((i + 1) + ". " + teams.get(i).menuLine);
        }

        System.out.print("\nEnter number (1-" + teams.size() + "): ");
        String line = scanner.nextLine().trim();

        int idx;
        try {
            idx = Integer.parseInt(line);
        } catch (NumberFormatException e) {
            System.out.println("Invalid choice\n");
            return null;
        }

        if (idx < 1 || idx > teams.size()) {
            System.out.println("That number is not on the list\n");
            return null;
        }

        return teams.get(idx - 1);
    }

    private static void printRoster(Connection conn, TeamChoice team) throws SQLException {
        String sql = "SELECT per.NAME, pl.POSITION, active.CONTRACT_END, "
                + "(SELECT COUNT(*) FROM " + SCHEMA + ".GOAL g WHERE g.PID = pl.PID) AS CAREER_GOALS "
                + "FROM (SELECT c.PID, MIN(c.VALID_UNTIL) AS CONTRACT_END "
                + "FROM " + SCHEMA + ".CONTRACT c "
                + "WHERE c.TEAM_NAME = ? "
                + "AND c.LEAGUE_NAME = ? "
                + "AND CURRENT DATE BETWEEN c.VALID_FROM AND c.VALID_UNTIL "
                + "GROUP BY c.PID) active "
                + "INNER JOIN " + SCHEMA + ".PLAYER pl ON pl.PID = active.PID "
                + "INNER JOIN " + SCHEMA + ".PERSON per ON per.PID = pl.PID "
                + "ORDER BY active.CONTRACT_END ASC, per.NAME ASC";

        System.out.println("\n--- Active roster for " + team.teamName + " (" + team.leagueName + ") ---");
        System.out.println("Showing players with an active contract on CURRENT DATE");

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, team.teamName);
            ps.setString(2, team.leagueName);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    System.out.println("No active contracts found for this team today");
                    return;
                }

                do {
                    String position = rs.getString("POSITION");
                    Object contractEnd = rs.getObject("CONTRACT_END");
                    int careerGoals = rs.getInt("CAREER_GOALS");

                    System.out.println(rs.getString("NAME")
                            + " | " + (position != null ? position : "n/a")
                            + " | contract ends " + (contractEnd != null ? contractEnd.toString() : "n/a")
                            + " | career goals " + careerGoals);
                } while (rs.next());
            }
        }
    }

    private static void printSQLException(String context, SQLException e) {
        System.err.println("Database error while " + context + ": " + e.getMessage());
        System.err.println("SQLSTATE: " + e.getSQLState() + ", SQLCODE: " + e.getErrorCode());
    }
}

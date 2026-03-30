package tasks;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;

public class AssignPlayerContract {
    private static final String SCHEMA = "CS421G76";

    public static void run(Connection conn, Scanner scanner) {
        try{
            System.out.print("Enter the pid of the player you wish to give a contract to: ");
            String pid = scanner.nextLine().trim();
            if (!verifyInput(pid, "pid", 36)){
                return;
            }
            if (!checkPlayerExists(conn, pid)){
                return;
            }

            System.out.print("Enter the team name that the player should get a contract for: ");
            String teamName = scanner.nextLine().trim();
            if (!verifyInput(teamName, "team name", 255)){
                return;
            }
            System.out.print("Enter the league the team plays in: ");
            String leagueName = scanner.nextLine().trim();
            if (!verifyInput(leagueName, "league name", 255)){
                return;
            }
            if (!checkTeamExists(conn, teamName,  leagueName)){
                return;
            }

            System.out.print("Enter the length of the contract (in years): ");
            String contractLength = scanner.nextLine().trim();
            if (!verifyNumber(contractLength, "contract length")){
                return;
            }

            List<Integer> takenNumbers = getTakenNumbers(conn, teamName, leagueName);
            String invalidNumbers = "";
            for (int i = 0; i < takenNumbers.size(); i++) {
                invalidNumbers += String.valueOf(takenNumbers.get(i)) + " ";
            }
            System.out.print("Enter jersey number. Cannot be one of " + invalidNumbers.trim() + ": ");
            String jerseyNumber = scanner.nextLine().trim();
            if (!verifyNumber(jerseyNumber, "jersey number")){
                return;
            }
            if (takenNumbers.contains(Integer.parseInt(jerseyNumber))) {
                return;
            }

            setActiveContractsToYesterday(conn, pid);

            createContract(conn, pid, teamName, leagueName, contractLength, jerseyNumber);

            printNewContractInfo(teamName, leagueName, contractLength, jerseyNumber);
        } catch (SQLException e) {
            printSQLException("Giving a contract to a player", e);
        }
    }

    private static void printNewContractInfo(String teamName, String leagueName, String contractLength, String jerseyNumber) {
        System.out.println("Contract created!");
        System.out.println("--- Contract Info ---");
        System.out.println("Contract with " + teamName + " in the " + leagueName);
        System.out.println("Jersey number for the duration of the contract: " + jerseyNumber);
        String until = LocalDate.now().plusYears(Long.parseLong(contractLength)).toString();
        String today = LocalDate.now().toString();
        System.out.println("Contract valid from today (" + today + ") until " + until);
    }

    private static void createContract(Connection conn, String pid, String teamName, String leagueName, String contractLength, String jerseyNumber) throws SQLException {
        Date validFrom = Date.valueOf(LocalDate.now());
        Date validUntil = Date.valueOf(LocalDate.now().plusYears(Long.parseLong(contractLength)));

        String cid = UUID.randomUUID().toString();

        String sql = "INSERT INTO " + SCHEMA + ".Contract VALUES " 
            + "(?, ?, ?, ?, ?, ?, ?);";
        
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, cid);
            ps.setString(2, pid);
            ps.setString(3, teamName);
            ps.setString(4, leagueName);
            ps.setDate(5, validFrom);
            ps.setDate(6, validUntil);
            ps.setInt(7, Integer.parseInt(jerseyNumber));
            
            ps.executeUpdate();
            ps.close();
        }
        return;
    }

    private static void setActiveContractsToYesterday(Connection conn, String pid) throws SQLException {
        List<String> activeContracts = new ArrayList<String>();

        Date yesterday = Date.valueOf(LocalDate.now().minusDays(1));


        String sql = "SELECT cid FROM " + SCHEMA + ".Contract WHERE pid = ? AND valid_until > ?;";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, pid);
            ps.setDate(2, yesterday);
            
            ResultSet resultSet = ps.executeQuery();
            while (resultSet.next()){
                String cid = resultSet.getString("cid");
                activeContracts.add(cid);
            }
            ps.close();
        }

        String updateSql = "UPDATE " + SCHEMA + ".Contract SET valid_until = ? WHERE cid = ?;" ;
        try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
            for (String id : activeContracts) {
                ps.setDate(1, yesterday);
                ps.setString(2, id);
                
                ps.executeUpdate();
            }
            ps.close();
        }
    }

    private static List<Integer> getTakenNumbers(Connection conn, String teamName, String leagueName) throws SQLException {
        List<Integer> takenNumbers = new ArrayList<Integer>();

        Date yesterday = Date.valueOf(LocalDate.now().minusDays(1));


        String sql = "SELECT jersey_number FROM " + SCHEMA + ".Contract WHERE team_name = ? AND league_name = ? AND valid_until > ?;";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, teamName);
            ps.setString(2, leagueName);
            ps.setDate(3, yesterday);
            
            ResultSet resultSet = ps.executeQuery();
            while (resultSet.next()){
                int jersey_number = resultSet.getInt("jersey_number");
                takenNumbers.add(jersey_number);
            }
            ps.close();
            return takenNumbers; 
        }
    }

    private static boolean checkTeamExists(Connection conn, String teamName, String leagueName) throws SQLException{
        String sql = "SELECT * FROM " + SCHEMA + ".Team WHERE team_name = ? AND league_name = ?;";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, teamName);
            ps.setString(2, leagueName);
            
            ResultSet resultSet = ps.executeQuery();
            if (resultSet.next()){
                ps.close();
                return true;
            }
            ps.close();
            return false; 
        }
    }

    private static boolean checkPlayerExists(Connection conn, String pid) throws SQLException{
        String sql = "SELECT * FROM " + SCHEMA + ".Player WHERE pid = ?;";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, pid);
            
            ResultSet resultSet = ps.executeQuery();
            if (resultSet.next()){
                ps.close();
                return true;
            }
            ps.close();
            return false; 
        }
    }

    private static boolean verifyInput(String input, String property, int maxLength){
        if (input.isEmpty()) {
            System.out.println("Please enter a non-empty " + property + ". \n");
            return false;
        }

        if (input.length() > maxLength) {
            System.out.println("Input too long, keep less than or equal to " + String.valueOf(maxLength) + " characters.");
            return false;
        }
        return true;
    }

    private static boolean verifyNumber(String input, String property){
        if (input.isEmpty()) {
            System.out.println("Please enter a non-empty " + property + ". \n");
            return false;
        }

        try {
            int num = Integer.parseInt(input);
            if (num < 0) {
                System.out.println(property + " cannot be less than 0.");
                return false;
            }
            return true;
        } catch (Exception e) {
            System.out.println("Invalid numerical input");
            return false;
        }
    }

    private static void printSQLException(String context, SQLException e) {
        System.err.println("Database error while " + context + ": " + e.getMessage());
        System.err.println("SQLSTATE: " + e.getSQLState() + ", SQLCODE: " + e.getErrorCode());
    }
}

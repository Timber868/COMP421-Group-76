package tasks;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;

public class RegisterPersonTask {
    private static final String SCHEMA = "CS421G76";

    public static void run(Connection conn, Scanner scanner) {
        try {
            System.out.print("Enter person name: ");
            String name = scanner.nextLine().trim();

            if (!verifyInput(name, "name", 255)){
                return;
            }

            System.out.print("Enter person nationality: ");
            String nationality = scanner.nextLine().trim();

            if (!verifyInput(nationality, "nationality", 255)){
                return;
            }

            System.out.print("Enter person birthdate (enter as: Month DD YYYY): ");
            String birthdate = scanner.nextLine().trim();

            if (!verifyDate(birthdate)){
                return;
            }

            String pid = UUID.randomUUID().toString();

            System.out.print("Enter person role (Coach, Player, Referee): ");
            String role = scanner.nextLine().trim();
            List<String> validRoles = List.of("Coach", "Player", "Referee");
            if (role.isEmpty() || !validRoles.contains(role)) {
                System.out.println("Please enter a valid role\n");
                return;
            }

            List<String> roleData;
            if (role.compareTo("Coach") == 0) {
                roleData = getCoachData(scanner);
                insertPersonRecord(conn, pid, name, nationality, birthdate);
                insertCoachRecord(conn, pid, roleData.get(0), roleData.get(1), roleData.get(2), roleData.get(3), roleData.get(4));
            }
            
            if (role.compareTo("Player") == 0) {
                roleData = getPlayerData(scanner);
                insertPersonRecord(conn, pid, name, nationality, birthdate);
                insertPlayerRecord(conn, pid, roleData.get(0), roleData.get(1), roleData.get(2));
            }
            
            if (role.compareTo("Referee") == 0) {
                roleData = getRefereeData(scanner);
                insertPersonRecord(conn, pid, name, nationality, birthdate);
                insertRefereeRecord(conn, pid, roleData.get(0));
            }
        } catch (SQLException e) {
            printSQLException("Registering a person", e);
        }
    }

    private static void insertPersonRecord(Connection conn, String pid, String name, String nationality, String birthdate) throws SQLException {
        String sql = "INSERT INTO " + SCHEMA + ".PERSON VALUES " 
                + "(?, ?, ?, DATE(TIMESTAMP_FORMAT(?, 'Month DD YYYY')));";
        
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, pid);
            ps.setString(2, name);
            ps.setString(3, nationality);
            ps.setString(4, birthdate);
            
            ps.executeUpdate();
            ps.close();

            System.out.println("Person successfully created, with name: " + name + ", nationality: " + nationality + " and birthdate: " + birthdate);
        }
        return;
    }

    private static void insertRefereeRecord(Connection conn, String pid, String certificationLevel) throws SQLException {
        String sql = "INSERT INTO " + SCHEMA + ".Referee VALUES " 
                + "(?, ?);";
        
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, pid);
            ps.setString(2, certificationLevel);
            
            ps.executeUpdate();
            ps.close();

            System.out.println("Successfully created referee with certification level: " + certificationLevel + " for person: " + pid);
        }
        return;
    }

    private static void insertPlayerRecord(Connection conn, String pid, String position, String dominantHand, String debutDate) throws SQLException {
        String sql = "INSERT INTO " + SCHEMA + ".Player VALUES " 
                + "(?, ?, ?, DATE(TIMESTAMP_FORMAT(?, 'Month DD YYYY')));";
        
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, pid);
            ps.setString(2, position);
            ps.setString(3, dominantHand);
            ps.setString(4, debutDate);
            
            ps.executeUpdate();
            ps.close();

            System.out.println("Successfully created player with position: " + position + ", dominant hand: " + dominantHand + ", and debut date: " + debutDate + " for person: " + pid);
        }
        return;
    }

    private static void insertCoachRecord(Connection conn, String pid, String roleSpecialty, String gamesCoached, String gamesWon, String teamName, String leagueName) throws SQLException {
        String sql;
        boolean includeTeam = true;
        if (teamName.compareTo("") == 0 & leagueName.compareTo("") == 0) {
            includeTeam = false;
            sql = "INSERT INTO " + SCHEMA + ".Coach (pid, role_specialty, games_coached, games_won) VALUES "
            + "(?, ?, ?, ?);";
        }
        else {
            sql = "INSERT INTO " + SCHEMA + ".Coach VALUES " 
            + "(?, ?, ?, ?, ?, ?);";
        }
        
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, pid);
            ps.setString(2, roleSpecialty);
            ps.setInt(3, Integer.parseInt(gamesCoached));
            ps.setInt(4, Integer.parseInt(gamesWon));
            if (includeTeam){
                ps.setString(5, teamName);
                ps.setString(6, leagueName);
            }
            
            ps.executeUpdate();
            ps.close();

            if (teamName.compareTo("") == 0 & leagueName.compareTo("") == 0) {
                System.out.println("Successfully created inactive coach with role specialty: " + roleSpecialty + ", games coached: " + gamesCoached + ", games won: " + gamesWon + " for person: " + pid);
            }
            else {
                System.out.println("Successfully created active coach with role specialty: " + roleSpecialty + ", games coached: " + gamesCoached + ", games won: " + gamesWon + " team: " + teamName + " in the " + leagueName + " for person: " + pid);
            }
        }
        return;
    }

    private static List<String> getRefereeData(Scanner scanner){
        List<String> refereeData = new ArrayList<String>();

        System.out.print("Enter referee certification level (ex: Professional, Semi-Professional): ");
        String certificationLevel = scanner.nextLine().trim();

        if (!verifyInput(certificationLevel, "certification level", 255)){
            return refereeData;
        }

        refereeData.add(certificationLevel);
        return refereeData;
    }

    private static List<String> getPlayerData(Scanner scanner){
        List<String> playerData = new ArrayList<String>();

        System.out.print("Enter player position: ");
        String position = scanner.nextLine().trim();

        if (!verifyInput(position, "position", 255)){
            return playerData;
        }

        System.out.print("Enter player dominant hand: ");
        String dominantHand = scanner.nextLine().trim();

        if (!verifyInput(dominantHand, "dominant hand", 255)){
            return playerData;
        }

        System.out.print("Enter player debut date (enter as: Month DD YYYY): ");
        String debutDate = scanner.nextLine().trim();

        if (!verifyDate(debutDate)){
            return playerData;
        }
        
        playerData.add(position);
        playerData.add(dominantHand);
        playerData.add(debutDate);
        return playerData;
    }

    private static List<String> getCoachData(Scanner scanner){
        List<String> coachData = new ArrayList<String>();

        System.out.print("Enter coach role specialty: ");
        String roleSpecialty = scanner.nextLine().trim();

        if (!verifyInput(roleSpecialty, "role specialty", 255)){
            return coachData;
        }

        System.out.print("Enter coach games won (as int): ");
        String gamesWon = scanner.nextLine().trim();

        if (!verifyNumber(gamesWon, "games won")){
            return coachData;
        }

        System.out.print("Enter coach games coached (as int): ");
        String gamesCoached = scanner.nextLine().trim();

        if (!verifyNumber(gamesCoached, "games coached")){
            return coachData;
        }

        System.out.print("Is the coach actively coaching a team now? (Y/n): ");
        String active = scanner.nextLine().trim();
        if (!verifyInput(active, "active status", 1)){
            return coachData;
        }
        String teamName = "";
        String teamLeagueName = "";
        if (active.compareTo("Y") == 0) {
            System.out.print("Enter name of the team that the coach is coaching : ");
            teamName = scanner.nextLine().trim();

            if (!verifyInput(teamName, "team name", 255)){
                return coachData;
            }

            System.out.print("Enter name of the league that the coach is coaching in: ");
            teamLeagueName = scanner.nextLine().trim();

            if (!verifyInput(teamLeagueName, "league name", 255)){
                return coachData;
            }
        }
        
        coachData.add(roleSpecialty);
        coachData.add(gamesCoached);
        coachData.add(gamesWon);
        coachData.add(teamName);
        coachData.add(teamLeagueName);
        return coachData;
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

    private static boolean verifyDate(String input) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM d uuuu").withResolverStyle(ResolverStyle.STRICT);
        try {
            LocalDate date = LocalDate.parse(input, formatter);
            return true;
        } catch (Exception e) {
            System.out.println("Invalid date input");
            return false;
        }        
    }

    private static void printSQLException(String context, SQLException e) {
        System.err.println("Database error while " + context + ": " + e.getMessage());
        System.err.println("SQLSTATE: " + e.getSQLState() + ", SQLCODE: " + e.getErrorCode());
    }
}

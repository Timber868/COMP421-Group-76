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

    public static void run(Connection conn, Scanner scanner) { // Entry point for this task
        try {
            System.out.print("Enter person name: "); // Prompt user for the new person's name
            String name = scanner.nextLine().trim(); // Get the input

            if (!verifyInput(name, "name", 255)){ // Check that it is a valid input if not return to main menu
                return;
            }

            System.out.print("Enter person nationality: "); // Prompt user for the new person's nationality
            String nationality = scanner.nextLine().trim(); // Get the input

            if (!verifyInput(nationality, "nationality", 255)){ // Check that it is a valid input if not return to main menu
                return;
            }

            System.out.print("Enter person birthdate (enter as: Month DD YYYY): "); // Prompt the user for the new person's birthdate
            String birthdate = scanner.nextLine().trim(); // Get the input
            if (!verifyDate(birthdate)){ // Make sure that it is a valid date and it needs to be like Month DD YYYY
                return;
            }

            String pid = UUID.randomUUID().toString(); // Generate a random uuid for the pid, to make db as realistic as possible

            System.out.print("Enter person role (Coach, Player, Referee): "); // Prompt the user to get the role of the new person
            String role = scanner.nextLine().trim(); // Get the input
            List<String> validRoles = List.of("Coach", "Player", "Referee"); // Create a list of the valid roles
            if (role.isEmpty() || !validRoles.contains(role)) { // Make sure the input is a valid role
                System.out.println("Please enter a valid role\n"); // If not valid say that a valid role should be selected
                return;
            }

            List<String> roleData; // Initialize a list of strings to capture the role specific data
            if (role.compareTo("Coach") == 0) { // If the role of the new person is a coach
                roleData = getCoachData(scanner); // Prompt the user to input coach information
                insertPersonRecord(conn, pid, name, nationality, birthdate); // Create the person using the gathered data
                insertCoachRecord(conn, pid, roleData.get(0), roleData.get(1), roleData.get(2), roleData.get(3), roleData.get(4)); // Create a coach entity using the corresponding data
            }
            
            if (role.compareTo("Player") == 0) { // If the role of the new person is a player
                roleData = getPlayerData(scanner); // Prompt the user to input player information
                insertPersonRecord(conn, pid, name, nationality, birthdate); // Create the person using the gathered data
                insertPlayerRecord(conn, pid, roleData.get(0), roleData.get(1), roleData.get(2)); // Create a player entity using the corresponding data
            }
            
            if (role.compareTo("Referee") == 0) { // If the role of the new person is a referee
                roleData = getRefereeData(scanner); // Prompt the user to input player information
                insertPersonRecord(conn, pid, name, nationality, birthdate); // Create the person using the gathered data
                insertRefereeRecord(conn, pid, roleData.get(0)); // Create a referee entity using the corresponding data
            }
        } catch (SQLException e) { // Catch any SQLException
            printSQLException("Registering a person", e); // Print the context and what the exception is
        }
    }

    // Function to create a person entity
    private static void insertPersonRecord(Connection conn, String pid, String name, String nationality, String birthdate) throws SQLException {
        String sql = "INSERT INTO " + SCHEMA + ".PERSON VALUES " 
                + "(?, ?, ?, DATE(TIMESTAMP_FORMAT(?, 'Month DD YYYY')));"; // Create the skeleton of the INSERT statement that should be used to create a person
        
        try (PreparedStatement ps = conn.prepareStatement(sql)) { // Use a PreparedStatement as shown in class
            // Populate the placeholders using the correct information
            ps.setString(1, pid);
            ps.setString(2, name);
            ps.setString(3, nationality);
            ps.setString(4, birthdate);
            
            ps.executeUpdate(); // Execute the update
            ps.close();

            // Print a confirmation message
            System.out.println("Person successfully created, with name: " + name + ", nationality: " + nationality + " and birthdate: " + birthdate);
        }
        return; // Return to execution
    }

    // Function to create a referee entity
    private static void insertRefereeRecord(Connection conn, String pid, String certificationLevel) throws SQLException {
        String sql = "INSERT INTO " + SCHEMA + ".Referee VALUES " 
                + "(?, ?);"; // Create the skeleton of the INSERT statement that should be used to create a referee
        
        // Same as above
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, pid);
            ps.setString(2, certificationLevel);
            
            ps.executeUpdate();
            ps.close();

            System.out.println("Successfully created referee with certification level: " + certificationLevel + " for person: " + pid);
        }
        return;
    }

    // Function to create a player entity
    private static void insertPlayerRecord(Connection conn, String pid, String position, String dominantHand, String debutDate) throws SQLException {
        String sql = "INSERT INTO " + SCHEMA + ".Player VALUES " 
                + "(?, ?, ?, DATE(TIMESTAMP_FORMAT(?, 'Month DD YYYY')));"; // Create the skeleton of the INSERT statement that should be used to create a player
        
        // Same as above
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

    // Function to create a referee entity
    private static void insertCoachRecord(Connection conn, String pid, String roleSpecialty, String gamesCoached, String gamesWon, String teamName, String leagueName) throws SQLException {
        String sql; // Since a coach does not have to have a coach, we will need two template queries
        boolean includeTeam = true;
        if (teamName.compareTo("") == 0 & leagueName.compareTo("") == 0) { // If the team name and league name are none, that means the coach is not active
            includeTeam = false; // Set flag to false
            sql = "INSERT INTO " + SCHEMA + ".Coach (pid, role_specialty, games_coached, games_won) VALUES "
            + "(?, ?, ?, ?);"; // Create the skeleton of the INSERT statement that should be used to create a coach with no team
        }
        else { // If team name and league name are both non empty then just do a regular INSERT 
            sql = "INSERT INTO " + SCHEMA + ".Coach VALUES " 
            + "(?, ?, ?, ?, ?, ?);"; // Create the skeleton of the INSERT statement that should be used to create a coach with a team
        }
        
        // Same as above
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, pid);
            ps.setString(2, roleSpecialty);
            ps.setInt(3, Integer.parseInt(gamesCoached));
            ps.setInt(4, Integer.parseInt(gamesWon));
            if (includeTeam){ // Only difference is that when you include a team you have 2 extra placeholders so include them
                ps.setString(5, teamName);
                ps.setString(6, leagueName);
            }
            
            ps.executeUpdate();
            ps.close();

            // Print message depending on whether coach has a team
            if (teamName.compareTo("") == 0 & leagueName.compareTo("") == 0) {
                System.out.println("Successfully created inactive coach with role specialty: " + roleSpecialty + ", games coached: " + gamesCoached + ", games won: " + gamesWon + " for person: " + pid);
            }
            else {
                System.out.println("Successfully created active coach with role specialty: " + roleSpecialty + ", games coached: " + gamesCoached + ", games won: " + gamesWon + " team: " + teamName + " in the " + leagueName + " for person: " + pid);
            }
        }
        return;
    }

    // Function that prompts the user for the referee data
    private static List<String> getRefereeData(Scanner scanner){
        List<String> refereeData = new ArrayList<String>(); // List to capture and return referee data

        // Prompt and capture certification level
        System.out.print("Enter referee certification level (ex: Professional, Semi-Professional): ");
        String certificationLevel = scanner.nextLine().trim();

        if (!verifyInput(certificationLevel, "certification level", 255)){ // Make sure that it is of the correct formatting
            return refereeData; // If it isn't return empty refereeData
        }

        refereeData.add(certificationLevel); // If it is good format add to list
        return refereeData;
    }

    // Same as above except with the player properties
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

    // Same as above except getting the coach properties
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
        if (active.compareTo("Y") == 0) { // If the coach is actively coaching a team prompt to get team information
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

    // Function to verify varchar/char inputs
    private static boolean verifyInput(String input, String property, int maxLength){
        if (input.isEmpty()) { // Make sure it is non empty
            System.out.println("Please enter a non-empty " + property + ". \n"); // Print error message
            return false; // return false
        }

        if (input.length() > maxLength) { // Make sure that the input is not too long
            System.out.println("Input too long, keep less than or equal to " + String.valueOf(maxLength) + " characters."); // Print error message
            return false; // Return false
        }
        return true; // If passed input validation return true
    }

    // Function to verify that numerical input is valid
    private static boolean verifyNumber(String input, String property){
        if (input.isEmpty()) { // If it is empty automatically not valid, same as above
            System.out.println("Please enter a non-empty " + property + ". \n");
            return false;
        }

        try {
            int num = Integer.parseInt(input); // Try to parse value into int
            if (num < 0) { // If it is an int make sure it is greater than 0
                System.out.println(property + " cannot be less than 0.");
                return false;
            }
            return true;
        } catch (Exception e) { // If we can't parse into into catch the exception
            System.out.println("Invalid numerical input"); // Print error mesage
            return false;
        }
    }

    // Function to verify date input
    private static boolean verifyDate(String input) {
        // Create a formatter that will be able to parse the date format of Month DD YYYY
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM d uuuu").withResolverStyle(ResolverStyle.STRICT);
        try {
            LocalDate date = LocalDate.parse(input, formatter); // Try to parse date
            return true; // If valid date return true
        } catch (Exception e) { // If invalid date throws exception 
            System.out.println("Invalid date input"); // Print error message
            return false; // Return false
        }        
    }

    // Function that prints the SQLException details and during what task it occured
    private static void printSQLException(String context, SQLException e) {
        System.err.println("Database error while " + context + ": " + e.getMessage());
        System.err.println("SQLSTATE: " + e.getSQLState() + ", SQLCODE: " + e.getErrorCode());
    }
}

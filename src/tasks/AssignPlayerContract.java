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

    public static void run(Connection conn, Scanner scanner) { // Entry point for this task
        try{
            System.out.print("Enter the pid of the player you wish to give a contract to: "); // Prompt the user for the player id
            String pid = scanner.nextLine().trim(); // Get the input
            if (!verifyInput(pid, "pid", 36)){ // Check that it is a valid input if not return to main menu
                return;
            }
            if (!checkPlayerExists(conn, pid)){
                System.out.println("Player with pid: " + pid + " does not exist.");
                return;
            }

            System.out.println("Here are the possible leagues for the player to sign in: "); // Display the leagues for a submenu task
            displayLeagues(conn); // Display the leagues
            System.out.print("Enter the name of the league the team plays in (from the list above by name): "); // Prompt the user to get the league
            String leagueName = scanner.nextLine().trim(); // Get the input
            if (!verifyInput(leagueName, "league name", 255)){ // Check that it is a valid input if not return to main menu
                return;
            }
            if (!displayTeamsInLeague(conn, leagueName)) { // If the selected league has no teams or invalid league selection
                return; // Return to main menu
            }

            System.out.print("Enter the team name that the player should get a contract for (from the list above by name): "); // Prompt user to select team name
            String teamName = scanner.nextLine().trim(); // Get the input
            if (!verifyInput(teamName, "team name", 255)){ // Check that it is a valid input if not return to main menu
                return;
            }
            if (!checkTeamExists(conn, teamName,  leagueName)){ // Verify that the team exists
                return;
            }

            System.out.print("Is this a transfer (Y/n): "); // Prompt user if this contract is being created due to a transfer
            String isTransfer = scanner.nextLine().trim(); // Get the input
            if (!verifyInput(isTransfer, "active status", 1)){ // Check that it is a valid input if not return to main menu
                return;
            }
            if (isTransfer.compareTo("Y") == 0) { // If the contract is being generated due to a trade, get the transfer information
                // Same team and league prompting as above, just for the team that the player is being traded from
                System.out.println("Here are the possible leagues the player is getting traded from: ");
                displayLeagues(conn);
                System.out.print("Enter the name of the league the trading team plays in (from the list above by name): ");
                String fromLeagueName = scanner.nextLine().trim();
                if (!verifyInput(fromLeagueName, "league name", 255)){
                    return;
                }
                if (!displayTeamsInLeague(conn, fromLeagueName)) {
                    return;
                }

                System.out.print("Enter the team name that the player is getting traded from (from the list above by name): ");
                String fromTeamName = scanner.nextLine().trim();
                if (!verifyInput(fromTeamName, "team name", 255)){
                    return;
                }
                if (!checkTeamExists(conn, fromTeamName,  fromLeagueName)){
                    return;
                }

                System.out.print("Enter the transfer fee: "); // Prompt the user for the transfer fee
                String fee = scanner.nextLine().trim(); // Get the input
                if (!verifyDouble(fee, "fee")) { // Check that it is a valid input if not return to main menu
                    return;
                }

                System.out.print("Enter the transfer type: "); // Prompt the user for the transfer type
                String transferType = scanner.nextLine().trim(); // Get the input
                if (!verifyInput(transferType, "transfer type", 255)){ // Check that it is a valid input if not return to main menu
                    return;
                }

                transferContract(conn, pid, fee, transferType, fromTeamName, fromLeagueName, teamName, leagueName); // Call the stored procedure to create the transfer and contract
                printTradeInfo(teamName, leagueName, fromLeagueName, fromTeamName); // Print confirmation message of trade
                return; // Return to main menu
            }

            // If the contract is not arising from a trade
            System.out.print("Enter the length of the contract (in years): "); // Prompt the user for the length of the contract
            String contractLength = scanner.nextLine().trim(); // Get the input
            if (!verifyInt(contractLength, "contract length")){ // Check that it is a valid input if not return to main menu
                return;
            }

            List<Integer> takenNumbers = getTakenNumbers(conn, teamName, leagueName); // Get the jersey numbers that are already taken on the team
            String invalidNumbers = ""; // Initialize the string of taken numbers
            for (int i = 0; i < takenNumbers.size(); i++) {
                invalidNumbers += String.valueOf(takenNumbers.get(i)) + " "; // Iterate through the taken numbers and join them on the string
            }
            System.out.print("Enter jersey number. Cannot be one of " + invalidNumbers.trim() + ": "); // Prompt the user for the jersey number while also displaying the taken numbers
            String jerseyNumber = scanner.nextLine().trim(); // Get the output
            if (!verifyInt(jerseyNumber, "jersey number")){ // Check that it is a valid input if not return to main menu
                return;
            }
            if (takenNumbers.contains(Integer.parseInt(jerseyNumber))) { // Check that the chosen number is not already taken
                return; // If it is return to main menu
            }

            setActiveContractsToYesterday(conn, pid); // Set the player's current active contracts to valid_until yesterday to signify invalid

            createContract(conn, pid, teamName, leagueName, contractLength, jerseyNumber); // Create the contract entity

            printNewContractInfo(teamName, leagueName, contractLength, jerseyNumber); // Print the new contract information
        } catch (SQLException e) { // If there is an SQLException that is thrown, catch it and print it nicely
            printSQLException("Giving a contract to a player", e);
        }
    }

    // Function that prints confirmation of the new contract if it was not due to a trade
    private static void printNewContractInfo(String teamName, String leagueName, String contractLength, String jerseyNumber) {
        System.out.println("Contract created!");
        System.out.println("--- Contract Info ---");
        System.out.println("Contract with " + teamName + " in the " + leagueName);
        System.out.println("Jersey number for the duration of the contract: " + jerseyNumber);
        String until = LocalDate.now().plusYears(Long.parseLong(contractLength)).toString();
        String today = LocalDate.now().toString();
        System.out.println("Contract valid from today (" + today + ") until " + until);
    }

    // Function that prints confirmation of the trade
    private static void printTradeInfo(String teamName, String leagueName, String fromLeagueName, String fromTeamName) {
        System.out.println("Trade Complete!");
        System.out.println("--- Trade Info ---");
        System.out.println("Traded to " + teamName + " in the " + leagueName);
        System.out.println("Traded from " + fromTeamName + " in the " + fromLeagueName);
    }

    // Function that is used to call the stored procedure for create a transfer and a new contract as a result
    private static void transferContract(Connection conn, String playerId, String fee, String type, String fromTeamName, String fromLeagueName, String teamName, String leagueName) throws SQLException {        
        String sql = "{CALL " + SCHEMA + ".CREATE_CONTRACT_FROM_TRADE(?,?, ?, ?, ?, ?, ?, ?, ?)}"; // Sql query template that is used to call a stored procedure

        try (PreparedStatement ps = conn.prepareCall(sql)) { // Load it into a PreparedStatement as shown in class
            // Set the values of the place holders
            ps.setString(1, playerId);
            ps.setString(2, UUID.randomUUID().toString());
            ps.setString(3, UUID.randomUUID().toString());
            ps.setDouble(4, Double.parseDouble(fee));
            ps.setString(5, type);
            ps.setString(6, fromTeamName);
            ps.setString(7, fromLeagueName);
            ps.setString(8, teamName);
            ps.setString(9, leagueName);

            ps.executeUpdate(); // Execute the update
            ps.close();
        }
    }

    // Function used to get the teams in a specific league
    private static boolean displayTeamsInLeague(Connection conn, String leagueName) throws SQLException {
        List<String> teamNames = new ArrayList<String>(); // Initialize a List of strings used to capture the team names
        
        String sql = "SELECT team_name FROM " + SCHEMA + ".Team WHERE league_name = ?;"; // Create the template sql query

        // Same as above
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, leagueName);
            ResultSet resultSet = ps.executeQuery();

            while (resultSet.next()){ // While there are tuples in the query result
                String teamName = resultSet.getString("team_name"); // Get the property under the neam team_name and add it to the list of team names
                teamNames.add(teamName);
            }
            ps.close();
        }

        if (teamNames.size() == 0) { // If after adding all the team names the list is still empty this means there are either no teams in the league or no league with the corresponding name
            System.out.println("Either no league with league name or no teams in league."); // Print error message
            return false;
        }

        System.out.println("Teams to choose from"); // If there are teams in the list
        for (int i = 0; i < teamNames.size(); i++) { // Iterate through them
            System.out.println(String.valueOf(i + 1) + ". " + teamNames.get(i)); // Print their names
        }
        return true;
    }

    // Function used to print the valid leagues in the db
    private static void displayLeagues(Connection conn) throws SQLException {
        // Same as the above function just with different sql query
        List<String> leagueNames = new ArrayList<String>();
        
        String sql = "SELECT league_name FROM " + SCHEMA + ".League;";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ResultSet resultSet = ps.executeQuery();
            while (resultSet.next()){
                String leagueName = resultSet.getString("league_name");
                leagueNames.add(leagueName);
            }
            ps.close();
        }

        System.out.println("Leagues to choose from");
        for (int i = 0; i < leagueNames.size(); i++) {
            System.out.println(String.valueOf(i + 1) + ". " + leagueNames.get(i));
        }
    }

    // Function used to create a contract entity
    private static void createContract(Connection conn, String pid, String teamName, String leagueName, String contractLength, String jerseyNumber) throws SQLException {
        Date validFrom = Date.valueOf(LocalDate.now()); // Set the valid_from date to be today
        Date validUntil = Date.valueOf(LocalDate.now().plusYears(Long.parseLong(contractLength))); // Valid_until date can be calculated using the contract length

        String cid = UUID.randomUUID().toString(); // Create a new random uuid for the cid

        String sql = "INSERT INTO " + SCHEMA + ".Contract VALUES " 
            + "(?, ?, ?, ?, ?, ?, ?);"; // Create the template sql query
        
        // Same as above by setting the placeholders
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

    // Function that is used to update any active contracts to be inactive by setting valid_until date to yesterday
    private static void setActiveContractsToYesterday(Connection conn, String pid) throws SQLException {
        List<String> activeContracts = new ArrayList<String>(); // Initialize the list of active contracts for the player pid

        Date yesterday = Date.valueOf(LocalDate.now().minusDays(1)); // Get yesterday's date


        String sql = "SELECT cid FROM " + SCHEMA + ".Contract WHERE pid = ? AND valid_until > ?;"; // Write the template sql query

        // Same as above
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, pid);
            ps.setDate(2, yesterday);
            
            ResultSet resultSet = ps.executeQuery();
            while (resultSet.next()){ // While there are tuples
                String cid = resultSet.getString("cid"); // Get the value of cid
                activeContracts.add(cid); // Add it to the list
            }
            ps.close();
        }

        // Write the template sql query for updating a contract with a given cid so that the valid_until date is yesterday
        String updateSql = "UPDATE " + SCHEMA + ".Contract SET valid_until = ? WHERE cid = ?;" ;
        try (PreparedStatement ps = conn.prepareStatement(updateSql)) { // Same as above
            for (String id : activeContracts) {
                ps.setDate(1, yesterday);
                ps.setString(2, id);
                
                ps.executeUpdate();
            }
            ps.close();
        }
    }

    // Function that is used to query the numbers of active contracts on a certain team
    private static List<Integer> getTakenNumbers(Connection conn, String teamName, String leagueName) throws SQLException {
        List<Integer> takenNumbers = new ArrayList<Integer>(); // Initialize a list of integers

        Date yesterday = Date.valueOf(LocalDate.now().minusDays(1)); // Get yesterday's date so that we can Select by dates greater than yesterday (i.e. active ones)

        // Set the template sql query for getting jersey numbers from active contracts from a team
        String sql = "SELECT jersey_number FROM " + SCHEMA + ".Contract WHERE team_name = ? AND league_name = ? AND valid_until > ?;";

        // Same as above
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, teamName);
            ps.setString(2, leagueName);
            ps.setDate(3, yesterday);
            
            ResultSet resultSet = ps.executeQuery();
            while (resultSet.next()){ // While there is another tuple
                int jersey_number = resultSet.getInt("jersey_number"); // Get the jersey_num parameter
                takenNumbers.add(jersey_number); // Add the number to the list
            }
            ps.close();
            return takenNumbers; // Return the taken numbers
        }
    }

    // Function that checks if a team exists given team name and league name
    private static boolean checkTeamExists(Connection conn, String teamName, String leagueName) throws SQLException{
        // Write the sql template query to select the team with matching PK
        String sql = "SELECT * FROM " + SCHEMA + ".Team WHERE team_name = ? AND league_name = ?;";

        // Same as above
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, teamName);
            ps.setString(2, leagueName);
            
            ResultSet resultSet = ps.executeQuery();
            if (resultSet.next()){ // If there is a result return true
                ps.close();
                return true;
            }
            ps.close();
            return false; // If not return false
        }
    }

    // Function that checks if a player exists given a pid
    private static boolean checkPlayerExists(Connection conn, String pid) throws SQLException{
        // Write the sql template query to slect the player with the given pid
        String sql = "SELECT * FROM " + SCHEMA + ".Player WHERE pid = ?;";

        // Same as above
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, pid);
            
            ResultSet resultSet = ps.executeQuery();
            if (resultSet.next()){ // If there is a result return true
                ps.close();
                return true;
            }
            ps.close();
            return false; // If not return false
        }
    }

    private static boolean verifyInput(String input, String property, int maxLength){
        if (input.isEmpty()) { // Make sure it is non empty
            System.out.println("Please enter a non-empty " + property + ". \n"); // Print error message
            return false;
        }

        if (input.length() > maxLength) { // Make sure that the input is not too long
            System.out.println("Input too long, keep less than or equal to " + String.valueOf(maxLength) + " characters."); // Print error message
            return false;
        }
        return true; // If passed input validation return true
    }

    private static boolean verifyInt(String input, String property){
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

    private static boolean verifyDouble(String input, String property){
        if (input.isEmpty()) { // If it is empty automatically not valid, same as above
            System.out.println("Please enter a non-empty " + property + ". \n");
            return false;
        }

        try {
            double num = Double.parseDouble(input); // Try to parse value into double
            if (num < 0) { // If it is an double make sure it is greater than 0
                System.out.println(property + " cannot be less than 0.");
                return false;
            }
            return true;
        } catch (Exception e) { // If we can't parse into into catch the exception
            System.out.println("Invalid numerical input"); // Print error mesage
            return false;
        }
    }

    // Function that prints the SQLException details and during what task it occured
    private static void printSQLException(String context, SQLException e) {
        System.err.println("Database error while " + context + ": " + e.getMessage());
        System.err.println("SQLSTATE: " + e.getSQLState() + ", SQLCODE: " + e.getErrorCode());
    }
}

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class AuthSystem {
    private static final String FILE_PATH = "users.txt";
    
    // Class to hold password and highscore
    private static class UserRecord {
        String password;
        int highscore;

        UserRecord(String password, int highscore) {
            this.password = password;
            this.highscore = highscore;
        }
    }

    private HashMap<String, UserRecord> userDatabase = new HashMap<>(); // Maps username to an instance of above class

    public AuthSystem() {
        loadUsers();
    }

    // Takes the file and loads it into the hashmap
    private void loadUsers() {
        File file = new File(FILE_PATH);
        if (!file.exists()) return;

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(":");
                if (parts.length == 3) {
                    String user = parts[0];
                    String pass = parts[1];
                    int score = Integer.parseInt(parts[2]);
                    userDatabase.put(user, new UserRecord(pass, score));
                }
            }
        } catch (IOException | NumberFormatException e) {
            System.err.println("Error loading user database: " + e.getMessage());
        }
    }

    public boolean register(String username, String password) {
        if (userDatabase.containsKey(username) || username.contains(":")) return false;
        
        userDatabase.put(username, new UserRecord(password, 0));
        saveAllUsers();
        return true;
    }

    public boolean login(String username, String password) {
        UserRecord record = userDatabase.get(username);
        return record != null && record.password.equals(password);
    }

    public int getHighScore(String username) {
        UserRecord record = userDatabase.get(username);
        return (record != null) ? record.highscore : 0;
    }

    public void updateHighScore(String username, int newScore) {
        UserRecord record = userDatabase.get(username);
        if (record != null && newScore > record.highscore) {
            record.highscore = newScore;
            saveAllUsers(); // Persist changes immediately
        }
    }

    // Rewrites the file to ensure data integrity
    private void saveAllUsers() {
        try (PrintWriter out = new PrintWriter(new FileWriter(FILE_PATH))) {
            for (Map.Entry<String, UserRecord> entry : userDatabase.entrySet()) {
                UserRecord r = entry.getValue();
                out.println(entry.getKey() + ":" + r.password + ":" + r.highscore);
            }
        } catch (IOException e) {
            System.err.println("Error saving user database: " + e.getMessage());
        }
    }
}
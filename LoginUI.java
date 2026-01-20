import javax.swing.*;

public class LoginUI {
    public static String showLogin(AuthSystem auth) {
        String[] options = {"Login", "Register", "Exit"};
        int choice = JOptionPane.showOptionDialog(null, "Welcome to Raycaster Shooter", 
                "Login System", JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, 
                null, options, options[0]);

        if (choice == 2 || choice == -1) System.exit(0);

        JTextField usernameField = new JTextField();
        JPasswordField passwordField = new JPasswordField();
        Object[] message = { "Username:", usernameField, "Password:", passwordField };

        int res = JOptionPane.showConfirmDialog(null, message, "Enter Credentials", JOptionPane.OK_CANCEL_OPTION);
        if (res == JOptionPane.OK_OPTION) {
            String user = usernameField.getText();
            String pass = new String(passwordField.getPassword());

            if (choice == 0) { // Login
                if (auth.login(user, pass)) return user;
                else JOptionPane.showMessageDialog(null, "Invalid username or password.");
            } else { // Register
                if (auth.register(user, pass)) {
                    JOptionPane.showMessageDialog(null, "Registration successful! Please login.");
                } else {
                    JOptionPane.showMessageDialog(null, "User already exists.");
                }
            }
        }
        return null;
    }
}
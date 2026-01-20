import javax.swing.JFrame;
//import javax.swing.Renderer;
import javax.swing.SwingUtilities;

public class RaycasterGame implements Runnable {
    private GameState state;
    private InputHandler input;
    private Renderer renderer;
    private JFrame frame;

    public RaycasterGame(AuthSystem auth, String username) {
        // Initialize GameState with the auth system and username
       state = new GameState(auth, username);
       input = new InputHandler();
       state.input = input; // Make sure the state knows about the input
       renderer = new Renderer(state); 

       renderer.addKeyListener(input);
       renderer.addMouseListener(input);

        frame = new JFrame("Raycaster Shooter - Player: " + username);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        renderer.setFocusable(true);
        
        frame.add(renderer);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        new Thread(this).start();
    }

    public static void main(String[] args) {
        AuthSystem auth = new AuthSystem();
        String user = null;

        // Loop until a valid user is returned from the LoginUI
        while (user == null) {
            user = LoginUI.showLogin(auth);
        }

        final String finalUser = user;
        SwingUtilities.invokeLater(() -> new RaycasterGame(auth, finalUser));
    }

    @Override
    public void run() {
        while (true) {
            state.update(input);
            renderer.repaint();
            try { Thread.sleep(16); } catch (Exception e) {}
        }
    }
}
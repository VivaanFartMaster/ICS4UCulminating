import java.awt.event.*;

public class InputHandler implements KeyListener, MouseListener {
    public boolean up, down, rotateLeft, rotateRight, shooting;
    public boolean paused = false; // The Master Switch

    @Override
    public void mousePressed(MouseEvent e) {
        int x = e.getX();
        int y = e.getY();

        // Check if click is in the Pause Button area (Top Right)
        // Rectangle: x=730 to 790, y=10 to 45
        if (x >= 720 && x <= 790 && y >= 10 && y <= 50) {
            paused = !paused;
        }
    }

    public void mouseReleased(MouseEvent e) {}
    public void mouseClicked(MouseEvent e) {}
    public void mouseEntered(MouseEvent e) {}
    public void mouseExited(MouseEvent e) {}
    
    @Override
    public void keyPressed(KeyEvent e) {
        int code = e.getKeyCode();
        if (code == KeyEvent.VK_W) up = true;
        if (code == KeyEvent.VK_S) down = true;
        if (code == KeyEvent.VK_A) rotateLeft = true;
        if (code == KeyEvent.VK_D) rotateRight = true;
        if (code == KeyEvent.VK_SPACE) shooting = true;
        // Also allow 'P' to pause
        if (code == KeyEvent.VK_P) paused = !paused;
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int code = e.getKeyCode();
        if (code == KeyEvent.VK_W) up = false;
        if (code == KeyEvent.VK_S) down = false;
        if (code == KeyEvent.VK_A) rotateLeft = false;
        if (code == KeyEvent.VK_D) rotateRight = false;
        if (code == KeyEvent.VK_SPACE) shooting = false;
    }
    public void keyTyped(KeyEvent e) {}
}
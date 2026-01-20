import javax.swing.*;
import java.awt.*;
import java.util.Collections;

public class Renderer extends JPanel {
    private GameState state;
    private double[] zBuffer; 

    public Renderer(GameState state) {
        this.state = state;
        setPreferredSize(new Dimension(800, 600));
        setBackground(Color.BLACK);
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        int w = getWidth();
        int h = getHeight();

        // 1. Draw Environment
        g2.setColor(Color.DARK_GRAY);
        g2.fillRect(0, 0, w, h/2);
        g2.setColor(new Color(50, 50, 50));
        g2.fillRect(0, h/2, w, h/2);

        // 2. Raycast Walls
        renderWalls(g2, w, h);

        // 3. Render Sprites (Enemies)
        renderSprites(g2, w, h);

        // 4. Draw Weapon
        renderWeapon(g2, w, h);

        // 5. Draw UI (This contains the scores)
        drawUI(g2);
    }

    private void renderWalls(Graphics2D g2, int w, int h) {
        if (zBuffer == null || zBuffer.length != w) zBuffer = new double[w];
        for (int x = 0; x < w; x++) {
            double cameraX = 2 * x / (double) w - 1; 
            double rayDirX = state.dirX + state.planeX * cameraX;
            double rayDirY = state.dirY + state.planeY * cameraX;
            int mapX = (int) state.posX;
            int mapY = (int) state.posY;
            double deltaDistX = Math.abs(1 / rayDirX);
            double deltaDistY = Math.abs(1 / rayDirY);
            double sideDistX, sideDistY;
            int stepX, stepY, hit = 0, side = 0;

            if (rayDirX < 0) { stepX = -1; sideDistX = (state.posX - mapX) * deltaDistX; }
            else { stepX = 1; sideDistX = (mapX + 1.0 - state.posX) * deltaDistX; }
            if (rayDirY < 0) { stepY = -1; sideDistY = (state.posY - mapY) * deltaDistY; }
            else { stepY = 1; sideDistY = (mapY + 1.0 - state.posY) * deltaDistY; }

            while (hit == 0) {
                if (sideDistX < sideDistY) { sideDistX += deltaDistX; mapX += stepX; side = 0; }
                else { sideDistY += deltaDistY; mapY += stepY; side = 1; }
                if (state.worldMap[mapX][mapY] > 0) hit = 1;
            }
            double perpWallDist = (side == 0) ? (sideDistX - deltaDistX) : (sideDistY - deltaDistY);
            zBuffer[x] = perpWallDist;
            int lineHeight = (int) (h / perpWallDist);
            int drawStart = Math.max(0, -lineHeight / 2 + h / 2);
            int drawEnd = Math.min(h - 1, lineHeight / 2 + h / 2);
            g2.setColor((side == 1) ? new Color(100, 100, 150) : new Color(80, 80, 130));
            g2.drawLine(x, drawStart, x, drawEnd);
        }
    }

    private void renderSprites(Graphics2D g2, int w, int h) {
        Collections.sort(state.enemies);
        for (Enemy e : state.enemies) {
            double spriteX = e.x - state.posX;
            double spriteY = e.y - state.posY;
            double invDet = 1.0 / (state.planeX * state.dirY - state.dirX * state.planeY);
            double transformX = invDet * (state.dirY * spriteX - state.dirX * spriteY);
            double transformY = invDet * (-state.planeY * spriteX + state.planeX * spriteY);
            if (transformY <= 0) continue;
            int spriteScreenX = (int)((w / 2) * (1 + transformX / transformY));
            int spriteSize = Math.abs((int)(h / transformY * e.getSize()));
            int drawStartX = Math.max(0, -spriteSize / 2 + spriteScreenX);
            int drawEndX = Math.min(w - 1, spriteSize / 2 + spriteScreenX);
            boolean isFlashing = (System.currentTimeMillis() - e.lastHitTime < 100);
            g2.setColor(isFlashing ? Color.WHITE : e.getColor());
            for (int stripe = drawStartX; stripe < drawEndX; stripe++) {
                if (transformY < zBuffer[stripe]) g2.fillRect(stripe, -spriteSize/2 + h/2, 1, spriteSize);
            }
        }
    }

    private void renderWeapon(Graphics2D g2, int w, int h) {
        g2.setColor(Color.GRAY);
        int gunY = h - h/3 + (state.shootingAction ? 15 : 0);
        g2.fillRect(w/2 - w/8, gunY, w/4, h/3);
        if (state.shootingAction && state.shootFrame < 3) {
            g2.setColor(Color.YELLOW);
            g2.fillOval(w/2 - 15, h/2 + 20, 30, 30);
        }
    }

    private void drawMinimap(Graphics2D g2) {
    int w = getWidth();
    int h = getHeight();
    
    // 1. Setup dimensions
    int blockSize = 6; 
    int padding = 20;
    int mapPixelWidth = state.MAP_HEIGHT * blockSize; 
    int mapPixelHeight = state.MAP_WIDTH * blockSize; 

    // Top left of minimap
    int startX = w - mapPixelWidth - padding;
    int startY = h - mapPixelHeight - padding;

    // 2. Draw Background and Border
    g2.setColor(new Color(0, 0, 0, 180));
    g2.fillRect(startX, startY, mapPixelWidth, mapPixelHeight);
    g2.setColor(Color.WHITE);
    g2.setStroke(new BasicStroke(1));
    g2.drawRect(startX, startY, mapPixelWidth, mapPixelHeight);

    // 3. Draw Walls (
    for (int r = 0; r < state.MAP_WIDTH; r++) {     // r = row (state.posX axis)
        for (int c = 0; c < state.MAP_HEIGHT; c++) { // c = col (state.posY axis)
            if (state.worldMap[r][c] > 0) {
                g2.setColor(Color.GRAY);
                // Horizontal Screen Position = startX + column
                // Vertical Screen Position   = startY + row
                g2.fillRect(startX + (c * blockSize), startY + (r * blockSize), blockSize, blockSize);
            }
        }
    }

    // 4. Draw Enemies
    g2.setColor(Color.RED);
    for (Enemy e : state.enemies) {
        int eX = startX + (int)(e.y * blockSize);
        int eY = startY + (int)(e.x * blockSize);
        g2.fillOval(eX - 1, eY - 1, 3, 3);
    }

    // 5. Draw Player
    g2.setColor(Color.GREEN);
    int pX = startX + (int)(state.posY * blockSize); // Player Y maps to Screen X
    int pY = startY + (int)(state.posX * blockSize); // Player X maps to Screen Y
    g2.fillOval(pX - 2, pY - 2, 5, 5);

    // 6. Draw View Direction Line
    g2.setColor(Color.GREEN);
    int lineLength = 8;
    g2.drawLine(pX, pY, 
                (int)(pX + state.dirY * lineLength), 
                (int)(pY + state.dirX * lineLength));
    }

    private void drawUI(Graphics2D g2) {
    int w = getWidth();
    int h = getHeight();

    // Pause button 
    g2.setColor(new Color(50, 50, 50, 200)); 
    g2.fillRoundRect(w - 80, 10, 70, 40, 10, 10);
    g2.setColor(Color.WHITE);
    g2.setStroke(new BasicStroke(2));
    g2.drawRoundRect(w - 80, 10, 70, 40, 10, 10);
    
    g2.setFont(new Font("Arial", Font.BOLD, 14));
    String btnText = state.input.paused ? "RESUME" : "PAUSE";
    g2.drawString(btnText, w - 73, 35);

    if (state.input.paused && !state.isGameOver) {
        g2.setColor(new Color(0, 0, 0, 120)); // Dim the screen
        g2.fillRect(0, 0, w, h);
        
        g2.setColor(Color.YELLOW);
        g2.setFont(new Font("Arial", Font.BOLD, 50));
        g2.drawString("GAME PAUSED", w/2 - 170, h/2);
    }

    // Player HUD with necessary info
    g2.setFont(new Font("Monospaced", Font.BOLD, 20));
    g2.setColor(state.isInvincible ? Color.YELLOW : Color.RED);
    g2.drawString("LIVES:    " + state.lives, 20, 40);
    g2.setColor(Color.CYAN);
    g2.drawString("SCORE:    " + state.currentScore, 20, 70);
    g2.setColor(Color.YELLOW);
    g2.drawString("HI-SCORE: " + state.highScore, 20, 100);

    drawMinimap(g2);

    // Game over screen
    if (state.isGameOver) {
        // Dim the background
        g2.setColor(new Color(0, 0, 0, 180)); // Semi-transparent black
        g2.fillRect(0, 0, w, h);

        // Draw game over text
        g2.setColor(Color.RED);
        g2.setFont(new Font("Arial", Font.BOLD, 60));
        String mainText = "GAME OVER";
        int mainWidth = g2.getFontMetrics().stringWidth(mainText);
        g2.drawString(mainText, w / 2 - mainWidth / 2, h / 2 - 50);

        // Show final score
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, 25));
        String scoreText = "FINAL SCORE: " + state.currentScore;
        int scoreWidth = g2.getFontMetrics().stringWidth(scoreText);
        g2.drawString(scoreText, w / 2 - scoreWidth / 2, h / 2 + 10);

        // Show high score
        g2.setColor(Color.YELLOW);
        g2.drawString("PERSONAL BEST: " + state.highScore, w / 2 - scoreWidth / 2, h / 2 + 45);

        // Restart text
        g2.setColor(Color.LIGHT_GRAY);
        g2.setFont(new Font("Arial", Font.ITALIC, 18));
        String retryText = "Press SPACE to Restart";
        int retryWidth = g2.getFontMetrics().stringWidth(retryText);
        g2.drawString(retryText, w / 2 - retryWidth / 2, h / 2 + 100);
    }
    }
}
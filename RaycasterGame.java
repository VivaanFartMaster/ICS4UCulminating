import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

public class RaycasterGame extends JPanel implements Runnable, KeyListener {

    // --- Constants ---
    private static final int MAP_WIDTH = 24;
    private static final int MAP_HEIGHT = 24;
    private static final double MOVE_SPEED = 0.08;
    private static final double ROT_SPEED = 0.05;
    
    // Map: 1 = Wall, 0 = Empty
    private final int[][] worldMap = {
        {1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},
        {1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
        {1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
        {1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
        {1,0,0,0,1,1,1,1,1,0,0,0,0,0,0,1,0,0,0,0,0,0,0,1},
        {1,0,0,0,1,0,0,0,1,0,0,0,0,0,0,1,0,0,0,0,0,0,0,1},
        {1,0,0,0,1,0,0,0,1,0,0,0,0,0,0,1,1,1,1,0,0,0,0,1},
        {1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
        {1,0,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
        {1,0,1,0,0,0,0,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
        {1,0,0,0,0,0,0,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
        {1,0,0,0,0,0,0,0,0,0,0,0,0,1,0,1,0,1,0,0,0,0,0,1},
        {1,0,0,0,0,0,0,0,0,0,0,0,0,1,0,0,0,0,0,0,0,0,0,1},
        {1,0,0,0,0,0,0,0,0,0,0,0,0,1,0,1,0,1,0,0,0,0,0,1},
        {1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
        {1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
        {1,1,1,1,1,1,1,1,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
        {1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
        {1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
        {1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
        {1,0,0,0,1,1,0,0,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
        {1,0,0,0,1,1,0,0,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
        {1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
        {1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1}
    };

    // --- Player State ---
    private double posX = 22, posY = 12; // Start pos
    private double dirX = -1, dirY = 0; // Initial direction vector
    private double planeX = 0, planeY = 0.66; // Camera plane (FOV)
    private int lives = 5;
    private boolean isInvincible = false;
    private long invincibilityStart = 0;
    
    // --- Input State ---
    private boolean up, down, left, right, rotateLeft, rotateRight;

    // --- Weapon ---
    private boolean shooting = false;
    private int shootFrame = 0; // For animation

    // --- Enemies ---
    enum EnemyType {
        SPEED(0.06, 0.4, 20, Color.GREEN),
        TANK(0.02, 0.8, 100, Color.RED),
        BIG(0.035, 1.2, 50, Color.ORANGE);

        double speed, size;
        int hp;
        Color color;

        EnemyType(double s, double sz, int hp, Color c) {
            this.speed = s; this.size = sz; this.hp = hp; this.color = c;
        }
    }

    class Enemy implements Comparable<Enemy> {
        double x, y;
        EnemyType type;
        int hp;
        long lastHitTime = 0;
        double distToPlayer; // For sorting

        Enemy(double x, double y, EnemyType type) {
            this.x = x; this.y = y; this.type = type; this.hp = type.hp;
        }

        @Override
        public int compareTo(Enemy o) {
            // Sort descending by distance (painter's algorithm)
            return Double.compare(o.distToPlayer, this.distToPlayer);
        }
    }

    private ArrayList<Enemy> enemies = new ArrayList<>();
    private Random rand = new Random();

    // --- Rendering Buffers ---
    private double[] zBuffer; // 1D Z-Buffer for wall distances

    // --- Main Setup ---
    public RaycasterGame() {
        setPreferredSize(new Dimension(800, 600));
        setBackground(Color.BLACK);
        addKeyListener(this);
        setFocusable(true);
        
        // Spawn Enemies
        spawnEnemies();
        
        new Thread(this).start();
    }

    private void spawnEnemies() {
        for (int i = 0; i < 8; i++) {
            EnemyType t = EnemyType.values()[rand.nextInt(3)];
            double ex, ey;
            do {
                ex = 1 + rand.nextInt(MAP_WIDTH - 2);
                ey = 1 + rand.nextInt(MAP_HEIGHT - 2);
            } while (worldMap[(int)ex][(int)ey] != 0 || 
                     Math.sqrt((ex-posX)*(ex-posX) + (ey-posY)*(ey-posY)) < 5);
            enemies.add(new Enemy(ex, ey, t));
        }
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Raycaster Shooter");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(new RaycasterGame());
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    // --- Game Loop ---
    @Override
    public void run() {
        long lastTime = System.nanoTime();
        final double ns = 1000000000.0 / 60.0;
        double delta = 0;
        
        while (true) {
            long now = System.nanoTime();
            delta += (now - lastTime) / ns;
            lastTime = now;
            
            repaint();
            try { Thread.sleep(2); } catch(Exception e) {}
        }
    }

    // --- Logic Update ---
    private void update() {
        // 1. Invincibility Timer
        if (isInvincible) {
            if (System.currentTimeMillis() - invincibilityStart > 1000) {
                isInvincible = false;
            }
        }

        // 2. Player Movement
        double moveStep = up ? MOVE_SPEED : (down ? -MOVE_SPEED : 0);
        if (moveStep != 0) {
            double nextX = posX + dirX * moveStep;
            double nextY = posY + dirY * moveStep;
            // Wall Collision Check (Simple sliding)
            if (worldMap[(int)(nextX)][(int)posY] == 0) posX = nextX;
            if (worldMap[(int)posX][(int)(nextY)] == 0) posY = nextY;
        }

        // 3. Player Rotation
        double rotStep = rotateRight ? -ROT_SPEED : (rotateLeft ? ROT_SPEED : 0);
        if (rotStep != 0) {
            double oldDirX = dirX;
            dirX = dirX * Math.cos(rotStep) - dirY * Math.sin(rotStep);
            dirY = oldDirX * Math.sin(rotStep) + dirY * Math.cos(rotStep);
            double oldPlaneX = planeX;
            planeX = planeX * Math.cos(rotStep) - planeY * Math.sin(rotStep);
            planeY = oldPlaneX * Math.sin(rotStep) + planeY * Math.cos(rotStep);
        }

        // 4. Weapon Logic
        if (shooting) {
            shootFrame++;
            if (shootFrame > 5) {
                shooting = false;
                shootFrame = 0;
            }
        }

        // 5. Enemy Logic
        for (int i = 0; i < enemies.size(); i++) {
            Enemy e = enemies.get(i);
            
            // Calculate distance for sorting and logic
            e.distToPlayer = Math.sqrt((posX - e.x) * (posX - e.x) + (posY - e.y) * (posY - e.y));

            // Move enemy towards player
            if (e.distToPlayer > 1.0) {
                double dx = posX - e.x;
                double dy = posY - e.y;
                double len = Math.sqrt(dx*dx + dy*dy);
                dx /= len; dy /= len; // Normalize
                
                double nextEx = e.x + dx * e.type.speed;
                double nextEy = e.y + dy * e.type.speed;
                
                // Simple entity wall collision
                if (worldMap[(int)nextEx][(int)e.y] == 0) e.x = nextEx;
                if (worldMap[(int)e.x][(int)nextEy] == 0) e.y = nextEy;
            }

            // Player Damage Logic
            if (e.distToPlayer < 0.5 && !isInvincible) {
                lives--;
                isInvincible = true;
                invincibilityStart = System.currentTimeMillis();
                if (lives <= 0) {
                    // Game Over handling (reset)
                    lives = 5;
                    enemies.clear();
                    spawnEnemies();
                    posX = 22; posY = 12;
                }
            }
        }

        // 6. Shooting Hitscan
        if (shooting && shootFrame == 1) { // Hit on first frame of shot
            // Simple hit check: is enemy in center of screen and visible?
            // We do this precisely in render, but for game logic, we approximate 
            // by checking angle to enemy vs player dir
            for (Enemy e : enemies) {
                 double dx = e.x - posX;
                 double dy = e.y - posY;
                 // Project enemy pos onto player direction vector
                 double dot = dx*dirX + dy*dirY;
                 if (dot > 0) { // In front
                     // Calculate perpendicular distance to player direction ray
                     double perpDist = Math.abs(dx*dirY - dy*dirX);
                     // If closer than width and visible (rough check)
                     if (perpDist < e.type.size * 0.5) {
                         e.hp -= 10;
                         e.lastHitTime = System.currentTimeMillis();
                     }
                 }
            }
            // Remove dead enemies
            enemies.removeIf(e -> e.hp <= 0);
            if (enemies.isEmpty()) spawnEnemies(); // Respawn wave
        }
    }

    // --- Render ---
    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        
        // Anti-aliasing off for retro feel, render speed
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

        int w = getWidth();
        int h = getHeight();

        // Initialize Z-Buffer
        if (zBuffer == null || zBuffer.length != w) {
            zBuffer = new double[w];
        }

        // Draw Ceiling and Floor
        g2.setColor(Color.DARK_GRAY);
        g2.fillRect(0, 0, w, h/2);
        g2.setColor(new Color(50, 50, 50));
        g2.fillRect(0, h/2, w, h/2);

        // --- Raycasting Walls ---
        for (int x = 0; x < w; x++) {
            // Calculate ray position and direction
            double cameraX = 2 * x / (double) w - 1; 
            double rayDirX = dirX + planeX * cameraX;
            double rayDirY = dirY + planeY * cameraX;

            // Which box of the map we're in
            int mapX = (int) posX;
            int mapY = (int) posY;

            // Length of ray from current position to next x or y-side
            double sideDistX;
            double sideDistY;

            // Length of ray from one x or y-side to next x or y-side
            double deltaDistX = (rayDirX == 0) ? 1e30 : Math.abs(1 / rayDirX);
            double deltaDistY = (rayDirY == 0) ? 1e30 : Math.abs(1 / rayDirY);
            double perpWallDist;

            // Step direction and initial sideDist calculation
            int stepX;
            int stepY;
            int hit = 0; // was there a wall hit?
            int side = 0; // NS or EW wall?

            if (rayDirX < 0) {
                stepX = -1;
                sideDistX = (posX - mapX) * deltaDistX;
            } else {
                stepX = 1;
                sideDistX = (mapX + 1.0 - posX) * deltaDistX;
            }
            if (rayDirY < 0) {
                stepY = -1;
                sideDistY = (posY - mapY) * deltaDistY;
            } else {
                stepY = 1;
                sideDistY = (mapY + 1.0 - posY) * deltaDistY;
            }

            // DDA Loop
            while (hit == 0) {
                // Jump to next map square
                if (sideDistX < sideDistY) {
                    sideDistX += deltaDistX;
                    mapX += stepX;
                    side = 0;
                } else {
                    sideDistY += deltaDistY;
                    mapY += stepY;
                    side = 1;
                }
                // Check bounds
                if(mapX < 0 || mapY < 0 || mapX >= MAP_WIDTH || mapY >= MAP_HEIGHT) {
                    hit = 1; perpWallDist = 100; // Fake hit distance
                } else if (worldMap[mapX][mapY] > 0) {
                    hit = 1;
                }
            }

            // Calculate distance projected on camera direction
            if (side == 0) perpWallDist = (mapX - posX + (1 - stepX) / 2) / rayDirX;
            else           perpWallDist = (mapY - posY + (1 - stepY) / 2) / rayDirY;

            zBuffer[x] = perpWallDist; // Store for sprite occlusion

            // Calculate height of line to draw on screen
            int lineHeight = (int) (h / perpWallDist);

            // Calculate lowest and highest pixel to fill in current stripe
            int drawStart = -lineHeight / 2 + h / 2;
            if (drawStart < 0) drawStart = 0;
            int drawEnd = lineHeight / 2 + h / 2;
            if (drawEnd >= h) drawEnd = h - 1;

            // Choose wall color
            Color wallColor = (side == 1) ? new Color(100, 100, 150) : new Color(80, 80, 130);
            g2.setColor(wallColor);
            g2.drawLine(x, drawStart, x, drawEnd);
        }

        // --- Sprite Casting (Enemies) ---
        // Sort enemies by distance from player
        for(Enemy e : enemies) {
             e.distToPlayer = ((posX - e.x) * (posX - e.x) + (posY - e.y) * (posY - e.y)); 
        }
        Collections.sort(enemies);

        for (Enemy e : enemies) {
            // Translate sprite position to relative to camera
            double spriteX = e.x - posX;
            double spriteY = e.y - posY;

            // Transform sprite with the inverse camera matrix
            double invDet = 1.0 / (planeX * dirY - dirX * planeY);
            double transformX = invDet * (dirY * spriteX - dirX * spriteY);
            double transformY = invDet * (-planeY * spriteX + planeX * spriteY); // Depth

            if(transformY <= 0) continue; // Behind player

            int spriteScreenX = (int)((w / 2) * (1 + transformX / transformY));

            // Calculate height of the sprite on screen
            int spriteHeight = Math.abs((int)(h / (transformY))) ; 
            // Scale based on enemy size stats
            spriteHeight = (int)(spriteHeight * e.type.size);
            
            // Calculate width of the sprite
            int spriteWidth = Math.abs((int)(h / (transformY))); 
            spriteWidth = (int)(spriteWidth * e.type.size);

            int drawStartY = -spriteHeight / 2 + h / 2;
            int drawEndY = spriteHeight / 2 + h / 2;
            if(drawStartY < 0) drawStartY = 0;
            if(drawEndY >= h) drawEndY = h - 1;

            int drawStartX = -spriteWidth / 2 + spriteScreenX;
            int drawEndX = spriteWidth / 2 + spriteScreenX;
            if(drawStartX < 0) drawStartX = 0;
            if(drawEndX >= w) drawEndX = w - 1;

            // Draw Sprite Stripes
            boolean isFlashing = (System.currentTimeMillis() - e.lastHitTime < 100);
            g2.setColor(isFlashing ? Color.WHITE : e.type.color);
            
            for(int stripe = drawStartX; stripe < drawEndX; stripe++) {
                // Determine if this stripe is in front of the wall
                if(transformY < zBuffer[stripe]) {
                    // Simple filled rectangle for enemy "billboard"
                    g2.drawLine(stripe, drawStartY, stripe, drawEndY);
                }
            }
        }

        // --- Weapon Rendering ---
        g2.setColor(Color.GRAY);
        // Simple Gun Model
        int gunW = w/4;
        int gunH = h/3;
        int gunX = w/2 - gunW/2;
        int gunY = h - gunH;
        
        // Recoil
        if(shooting) gunY += 10;
        
        g2.fillRect(gunX, gunY, gunW, gunH);
        g2.setColor(Color.DARK_GRAY);
        g2.drawRect(gunX, gunY, gunW, gunH);

        // Muzzle Flash
        if (shooting && shootFrame < 3) {
            g2.setColor(new Color(255, 255, 0, 200));
            g2.fillOval(w/2 - 20, h/2 + 20, 40, 40);
            g2.setColor(Color.WHITE);
            g2.fillOval(w/2 - 10, h/2 + 30, 20, 20);
        }

        // --- UI / HUD ---
        drawUI(g2, w, h);
    }

    private void drawUI(Graphics2D g, int w, int h) {
        // Crosshair
        g.setColor(Color.GREEN);
        g.drawLine(w/2 - 10, h/2, w/2 + 10, h/2);
        g.drawLine(w/2, h/2 - 10, w/2, h/2 + 10);

        // Lives
        g.setColor(isInvincible ? Color.YELLOW : Color.RED);
        g.setFont(new Font("Arial", Font.BOLD, 20));
        g.drawString("LIVES: " + lives, 20, 30);
        g.drawString("ENEMIES: " + enemies.size(), 20, 60);

    // --- Input Handling ---
    @Override
    public void keyPressed(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_W: up = true; break;
            case KeyEvent.VK_S: down = true; break;
            case KeyEvent.VK_A: left = true; break; // Strafe left (optional)
            case KeyEvent.VK_D: right = true; break; // Strafe right (optional)
            case KeyEvent.VK_LEFT: rotateLeft = true; break;
            case KeyEvent.VK_RIGHT: rotateRight = true; break;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_W: up = false; break;
            case KeyEvent.VK_S: down = false; break;
            case KeyEvent.VK_A: left = false; break;
            case KeyEvent.VK_D: right = false; break;
            case KeyEvent.VK_LEFT: rotateLeft = false; break;
            case KeyEvent.VK_RIGHT: rotateRight = false; break;
            case KeyEvent.VK_SPACE: shooting = false; break;
        }
    }
    @Override public void keyTyped(KeyEvent e) {}
}

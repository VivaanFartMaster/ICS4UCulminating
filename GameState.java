import java.util.ArrayList;
import java.util.Random;

public class GameState {
    // --- Constants ---
    public static final int MAP_WIDTH = 24;
    public static final int MAP_HEIGHT = 24;
    private static final double MOVE_SPEED = 0.08;
    private static final double ROT_SPEED = 0.05;

    // Map: 1 = Wall, 0 = Empty
    public final int[][] worldMap = {
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

    // --- Player Position & Camera ---
    public double posX = 22, posY = 12; 
    public double dirX = -1, dirY = 0; 
    public double planeX = 0, planeY = 0.66; 

    // --- Player Stats & Status ---
    public int lives = 5;
    public boolean isInvincible = false;
    public long invincibilityStart = 0;
    public boolean isGameOver = false;

    // Scoring & Persistence 
    public int currentScore = 0;
    public int highScore = 0;
    public String currentUsername;
    private AuthSystem auth;

    // --- Weapon Animation ---
    public boolean shootingAction = false; 
    public int shootFrame = 0; 

    // --- Enemies ---
    public ArrayList<Enemy> enemies = new ArrayList<>();
    private Random rand = new Random();
    public InputHandler input;
    public GameState(AuthSystem auth, String username) {
        this.auth = auth;
        this.currentUsername = username;
        // Get high score
        this.highScore = auth.getHighScore(username);
        spawnEnemies();
    }

    public void spawnEnemies() {
        for (int i = 0; i < 8; i++) {
            double ex, ey;
            do {
                ex = 1 + rand.nextInt(MAP_WIDTH - 2);
                ey = 1 + rand.nextInt(MAP_HEIGHT - 2);
            } while (worldMap[(int)ex][(int)ey] != 0 || 
                     Math.sqrt((ex-posX)*(ex-posX) + (ey-posY)*(ey-posY)) < 5);

            
            int typeChoice = rand.nextInt(3);
            if (typeChoice == 0) enemies.add(new SpeedEnemy(ex, ey));
            else if (typeChoice == 1) enemies.add(new TankEnemy(ex, ey));
            else enemies.add(new BouncerEnemy(ex, ey));
        }
    }

    public void update(InputHandler input) {
        // If paused or game is over, then do not play any game updates.
        if (input.paused || isGameOver) {
        if (isGameOver && input.shooting) {
            resetGame();
        }
        return; 
    }

        // Handle temporary invincibility after being hit
        if (isInvincible && System.currentTimeMillis() - invincibilityStart > 1000) {
            isInvincible = false;
        }

        // --- Player Movement ---
        double moveStep = input.up ? MOVE_SPEED : (input.down ? -MOVE_SPEED : 0);
        if (moveStep != 0) {
            double nextX = posX + dirX * moveStep;
            double nextY = posY + dirY * moveStep;
            if (worldMap[(int)nextX][(int)posY] == 0) posX = nextX;
            if (worldMap[(int)posX][(int)nextY] == 0) posY = nextY;
        }

        // --- Player Rotation ---
        double rotStep = input.rotateRight ? -ROT_SPEED : (input.rotateLeft ? ROT_SPEED : 0);
        if (rotStep != 0) {
            double oldDirX = dirX;
            dirX = dirX * Math.cos(rotStep) - dirY * Math.sin(rotStep);
            dirY = oldDirX * Math.sin(rotStep) + dirY * Math.cos(rotStep);
            double oldPlaneX = planeX;
            planeX = planeX * Math.cos(rotStep) - planeY * Math.sin(rotStep);
            planeY = oldPlaneX * Math.sin(rotStep) + planeY * Math.cos(rotStep);
        }

        // --- Weapon Animation State ---
        if (input.shooting && shootFrame == 0) shootingAction = true;
        if (shootingAction) {
            shootFrame++;
            if (shootFrame > 5) { shootingAction = false; shootFrame = 0; }
        }

        updateEnemies();
    }

    private void updateEnemies() {
        for (int i = 0; i < enemies.size(); i++) {
            Enemy e = enemies.get(i);
            e.distToPlayer = Math.sqrt((posX - e.x) * (posX - e.x) + (posY - e.y) * (posY - e.y));

            // Run the specific update logic for each subclass
            e.update(this);

            // Collision with Player
            if (e.distToPlayer < 0.5 && !isInvincible) {
                lives--;
                isInvincible = true;
                invincibilityStart = System.currentTimeMillis();
                if (lives <= 0) {
                    isGameOver = true;
                }
            }
        }

        // --- Hitscan Shooting Logic ---
        if (shootingAction && shootFrame == 1) {
            for (Enemy e : enemies) {
                double dx = e.x - posX;
                double dy = e.y - posY;
                double dot = dx * dirX + dy * dirY;
                
                if (dot > 0) { // Check if enemy is in front
                    double perpDist = Math.abs(dx * dirY - dy * dirX);
                    if (perpDist < e.getSize() * 0.5) {
                        e.hp -= 10;
                        e.lastHitTime = System.currentTimeMillis();
                        
                        if (e.hp <= 0) {
                            currentScore += e.getPoints();
                            // Update and persist High Score if broken
                            if (currentScore > highScore) {
                                highScore = currentScore;
                                auth.updateHighScore(currentUsername, highScore);
                            }
                        }
                    }
                }
            }
            // Clear dead enemies and spawn new wave if empty
            enemies.removeIf(e -> e.hp <= 0);
            if (enemies.isEmpty() && !isGameOver) spawnEnemies();
        }
    }

    private void resetGame() {
        lives = 5;
        currentScore = 0;
        isGameOver = false;
        isInvincible = false;
        enemies.clear();
        spawnEnemies();
        // Reset player to starting position
        posX = 22; posY = 12;
        dirX = -1; dirY = 0;
        planeX = 0; planeY = 0.66;
    }
}
import java.awt.Color;

public abstract class Enemy implements Comparable<Enemy> {
    public double x, y;
    public double distToPlayer;
    public int hp;
    public long lastHitTime;
    
    protected double speed;
    protected double size;
    protected Color color;
    protected int points;

    public Enemy(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public abstract void update(GameState state);

    public double getSize() { return size; }
    public Color getColor() { return color; }
    public int getPoints() { return points; }

    @Override
    public int compareTo(Enemy other) { // Drawing close enemies first, this allows for closer enemies to "cover" farther away ones properly.
        return Double.compare(other.distToPlayer, this.distToPlayer);
    }
}

// Moves quickly toward the player
class SpeedEnemy extends Enemy {
    public SpeedEnemy(double x, double y) {
        super(x, y);
        this.speed = 0.06; this.size = 0.4; this.hp = 20;
        this.color = Color.GREEN; this.points = 100;
    }
    @Override
    public void update(GameState state) {
        double dx = (state.posX - x) / (distToPlayer + 0.01);
        double dy = (state.posY - y) / (distToPlayer + 0.01);
        if (state.worldMap[(int)(x + dx * speed)][(int)y] == 0) x += dx * speed;
        if (state.worldMap[(int)x][(int)(y + dy * speed)] == 0) y += dy * speed;
    }
}

// Moves slower, but more health
class TankEnemy extends Enemy {
    public TankEnemy(double x, double y) {
        super(x, y);
        this.speed = 0.02; this.size = 0.8; this.hp = 100;
        this.color = Color.RED; this.points = 500;
    }
    @Override
    public void update(GameState state) {
        double dx = (state.posX - x) / (distToPlayer + 0.01);
        double dy = (state.posY - y) / (distToPlayer + 0.01);
        if (state.worldMap[(int)(x + dx * speed)][(int)y] == 0) x += dx * speed;
        if (state.worldMap[(int)x][(int)(y + dy * speed)] == 0) y += dy * speed;
    }
}


// More "mindless", simply bounces off walls
class BouncerEnemy extends Enemy {
    private double vx, vy;
    public BouncerEnemy(double x, double y) {
        super(x, y);
        this.speed = 0.1; this.size = 0.6; this.hp = 40;
        this.color = Color.MAGENTA; this.points = 300;
        double angle = Math.random() * Math.PI * 2;
        this.vx = Math.cos(angle) * speed;
        this.vy = Math.sin(angle) * speed;
    }
    @Override
    public void update(GameState state) {
        double nextX = x + vx;
        double nextY = y + vy;
        if (state.worldMap[(int)nextX][(int)y] != 0) vx = -vx;
        else x = nextX;
        if (state.worldMap[(int)x][(int)nextY] != 0) vy = -vy;
        else y = nextY;
    }
}
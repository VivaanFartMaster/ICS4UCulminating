import java.awt.Color;

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
package domain.enemy;

import domain.Position;

import java.util.Objects;

/**
 * Enemigo lógico de sesión: posición continua, salud y estado de la FSM.
 * No hereda de Player, no conoce LibGDX, teclado ni persistencia y nunca se guarda en JSON.
 * La caja de colisión comparte medidas con el jugador para reutilizar la misma noción de cuerpo.
 */
public final class Zombie {
    public static final double WIDTH = 0.6;
    public static final double HEIGHT = 1.8;

    private double x;
    private double y;
    private double z;
    private int health;
    private ZombieState state = ZombieState.IDLE;
    private double deadSeconds;
    private boolean crawler;
    private boolean kamikaze;
    private boolean shattered;
    private double hitFlashSeconds;
    private double velocityY;
    private boolean onGround = true;
    private double fallPeakY;
    private double jumpCooldownSeconds;

    public Zombie(double x, double y, double z, int health) {
        if (health <= 0) {
            throw new IllegalArgumentException("Un zombi nace con salud positiva");
        }
        this.x = x;
        this.y = y;
        this.z = z;
        this.health = health;
        this.fallPeakY = y;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public void setPosition(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    /** Position entera de los pies, con el mismo criterio floor que Player.toPosition(). */
    public Position toPosition() {
        return new Position((int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z));
    }

    public int getHealth() {
        return health;
    }

    public boolean isAlive() {
        return health > 0 && state != ZombieState.DEAD;
    }

    /** Resta salud sin bajar de cero; la transición a DEAD la decide la FSM en el siguiente update. */
    public void takeDamage(int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("El daño debe ser positivo");
        }
        health = Math.max(0, health - amount);
        hitFlashSeconds = 0.18;
    }

    public ZombieState getState() {
        return state;
    }

    public void setState(ZombieState state) {
        this.state = Objects.requireNonNull(state, "state no puede ser null");
    }

    /** Segundos transcurridos desde que la FSM lo declaró DEAD; lo usan el despawn y el efecto visual. */
    public double getDeadSeconds() {
        return deadSeconds;
    }

    public void addDeadTime(double deltaSeconds) {
        deadSeconds += deltaSeconds;
    }

    public double height() { return crawler ? 0.8 : HEIGHT; }
    public boolean isCrawler() { return crawler; }
    public boolean isKamikaze() { return kamikaze; }
    public boolean isShattered() { return shattered; }
    public double hitFlashSeconds() { return hitFlashSeconds; }
    public double velocityY() { return velocityY; }
    public void setVelocityY(double value) { velocityY = value; }
    public boolean isOnGround() { return onGround; }
    public void setOnGround(boolean value) { onGround = value; }
    public double jumpCooldownSeconds() { return jumpCooldownSeconds; }

    public void advanceTimers(double seconds) {
        advanceFlash(seconds);
        advanceMotionTimers(seconds);
    }
    public void advanceFlash(double seconds) { hitFlashSeconds = Math.max(0, hitFlashSeconds - seconds); }
    public void advanceMotionTimers(double seconds) {
        jumpCooldownSeconds = Math.max(0, jumpCooldownSeconds - seconds);
    }
    public void beginJump(double speed) {
        velocityY = speed;
        onGround = false;
        fallPeakY = y;
        jumpCooldownSeconds = 1.0;
    }
    public void trackFallHeight() { fallPeakY = Math.max(fallPeakY, y); }
    public double fallDistance() { return Math.max(0, fallPeakY - y); }
    public void finishLanding() {
        velocityY = 0;
        onGround = true;
        fallPeakY = y;
    }
    public void markKamikaze() { kamikaze = true; }
    public void becomeCrawler() { crawler = true; kamikaze = true; }
    public void shatter() {
        shattered = true;
        kamikaze = true;
        health = 0;
        state = ZombieState.DEAD;
        velocityY = 0;
    }

    public double distanceTo(double otherX, double otherY, double otherZ) {
        double dx = x - otherX;
        double dy = y - otherY;
        double dz = z - otherZ;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
}

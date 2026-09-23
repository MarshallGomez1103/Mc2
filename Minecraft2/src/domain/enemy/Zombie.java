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
    private double attackTimer;
    private double deadSeconds;

    public Zombie(double x, double y, double z, int health) {
        if (health <= 0) {
            throw new IllegalArgumentException("Un zombi nace con salud positiva");
        }
        this.x = x;
        this.y = y;
        this.z = z;
        this.health = health;
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
    }

    public ZombieState getState() {
        return state;
    }

    /** Cambiar de estado reinicia el temporizador de ataque: el primer golpe siempre espera el cooldown. */
    public void setState(ZombieState state) {
        ZombieState next = Objects.requireNonNull(state, "state no puede ser null");
        if (next != this.state) {
            attackTimer = 0;
        }
        this.state = next;
    }

    public double getAttackTimer() {
        return attackTimer;
    }

    public void addAttackTime(double deltaSeconds) {
        attackTimer += deltaSeconds;
    }

    public void resetAttackTimer() {
        attackTimer = 0;
    }

    /** Segundos transcurridos desde que la FSM lo declaró DEAD; lo usan el despawn y el efecto visual. */
    public double getDeadSeconds() {
        return deadSeconds;
    }

    public void addDeadTime(double deltaSeconds) {
        deadSeconds += deltaSeconds;
    }

    public double distanceTo(double otherX, double otherY, double otherZ) {
        double dx = x - otherX;
        double dy = y - otherY;
        double dz = z - otherZ;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
}

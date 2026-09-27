package domain.player;

import java.util.Objects;

/** Vida de sesión: daño, caída y reaparición comparten una única causa de muerte. */
public final class PlayerLife {
    public enum DeathCause { VOID, ENEMY, KAMIKAZE }

    public static final int MAX_HEALTH = 100;
    private static final double VOID_Y = -8.0;

    private final Player player;
    private double respawnX;
    private double respawnY;
    private double respawnZ;
    private DeathCause deathCause;
    private double health = MAX_HEALTH;
    public static final double REGEN_DELAY_SECONDS = 5;
    public static final double REGEN_PER_SECOND = 5;
    public static final double HIT_FLASH_SECONDS = .3;
    private double secondsWithoutDamage;
    private double hitFlashSeconds;

    public PlayerLife(Player player) {
        this.player = Objects.requireNonNull(player, "player no puede ser null");
        saveCheckpoint();
    }

    public int health() {
        return (int) Math.floor(health + 1e-9);
    }

    public double healthFraction() {
        return health / MAX_HEALTH;
    }

    /** Daño positivo; los golpes posteriores a la muerte no cambian su causa. */
    public void takeDamage(int amount, DeathCause cause) {
        if (amount <= 0) throw new IllegalArgumentException("El daño debe ser positivo");
        Objects.requireNonNull(cause, "cause no puede ser null");
        if (isDead()) return;
        health = Math.max(0, health - amount);
        secondsWithoutDamage = 0;
        hitFlashSeconds = HIT_FLASH_SECONDS;
        if (health == 0) die(cause);
    }

    public double hitFlashSeconds() {
        return hitFlashSeconds;
    }

    /** Solo recibe tiempo de simulación; pausa y muerte conservan el estado. */
    public void advance(double deltaSeconds) {
        if (isDead() || !Double.isFinite(deltaSeconds) || deltaSeconds <= 0) return;
        hitFlashSeconds = Math.max(0, hitFlashSeconds - deltaSeconds);
        double previous = secondsWithoutDamage;
        secondsWithoutDamage += deltaSeconds;
        double regenerationSeconds = Math.max(0, secondsWithoutDamage - REGEN_DELAY_SECONDS)
                - Math.max(0, previous - REGEN_DELAY_SECONDS);
        health = Math.min(MAX_HEALTH, health + regenerationSeconds * REGEN_PER_SECOND);
    }

    public boolean isDead() {
        return deathCause != null;
    }

    public DeathCause deathCause() {
        return deathCause;
    }

    public void update() {
        if (isDead()) {
            return;
        }
        if (player.getY() < VOID_Y) {
            die(DeathCause.VOID);
        } else if (player.isOnGround()) {
            saveCheckpoint();
        }
    }

    /** Muerte inmediata, incluida la caída al vacío, independientemente de la vida restante. */
    public void die(DeathCause cause) {
        Objects.requireNonNull(cause, "cause no puede ser null");
        if (!isDead()) {
            health = 0;
            deathCause = cause;
        }
    }

    public void respawn() {
        if (!isDead()) {
            return;
        }
        player.setX(respawnX);
        player.setY(respawnY);
        player.setZ(respawnZ);
        player.setVelocityY(0);
        player.setOnGround(false);
        deathCause = null;
        health = MAX_HEALTH;
        secondsWithoutDamage = 0;
        hitFlashSeconds = 0;
    }

    private void saveCheckpoint() {
        respawnX = player.getX();
        respawnY = player.getY();
        respawnZ = player.getZ();
    }
}

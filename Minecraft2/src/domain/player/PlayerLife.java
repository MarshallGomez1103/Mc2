package domain.player;

import java.util.Objects;

/** Estado de vida de sesión: la caída y futuros ataques comparten una única entrada de muerte. */
public final class PlayerLife {
    public enum DeathCause { VOID, ENEMY }

    private static final double VOID_Y = -8.0;

    private final Player player;
    private double respawnX;
    private double respawnY;
    private double respawnZ;
    private DeathCause deathCause;

    public PlayerLife(Player player) {
        this.player = Objects.requireNonNull(player, "player no puede ser null");
        saveCheckpoint();
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

    /** Punto de integración futuro para daño letal de un zombi. */
    public void die(DeathCause cause) {
        Objects.requireNonNull(cause, "cause no puede ser null");
        if (!isDead()) {
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
    }

    private void saveCheckpoint() {
        respawnX = player.getX();
        respawnY = player.getY();
        respawnZ = player.getZ();
    }
}

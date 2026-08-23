package domain.player;

import domain.Position;

/**
 * Estado del jugador: posición continua, orientación y estado físico vertical.
 *
 * Contrato congelado (ver docs de Estudiante 2, sección 0.6): solo esta clase
 * puede agregar campos aquí. Las demás clases del paquete domain.player
 * (PlayerMovementService, PlayerPhysics, CollisionResolver) consumen
 * exclusivamente esta API pública.
 *
 * La posición se guarda como doubles porque el movimiento y la física
 * necesitan continuidad; {@link #toPosition()} la convierte a la Position
 * entera que usan World/Chunk y, eventualmente, la persistencia.
 */
public final class Player {
    public static final double WIDTH = 0.6;
    public static final double HEIGHT = 1.8;
    public static final double EYE_HEIGHT = 1.6;
    public static final double GRAVITY = -20.0;
    public static final double JUMP_SPEED = 8.0;
    public static final double MOVE_SPEED = 4.3;

    private static final float MIN_PITCH = -89f;
    private static final float MAX_PITCH = 89f;

    private double x;
    private double y;
    private double z;
    private float yaw;
    private float pitch;
    private double velocityY;
    private boolean onGround;

    public Player(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = 0f;
        this.pitch = 0f;
        this.velocityY = 0d;
        this.onGround = false;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public double getZ() {
        return z;
    }

    public void setZ(double z) {
        this.z = z;
    }

    public float getYaw() {
        return yaw;
    }

    public void setYaw(float yaw) {
        this.yaw = normalizeYaw(yaw);
    }

    public float getPitch() {
        return pitch;
    }

    public void setPitch(float pitch) {
        this.pitch = clampPitch(pitch);
    }

    public double getVelocityY() {
        return velocityY;
    }

    public void setVelocityY(double velocityY) {
        this.velocityY = velocityY;
    }

    public boolean isOnGround() {
        return onGround;
    }

    public void setOnGround(boolean onGround) {
        this.onGround = onGround;
    }

    /** Convierte la posición continua a la Position entera usada por World/Chunk (floor de cada eje). */
    public Position toPosition() {
        return new Position((int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z));
    }

    /** Vector de dirección horizontal "hacia adelante" según yaw, normalizado, componente y = 0. */
    public double[] forwardVector() {
        double radians = Math.toRadians(yaw);
        return new double[] {-Math.sin(radians), Math.cos(radians)};
    }

    /** Vector "hacia la derecha", perpendicular a forward, normalizado. */
    public double[] rightVector() {
        double radians = Math.toRadians(yaw);
        return new double[] {Math.cos(radians), Math.sin(radians)};
    }

    private static float clampPitch(float value) {
        if (value < MIN_PITCH) {
            return MIN_PITCH;
        }
        if (value > MAX_PITCH) {
            return MAX_PITCH;
        }
        return value;
    }

    private static float normalizeYaw(float value) {
        float normalized = value % 360f;
        if (normalized < 0f) {
            normalized += 360f;
        }
        return normalized;
    }
}

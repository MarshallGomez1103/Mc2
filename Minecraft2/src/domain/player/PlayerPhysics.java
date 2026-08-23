package domain.player;

/**
 * Componente vertical del movimiento: gravedad continua y salto.
 *
 * No conoce nada de {@code World} ni de colisión — solo lee/escribe el
 * estado físico de {@link Player} ({@code velocityY}, {@code onGround}).
 * El delta vertical que calcula aquí NO se aplica directamente a
 * {@code player.setY(...)}; es responsabilidad del Participante 3
 * ({@code CollisionResolver}) decidir si ese delta se aplica completo,
 * se recorta (choque con techo o piso) o se cancela, y de poner
 * {@code onGround = true} cuando el jugador aterriza.
 *
 * Simplificación documentada: no se implementa velocidad terminal de
 * caída (terminal velocity). No es obligatoria para el MVP y omitirla
 * mantiene esta clase simple; la velocidad de caída crece sin límite
 * mientras el jugador esté en el aire.
 */
public final class PlayerPhysics {

    /**
     * Acumula gravedad en la velocidad vertical del jugador.
     * Llamar una vez por frame/tick.
     */
    public void applyGravity(Player player, double deltaSeconds) {
        player.setVelocityY(player.getVelocityY() + Player.GRAVITY * deltaSeconds);
    }

    /**
     * Aplica un salto SOLO si el jugador está en el suelo.
     * No hace nada si no lo está (no hay doble salto en el MVP).
     */
    public void jump(Player player) {
        if (player.isOnGround()) {
            player.setVelocityY(Player.JUMP_SPEED);
            player.setOnGround(false);
        }
    }

    /**
     * Calcula el desplazamiento vertical deseado para este frame (sin aplicarlo).
     */
    public double computeIntendedDeltaY(Player player, double deltaSeconds) {
        return player.getVelocityY() * deltaSeconds;
    }
}

package domain.enemy;

/**
 * Condiciones observables que la FSM necesita para decidir; no incluye rutas ni referencias a World.
 *
 * @param health           salud actual del zombi; {@code <= 0} significa muerto.
 * @param distanceToPlayer distancia euclidiana al jugador en bloques; debe ser finita y no negativa.
 * @param playerAlive      false cuando el jugador ya murió: no hay objetivo que perseguir.
 */
public record ZombiePerception(int health, double distanceToPlayer, boolean playerAlive) {
    public ZombiePerception {
        if (Double.isNaN(distanceToPlayer) || Double.isInfinite(distanceToPlayer) || distanceToPlayer < 0) {
            throw new IllegalArgumentException("distanceToPlayer debe ser finita y >= 0");
        }
    }
}

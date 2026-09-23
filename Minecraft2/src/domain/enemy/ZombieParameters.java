package domain.enemy;

/**
 * Parámetros centralizados de un zombi. Estudiante 3 podrá construirlos a partir de
 * {@link Difficulty}; la IA solo los consume y nunca pregunta por la dificultad.
 *
 * @param detectionRange        distancia (bloques) a la que un zombi IDLE nota al jugador.
 * @param attackRange           distancia a la que puede golpear.
 * @param loseTargetRange       distancia a la que un zombi en CHASE abandona la persecución.
 * @param moveSpeed             bloques por segundo al perseguir.
 * @param maxHealth             golpes de jugador que resiste (cada golpe quita 1).
 * @param attackCooldownSeconds tiempo en ATTACK antes de cada golpe; el primero también espera.
 * @param repathIntervalSeconds recálculo periódico de ruta aunque nada cambie.
 * @param repathDistance        desplazamiento del jugador respecto al destino que fuerza recálculo.
 * @param noRouteRetrySeconds   espera tras un A* sin ruta antes de intentar de nuevo.
 */
public record ZombieParameters(
        double detectionRange,
        double attackRange,
        double loseTargetRange,
        double moveSpeed,
        int maxHealth,
        double attackCooldownSeconds,
        double repathIntervalSeconds,
        double repathDistance,
        double noRouteRetrySeconds) {

    public ZombieParameters {
        if (attackRange <= 0 || attackRange > detectionRange || detectionRange > loseTargetRange) {
            throw new IllegalArgumentException(
                    "Se requiere 0 < attackRange <= detectionRange <= loseTargetRange");
        }
        if (moveSpeed <= 0 || maxHealth <= 0 || attackCooldownSeconds <= 0
                || repathIntervalSeconds <= 0 || repathDistance <= 0 || noRouteRetrySeconds <= 0) {
            throw new IllegalArgumentException("Todos los parámetros deben ser positivos");
        }
    }

    /** Valores de sesión propuestos para NORMAL hasta que Estudiante 3 los derive de Difficulty. */
    public static ZombieParameters defaults() {
        return new ZombieParameters(12.0, 1.6, 18.0, 2.6, 3, 1.0, 1.5, 1.5, 1.0);
    }
}
